package com.uagrm.si2g2.reporte.dto;

import jakarta.validation.constraints.NotBlank;

public record ReporteNaturalLanguageRequest(
        @NotBlank String consulta,
        ReportePresentacionRequest presentacion,
        Integer page,
        Integer size
) {
}
