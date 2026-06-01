package com.uagrm.si2g2.reporte.dto;

import java.math.BigDecimal;
import java.util.List;

public record ReporteDatasetResponse(String label, List<BigDecimal> data, String color) {
}
