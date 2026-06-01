package com.uagrm.si2g2.reporte.application;

import com.uagrm.si2g2.reporte.dto.ReporteMetadataResponse;
import com.uagrm.si2g2.reporte.dto.ReportePreviewResponse;

public interface ReporteHandler {
    String codigo();
    ReporteMetadataResponse metadata();
    ReportePreviewResponse ejecutar(ReporteExecutionContext context);
}
