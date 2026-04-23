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

    public static void clear() {
        CURRENT.remove();
    }
}
