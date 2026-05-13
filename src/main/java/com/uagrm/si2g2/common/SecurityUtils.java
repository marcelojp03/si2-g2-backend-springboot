package com.uagrm.si2g2.common;

import com.uagrm.si2g2.auth.domain.Usuario;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {}

    /** Devuelve el UUID del usuario autenticado en el request actual, o null si no hay sesión. */
    public static UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Usuario u) {
            return u.getId();
        }
        return null;
    }

    public static Usuario currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Usuario u) {
            return u;
        }
        return null;
    }

    public static boolean currentUserHasRole(String roleCode) {
        Usuario user = currentUser();
        return user != null && user.getRoles().stream().anyMatch(role -> roleCode.equals(role.getCodigo()));
    }

    public static boolean currentUserHasAuthority(String authority) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(granted -> authority.equals(granted.getAuthority()));
    }

    public static UUID requireCurrentInstitutionId() {
        Usuario user = currentUser();
        if (user == null || user.getIdInstitucion() == null) {
            throw new AccessDeniedException("El usuario autenticado no tiene una institución asociada");
        }
        return user.getIdInstitucion();
    }
}
