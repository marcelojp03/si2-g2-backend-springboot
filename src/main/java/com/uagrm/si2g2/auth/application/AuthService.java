package com.uagrm.si2g2.auth.application;

import com.uagrm.si2g2.auth.domain.PasswordRecoveryChallenge;
import com.uagrm.si2g2.auth.domain.PasswordRecoveryChallengeRepository;
import com.uagrm.si2g2.auth.domain.Permiso;
import com.uagrm.si2g2.auth.domain.Rol;
import com.uagrm.si2g2.auth.domain.Usuario;
import com.uagrm.si2g2.auth.domain.UsuarioRepository;
import com.uagrm.si2g2.auth.dto.AuthResponse;
import com.uagrm.si2g2.auth.dto.LoginRequest;
import com.uagrm.si2g2.auth.dto.PasswordRecoveryRequest;
import com.uagrm.si2g2.auth.dto.PasswordRecoveryResponse;
import com.uagrm.si2g2.auth.dto.PasswordRecoveryVerifyRequest;
import com.uagrm.si2g2.auth.dto.PasswordResetRequest;
import com.uagrm.si2g2.auth.dto.RegisterRequest;
import com.uagrm.si2g2.auditoria.application.AuditoriaService;
import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.config.AppProperties;
import com.uagrm.si2g2.persona.application.PersonaProvisioningService;
import com.uagrm.si2g2.saas.suscripcion.domain.SuscripcionInstitucionRepository;
import com.uagrm.si2g2.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordRecoveryChallengeRepository passwordRecoveryChallengeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AuditoriaService auditoriaService;
    private final RoleService roleService;
    private final PersonaProvisioningService personaProvisioningService;
    private final AppProperties appProperties;
    private final SuscripcionInstitucionRepository suscripcionRepo;
    private final IntentoLoginService intentoLoginService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            log.warn("Intento de registro con correo duplicado: {}", request.getCorreo());
            throw new IllegalStateException("Ya existe un usuario con el correo: " + request.getCorreo());
        }

        UUID idInstitucion = resolveTargetInstitutionId(request);

        // Validar límite de usuarios del plan vigente
        if (idInstitucion != null) {
            suscripcionRepo.findActivaByIdInstitucion(idInstitucion).ifPresent(s -> {
                long actuales = usuarioRepository.countByIdInstitucionAndEstado(idInstitucion, "ACTIVO");
                if (actuales >= s.getPlan().getMaxUsuarios()) {
                    throw new IllegalStateException(
                            "Se ha alcanzado el límite de usuarios del plan " + s.getPlan().getCodigo()
                                    + " (" + s.getPlan().getMaxUsuarios() + "). Actualice su plan para agregar más usuarios.");
                }
            });
        }

        if (request.getIdRol() == null && (request.getCodigoRol() == null || request.getCodigoRol().isBlank())) {
            throw new IllegalArgumentException("Debes seleccionar un rol para el nuevo usuario");
        }

        String codigoRol = (request.getCodigoRol() != null && !request.getCodigoRol().isBlank())
                ? request.getCodigoRol()
                : null;

        Rol rol = roleService.resolveAssignableRole(request.getIdRol(), codigoRol);

        Usuario usuario = Usuario.builder()
                .idInstitucion(idInstitucion)
                .correo(request.getCorreo())
                .hashContrasena(passwordEncoder.encode(request.getContrasena()))
                .nombres(request.getNombres())
                .apellidos(request.getApellidos())
                .roles(Set.of(rol))
                .build();

        usuarioRepository.save(usuario);
        personaProvisioningService.provisionForUsuario(usuario);
        log.info("Register exitoso: correo={}, rol={}", usuario.getCorreo(), rol.getCodigo());
        auditoriaService.registrar(usuario.getIdInstitucion(), usuario.getId(),
                "AUTH", "REGISTER", "usuario", usuario.getId().toString(),
                true, "Rol asignado: " + rol.getCodigo());

        return buildAuthResponse(usuario);
    }

    private UUID resolveTargetInstitutionId(RegisterRequest request) {
        if (SecurityUtils.currentUserHasRole("SUPER_ADMIN")) {
            return request.getIdInstitucion();
        }

        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        if (request.getIdInstitucion() != null && !idInstitucion.equals(request.getIdInstitucion())) {
            throw new AccessDeniedException("No puedes crear usuarios para otra institución");
        }
        return idInstitucion;
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getCorreo(), request.getContrasena())
            );
        } catch (BadCredentialsException e) {
            log.warn("Login fallido: correo={}", request.getCorreo());
            // Resolver usuario si existe para obtener idInstitucion
            Usuario usuarioFallido = usuarioRepository.findByCorreo(request.getCorreo()).orElse(null);
            intentoLoginService.registrarFallo(
                    request.getCorreo(),
                    "CREDENCIALES_INVALIDAS",
                    usuarioFallido != null ? usuarioFallido.getId() : null,
                    usuarioFallido != null ? usuarioFallido.getIdInstitucion() : null);
            auditoriaService.registrar(null, null,
                    "AUTH", "LOGIN_FALLIDO", "usuario", null,
                    false, "Credenciales incorrectas para: " + request.getCorreo());
            throw e;
        }

        Usuario usuario = usuarioRepository.findByCorreo(request.getCorreo())
                .orElseThrow();

        log.info("Login exitoso: correo={}, roles={}", usuario.getCorreo(),
                usuario.getRoles().stream().map(Rol::getCodigo).collect(Collectors.joining(",")));
        intentoLoginService.registrarExito(usuario);
        auditoriaService.registrar(usuario.getIdInstitucion(), usuario.getId(),
                "AUTH", "LOGIN_EXITOSO", "usuario", usuario.getId().toString(),
                true, null);

        return buildAuthResponse(usuario);
    }

    @Transactional
    public PasswordRecoveryResponse requestPasswordRecovery(PasswordRecoveryRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(request.getCorreo()).orElse(null);
        if (usuario == null) {
            auditoriaService.registrarDetallado(null, null, "AUTH", "PASSWORD_RECOVERY_REQUEST",
                    "usuario", null, null, Map.of("correo", request.getCorreo()), true,
                    "Solicitud de recuperación registrada");
            return PasswordRecoveryResponse.builder()
                    .mensaje("Si el correo existe, se ha generado un desafío de recuperación")
                    .build();
        }

        PasswordRecoveryChallenge challenge = PasswordRecoveryChallenge.builder()
                .idUsuario(usuario.getId())
                .correo(usuario.getCorreo())
                .codigoVerificacion(generateOtp())
                .tokenRecuperacion(UUID.randomUUID().toString())
                .expiraEn(java.time.Instant.now().plusSeconds(appProperties.getSecurity().getAccountRecovery().getExpirationMinutes() * 60L))
                .build();
        passwordRecoveryChallengeRepository.save(challenge);

        auditoriaService.registrarDetallado(usuario.getIdInstitucion(), usuario.getId(), "AUTH",
                "PASSWORD_RECOVERY_REQUEST", "password_recovery_challenge", challenge.getId().toString(),
                null, Map.of("correo", usuario.getCorreo()), true,
                "Desafío de recuperación generado");

        log.info("Password recovery challenge generated for {} challengeId={} otp={}",
                usuario.getCorreo(), challenge.getId(), challenge.getCodigoVerificacion());

        return PasswordRecoveryResponse.builder()
                .mensaje("Se ha generado un desafío de recuperación")
                .challengeId(challenge.getId())
                .codigoVerificacionDebug(appProperties.getSecurity().getAccountRecovery().isExposeDebugData()
                        ? challenge.getCodigoVerificacion() : null)
                .build();
    }

    @Transactional
    public PasswordRecoveryResponse verifyPasswordRecovery(PasswordRecoveryVerifyRequest request) {
        PasswordRecoveryChallenge challenge = loadActiveChallenge(request.getChallengeId());
        validateNotExpired(challenge);

        challenge.setIntentosVerificacion(challenge.getIntentosVerificacion() + 1);
        if (challenge.getIntentosVerificacion() > appProperties.getSecurity().getAccountRecovery().getMaxAttempts()) {
            challenge.setUsado(true);
            passwordRecoveryChallengeRepository.save(challenge);
            auditoriaService.registrarDetallado(null, challenge.getIdUsuario(), "AUTH", "PASSWORD_RECOVERY_VERIFY",
                    "password_recovery_challenge", challenge.getId().toString(),
                    Map.of("intentos", challenge.getIntentosVerificacion()), null,
                    false, "Se excedió el máximo de intentos de verificación");
            throw new IllegalStateException("Se excedió el máximo de intentos de verificación");
        }

        if (!Objects.equals(challenge.getCodigoVerificacion(), request.getCodigoVerificacion())) {
            passwordRecoveryChallengeRepository.save(challenge);
            auditoriaService.registrarDetallado(null, challenge.getIdUsuario(), "AUTH", "PASSWORD_RECOVERY_VERIFY",
                    "password_recovery_challenge", challenge.getId().toString(),
                    Map.of("codigoIngresado", request.getCodigoVerificacion()), null,
                    false, "Código de verificación incorrecto");
            throw new IllegalArgumentException("Código de verificación incorrecto");
        }

        challenge.setVerificado(true);
        challenge.setVerificadoEn(java.time.Instant.now());
        passwordRecoveryChallengeRepository.save(challenge);
        auditoriaService.registrarDetallado(null, challenge.getIdUsuario(), "AUTH", "PASSWORD_RECOVERY_VERIFY",
                "password_recovery_challenge", challenge.getId().toString(),
                null, Map.of("verificado", true), true,
                "Código de verificación validado");

        return PasswordRecoveryResponse.builder()
                .mensaje("Código validado correctamente")
                .challengeId(challenge.getId())
                .recoveryToken(challenge.getTokenRecuperacion())
                .build();
    }

    @Transactional
    public PasswordRecoveryResponse resetPassword(PasswordResetRequest request) {
        PasswordRecoveryChallenge challenge = loadActiveChallenge(request.getChallengeId());
        validateNotExpired(challenge);

        if (!challenge.isVerificado()) {
            throw new IllegalStateException("Debes verificar el código antes de cambiar la contraseña");
        }
        if (!Objects.equals(challenge.getTokenRecuperacion(), request.getRecoveryToken())) {
            auditoriaService.registrarDetallado(null, challenge.getIdUsuario(), "AUTH", "PASSWORD_RESET",
                    "password_recovery_challenge", challenge.getId().toString(),
                    null, null, false, "Token de recuperación inválido");
            throw new IllegalArgumentException("Token de recuperación inválido");
        }

        Usuario usuario = usuarioRepository.findById(challenge.getIdUsuario())
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado para el desafío de recuperación"));

        String oldHash = usuario.getHashContrasena();
        usuario.setHashContrasena(passwordEncoder.encode(request.getNuevaContrasena()));
        usuario.setRequiereCambioContrasena(false);
        usuarioRepository.save(usuario);

        challenge.setUsado(true);
        challenge.setUsadoEn(java.time.Instant.now());
        passwordRecoveryChallengeRepository.save(challenge);

        auditoriaService.registrarDetallado(usuario.getIdInstitucion(), usuario.getId(), "AUTH", "PASSWORD_RESET",
                "usuario", usuario.getId().toString(),
                Map.of("hashContrasena", oldHash), Map.of("hashContrasena", usuario.getHashContrasena()),
                true, "Contraseña restablecida mediante recuperación de cuenta");

        return PasswordRecoveryResponse.builder()
                .mensaje("Contraseña actualizada correctamente")
                .challengeId(challenge.getId())
                .build();
    }

    private AuthResponse buildAuthResponse(Usuario usuario) {
        List<String> roles = usuario.getRoles().stream()
                .map(Rol::getCodigo)
                .collect(Collectors.toList());
        List<String> permisos = usuario.getRoles().stream()
                .flatMap(rol -> rol.getPermisos().stream())
                .map(Permiso::getCodigo)
                .distinct()
                .toList();

        Map<String, Object> claims = new HashMap<>();
        if (usuario.getIdInstitucion() != null) {
            claims.put("id_institucion", usuario.getIdInstitucion().toString());
            // Incluir plan y módulos activos si la institución tiene suscripción vigente
            suscripcionRepo.findActivaByIdInstitucion(usuario.getIdInstitucion()).ifPresent(s -> {
                claims.put("plan_codigo", s.getPlan().getCodigo());
                List<String> modulos = s.getPlan().getModulos().stream()
                        .filter(m -> "ACTIVO".equals(m.getEstado()))
                        .map(m -> m.getCodigo())
                        .toList();
                claims.put("modulos_activos", modulos);
            });
        }
        claims.put("roles", roles);
        claims.put("permisos", permisos);

        String token = jwtService.generateToken(claims, usuario);

        return AuthResponse.builder()
                .token(token)
                .correo(usuario.getCorreo())
                .roles(roles)
                .permisos(permisos)
                .requiereCambioContrasena(usuario.isRequiereCambioContrasena())
                .build();
    }

    private PasswordRecoveryChallenge loadActiveChallenge(UUID challengeId) {
        if (challengeId == null) {
            throw new IllegalArgumentException("Debes indicar un challengeId válido");
        }
        return passwordRecoveryChallengeRepository.findByIdAndUsadoFalse(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Desafío de recuperación no encontrado o ya utilizado"));
    }

    private void validateNotExpired(PasswordRecoveryChallenge challenge) {
        if (challenge.getExpiraEn().isBefore(java.time.Instant.now())) {
            throw new IllegalStateException("El desafío de recuperación ha expirado");
        }
    }

    private String generateOtp() {
        return String.format("%06d", new Random().nextInt(1_000_000));
    }
}
