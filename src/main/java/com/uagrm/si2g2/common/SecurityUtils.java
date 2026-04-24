package com.uagrm.si2g2.common;

import com.uagrm.si2g2.auth.domain.Usuario;
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
}
