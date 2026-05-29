package com.uagrm.si2g2.auth.application;

import com.uagrm.si2g2.auth.domain.Rol;
import com.uagrm.si2g2.auth.domain.Usuario;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoleHierarchyTest {

    @Test
    void directorCannotAssignAdmin() {
        Usuario director = userWithRole("DIRECTOR");
        Rol admin = globalRole("ADMIN_INSTITUCION");
        assertFalse(RoleHierarchy.canAssign(director, admin));
    }

    @Test
    void directorCanAssignSecretario() {
        Usuario director = userWithRole("DIRECTOR");
        Rol secretario = globalRole("SECRETARIO");
        assertTrue(RoleHierarchy.canAssign(director, secretario));
    }

    @Test
    void secretarioCannotAssignDirector() {
        Usuario secretario = userWithRole("SECRETARIO");
        Rol director = globalRole("DIRECTOR");
        assertFalse(RoleHierarchy.canAssign(secretario, director));
    }

    @Test
    void adminCanAssignDirectorButNotAnotherAdmin() {
        Usuario admin = userWithRole("ADMIN_INSTITUCION");
        assertTrue(RoleHierarchy.canAssign(admin, globalRole("DIRECTOR")));
        assertFalse(RoleHierarchy.canAssign(admin, globalRole("ADMIN_INSTITUCION")));
    }

    @Test
    void institutionalRoleIsAssignableWhenHierarchyAllows() {
        Usuario director = userWithRole("DIRECTOR");
        Rol custom = Rol.builder().codigo("INST_ABC_COORD").nombre("Coordinador").esGlobal(false).build();
        assertTrue(RoleHierarchy.canAssign(director, custom));
    }

    private static Usuario userWithRole(String codigo) {
        return Usuario.builder()
                .correo("test@example.com")
                .hashContrasena("secret")
                .nombres("Test")
                .apellidos("User")
                .roles(Set.of(Rol.builder().codigo(codigo).nombre(codigo).esGlobal(true).build()))
                .build();
    }

    private static Rol globalRole(String codigo) {
        return Rol.builder().codigo(codigo).nombre(codigo).esGlobal(true).build();
    }
}
