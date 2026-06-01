package com.uagrm.si2g2.reporte.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record ReportePreviewRequest(
        @NotBlank String codigoReporte,
        Map<String, Object> filtros,
        ReportePresentacionRequest presentacion,
        Integer page,
        Integer size
) {
}
