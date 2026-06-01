package com.uagrm.si2g2.reporte.dto;

import java.util.List;

public record ReporteFilterDefinitionResponse(
        String field,
        String label,
        String type,
        boolean required,
        List<ReporteFilterOptionResponse> options
) {
}
