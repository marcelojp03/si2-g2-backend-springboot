package com.uagrm.si2g2.auth.application;

import com.uagrm.si2g2.auth.domain.Rol;
import com.uagrm.si2g2.auth.domain.Usuario;

import java.util.Map;
import java.util.Set;

/**
 * Jerarquía de roles globales: un usuario solo puede asignar roles estrictamente inferiores al suyo.
 * Los roles institucionales personalizados (INST_*) no participan en la jerarquía numérica.
 */
public final class RoleHierarchy {

    private static final Map<String, Integer> GLOBAL_RANK = Map.of(
            "SUPER_ADMIN", 100,
            "ADMIN_INSTITUCION", 80,
            "DIRECTOR", 60,
            "SECRETARIO", 40,
            "DOCENTE", 30,
            "TUTOR", 20,
            "ESTUDIANTE", 10
    );

    private static final Set<String> PROTECTED_GLOBAL = Set.of("SUPER_ADMIN");

    private RoleHierarchy() {}

    public static int rankOf(String codigoRol) {
        return GLOBAL_RANK.getOrDefault(codigoRol, 0);
    }

    public static int maxRankOf(Usuario usuario) {
        if (usuario == null || usuario.getRoles() == null) {
            return 0;
        }
        return usuario.getRoles().stream()
                .mapToInt(rol -> rankOf(rol.getCodigo()))
                .max()
                .orElse(0);
    }

    public static boolean canAssign(Usuario assigner, Rol targetRole) {
        if (targetRole == null) {
            return false;
        }
        if (PROTECTED_GLOBAL.contains(targetRole.getCodigo())) {
            return false;
        }
        if (assigner == null) {
            return false;
        }
        if (maxRankOf(assigner) >= rankOf("SUPER_ADMIN")) {
            return true;
        }
        int assignerRank = maxRankOf(assigner);
        if (assignerRank <= 0) {
            return false;
        }
        if (targetRole.isEsGlobal()) {
            int targetRank = rankOf(targetRole.getCodigo());
            return targetRank > 0 && targetRank < assignerRank;
        }
        return true;
    }
}
