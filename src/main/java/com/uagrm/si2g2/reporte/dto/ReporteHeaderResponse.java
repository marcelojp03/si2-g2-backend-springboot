package com.uagrm.si2g2.reporte.dto;

import java.time.Instant;
import java.util.List;

public record ReporteHeaderResponse(
        String nombreReporte,
        Instant generadoEn,
        String usuario,
        List<String> filtrosAplicados
) {
}
