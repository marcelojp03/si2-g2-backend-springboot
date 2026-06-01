package com.uagrm.si2g2.reporte.export;

import com.uagrm.si2g2.reporte.dto.ReportePreviewResponse;

public interface ReporteExporter {
    String formato();
    String contentType();
    String extension();
    byte[] exportar(ReportePreviewResponse reporte);
}
