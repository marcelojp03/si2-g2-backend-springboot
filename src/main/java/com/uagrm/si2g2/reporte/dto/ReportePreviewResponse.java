package com.uagrm.si2g2.reporte.dto;

import java.util.List;
import java.util.Map;

public record ReportePreviewResponse(
        ReporteHeaderResponse encabezado,
        List<ReporteColumnResponse> columnas,
        List<Map<String, Object>> filas,
        ReporteChartResponse grafico,
        long totalRegistros,
        int page,
        int size
) {
}
