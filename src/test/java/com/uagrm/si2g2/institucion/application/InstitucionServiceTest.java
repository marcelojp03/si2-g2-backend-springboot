package com.uagrm.si2g2.institucion.application;

import com.uagrm.si2g2.auditoria.application.AuditoriaService;
import com.uagrm.si2g2.auth.domain.Rol;
import com.uagrm.si2g2.auth.domain.Usuario;
import com.uagrm.si2g2.institucion.domain.ConfiguracionInstitucionRepository;
import com.uagrm.si2g2.institucion.domain.InstitucionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class InstitucionServiceTest {

    @Mock
    private InstitucionRepository institucionRepository;

    @Mock
    private ConfiguracionInstitucionRepository configuracionRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private InstitucionService institucionService;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
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
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRejectAccessToDifferentInstitutionForNonSuperAdmin() {
        assertThrows(AccessDeniedException.class,
                () -> institucionService.listarConfiguraciones(UUID.randomUUID()));
    }
}
