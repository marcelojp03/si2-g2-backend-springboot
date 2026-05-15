package com.uagrm.si2g2.dashboard.dto;

public record DashboardKpi(
        String codigo,
        String titulo,
        String valor,
        String subtitulo,
        String icono,
        String severidad,
        String rutaAccion
) {
}
