package com.uagrm.si2g2.auth.application;

import com.uagrm.si2g2.auditoria.application.AuditoriaService;
import com.uagrm.si2g2.auth.domain.PermisoRepository;
import com.uagrm.si2g2.auth.domain.Rol;
import com.uagrm.si2g2.auth.domain.RolRepository;
import com.uagrm.si2g2.auth.domain.Usuario;
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

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RolRepository rolRepository;

    @Mock
    private PermisoRepository permisoRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private RoleService roleService;

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
    void shouldRejectAssigningRoleFromAnotherInstitution() {
        Rol foreignRole = Rol.builder()
                .id(UUID.randomUUID())
                .codigo("INST_OTHER_COORD")
                .nombre("Coordinador")
                .idInstitucion(UUID.randomUUID())
                .esGlobal(false)
                .build();

        when(rolRepository.findById(foreignRole.getId())).thenReturn(Optional.of(foreignRole));

        assertThrows(AccessDeniedException.class,
                () -> roleService.resolveAssignableRole(foreignRole.getId(), null));
    }
}
