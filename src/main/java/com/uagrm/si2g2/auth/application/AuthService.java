package com.uagrm.si2g2.auth.application;

import com.uagrm.si2g2.auth.domain.Rol;
import com.uagrm.si2g2.auth.domain.RolRepository;
import com.uagrm.si2g2.auth.domain.Usuario;
import com.uagrm.si2g2.auth.domain.UsuarioRepository;
import com.uagrm.si2g2.auth.dto.AuthResponse;
import com.uagrm.si2g2.auth.dto.LoginRequest;
import com.uagrm.si2g2.auth.dto.RegisterRequest;
import com.uagrm.si2g2.auditoria.application.AuditoriaService;
import com.uagrm.si2g2.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AuditoriaService auditoriaService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            log.warn("Intento de registro con correo duplicado: {}", request.getCorreo());
            throw new IllegalStateException("Ya existe un usuario con el correo: " + request.getCorreo());
        }

        String codigoRol = (request.getCodigoRol() != null && !request.getCodigoRol().isBlank())
                ? request.getCodigoRol()
                : "ADMIN_INSTITUCION";
        Rol rol = rolRepository.findByCodigo(codigoRol)
                .orElseThrow(() -> new IllegalStateException(
                        "Rol '" + codigoRol + "' no encontrado. Ejecute db-script.sql primero."));

        Usuario usuario = Usuario.builder()
                .idInstitucion(request.getIdInstitucion())
                .correo(request.getCorreo())
                .hashContrasena(passwordEncoder.encode(request.getContrasena()))
                .nombres(request.getNombres())
                .apellidos(request.getApellidos())
                .roles(Set.of(rol))
                .build();

        usuarioRepository.save(usuario);
        log.info("Register exitoso: correo={}, rol={}", usuario.getCorreo(), codigoRol);
        auditoriaService.registrar(usuario.getIdInstitucion(), usuario.getId(),
                "AUTH", "REGISTER", "usuario", usuario.getId().toString(),
                true, "Rol asignado: " + codigoRol);

        return buildAuthResponse(usuario);
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getCorreo(), request.getContrasena())
            );
        } catch (BadCredentialsException e) {
            log.warn("Login fallido: correo={}", request.getCorreo());
            auditoriaService.registrar(null, null,
                    "AUTH", "LOGIN_FALLIDO", "usuario", null,
                    false, "Credenciales incorrectas para: " + request.getCorreo());
            throw e;
        }

        Usuario usuario = usuarioRepository.findByCorreo(request.getCorreo())
                .orElseThrow();

        log.info("Login exitoso: correo={}, roles={}", usuario.getCorreo(),
                usuario.getRoles().stream().map(Rol::getCodigo).collect(Collectors.joining(",")));
        auditoriaService.registrar(usuario.getIdInstitucion(), usuario.getId(),
                "AUTH", "LOGIN_EXITOSO", "usuario", usuario.getId().toString(),
                true, null);

        return buildAuthResponse(usuario);
    }

    private AuthResponse buildAuthResponse(Usuario usuario) {
        List<String> roles = usuario.getRoles().stream()
                .map(Rol::getCodigo)
                .collect(Collectors.toList());

        Map<String, Object> claims = new HashMap<>();
        if (usuario.getIdInstitucion() != null) {
            claims.put("id_institucion", usuario.getIdInstitucion().toString());
        }
        claims.put("roles", roles);

        String token = jwtService.generateToken(claims, usuario);

        return AuthResponse.builder()
                .token(token)
                .correo(usuario.getCorreo())
                .roles(roles)
                .requiereCambioContrasena(usuario.isRequiereCambioContrasena())
                .build();
    }
}
