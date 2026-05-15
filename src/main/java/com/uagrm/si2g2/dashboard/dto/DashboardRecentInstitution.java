package com.uagrm.si2g2.dashboard.dto;

import java.time.Instant;
import java.util.UUID;

public record DashboardRecentInstitution(
        UUID id,
        String codigo,
        String nombre,
        String tipoInstitucion,
        String estado,
        String direccion,
        long usuariosActivos,
        Instant creadoEn
) {
}
