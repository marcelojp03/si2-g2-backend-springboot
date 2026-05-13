package com.uagrm.si2g2.auth.application;

import com.uagrm.si2g2.auditoria.application.AuditoriaService;
import com.uagrm.si2g2.auth.domain.PasswordRecoveryChallenge;
import com.uagrm.si2g2.auth.domain.PasswordRecoveryChallengeRepository;
import com.uagrm.si2g2.auth.domain.Rol;
import com.uagrm.si2g2.auth.domain.Usuario;
import com.uagrm.si2g2.auth.domain.UsuarioRepository;
import com.uagrm.si2g2.auth.dto.PasswordRecoveryRequest;
import com.uagrm.si2g2.auth.dto.PasswordRecoveryResponse;
import com.uagrm.si2g2.auth.dto.RegisterRequest;
import com.uagrm.si2g2.config.AppProperties;
import com.uagrm.si2g2.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordRecoveryChallengeRepository passwordRecoveryChallengeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private AuditoriaService auditoriaService;

    @Mock
    private RoleService roleService;

    private AuthService authService;

    private AppProperties appProperties;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.getSecurity().getAccountRecovery().setExposeDebugData(true);
        tenantId = UUID.randomUUID();
        Usuario user = Usuario.builder()
                .id(UUID.randomUUID())
                .idInstitucion(tenantId)
                .correo("admin@test.com")
                .hashContrasena("secret")
                .nombres("Admin")
                .apellidos("Tenant")
                .roles(Set.of(Rol.builder().codigo("ADMIN_INSTITUCION").nombre("Admin").build()))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
        authService = new AuthService(
                usuarioRepository,
                passwordRecoveryChallengeRepository,
                passwordEncoder,
                jwtService,
                authenticationManager,
                auditoriaService,
                roleService,
                appProperties
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRejectRegisteringUserForAnotherInstitution() {
        RegisterRequest request = new RegisterRequest();
        request.setCorreo("new@test.com");
        request.setContrasena("12345678");
        request.setNombres("Nuevo");
        request.setApellidos("Usuario");
        request.setIdInstitucion(UUID.randomUUID());
        request.setCodigoRol("DIRECTOR");

        when(usuarioRepository.existsByCorreo(request.getCorreo())).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> authService.register(request));
    }

    @Test
    void shouldGenerateRecoveryChallengeWithDebugCode() {
        Usuario usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .idInstitucion(tenantId)
                .correo("recover@test.com")
                .hashContrasena("hashed")
                .nombres("Recover")
                .apellidos("User")
                .roles(Set.of())
                .build();

        PasswordRecoveryRequest request = new PasswordRecoveryRequest();
        request.setCorreo(usuario.getCorreo());

        when(usuarioRepository.findByCorreo(usuario.getCorreo())).thenReturn(java.util.Optional.of(usuario));
        when(passwordRecoveryChallengeRepository.save(any(PasswordRecoveryChallenge.class)))
                .thenAnswer(invocation -> {
                    PasswordRecoveryChallenge challenge = invocation.getArgument(0);
                    challenge.setId(UUID.randomUUID());
                    return challenge;
                });

        PasswordRecoveryResponse response = authService.requestPasswordRecovery(request);

        assertNotNull(response.getChallengeId());
        assertNotNull(response.getCodigoVerificacionDebug());
        assertEquals(6, response.getCodigoVerificacionDebug().length());
        verify(passwordRecoveryChallengeRepository).save(any(PasswordRecoveryChallenge.class));
    }
}
