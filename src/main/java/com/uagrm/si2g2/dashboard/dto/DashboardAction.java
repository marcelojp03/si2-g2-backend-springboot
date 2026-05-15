package com.uagrm.si2g2.dashboard.dto;

public record DashboardAction(
        String codigo,
        String titulo,
        String descripcion,
        String icono,
        String ruta,
        String severidad
) {
}
