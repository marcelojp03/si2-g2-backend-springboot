package com.uagrm.si2g2.ia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request para el endpoint FastAPI POST /api/ia/reporte/nl-a-reporte.
 * Contiene la consulta en lenguaje natural que el usuario desea transformar en un reporte.
 */
public record ReporteNlIaRequest(
        @NotBlank @Size(max = 500) String consulta
) {}
