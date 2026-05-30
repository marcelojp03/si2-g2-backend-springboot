package com.uagrm.si2g2.tenant;

import java.util.UUID;

/**
 * Almacena el id_institucion del usuario autenticado para el request actual.
 * Llenado por JwtAuthFilter, limpiado al finalizar el request.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(UUID idInstitucion) {
        CURRENT.set(idInstitucion);
    }

    public static UUID get() {
        return CURRENT.get();
    }

    /**
     * Retorna el id_institucion del contexto actual o lanza AccessDeniedException
     * si no hay ninguno (e.g. SUPER_ADMIN sin institución activa o endpoint no autenticado).
     */
    public static UUID getOrThrow() {
        UUID id = CURRENT.get();
        if (id == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Operación requiere contexto de institución (id_institucion no presente en JWT)");
        }
        return id;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
