package com.uagrm.si2g2.dashboard.dto;

public record DashboardAlert(
        String id,
        String modulo,
        String severidad,
        String titulo,
        String detalle,
        String rutaAccion,
        String estado
) {
}
