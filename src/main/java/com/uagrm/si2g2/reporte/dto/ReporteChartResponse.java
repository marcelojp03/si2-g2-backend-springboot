package com.uagrm.si2g2.reporte.dto;

import java.util.List;

public record ReporteChartResponse(String type, List<String> labels, List<ReporteDatasetResponse> datasets) {
}
