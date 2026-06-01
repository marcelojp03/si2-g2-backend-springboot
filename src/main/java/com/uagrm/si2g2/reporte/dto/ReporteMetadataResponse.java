package com.uagrm.si2g2.reporte.dto;

import java.util.List;

public record ReporteMetadataResponse(
        String codigo,
        String nombre,
        String descripcion,
        String tipo,
        boolean grafico,
        List<ReporteFilterDefinitionResponse> filtros,
        List<String> formatosExportacion
) {
}
