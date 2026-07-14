package com.uagrm.si2g2.alertas.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record AnalisisRiesgoResponse(
        int totalEstudiantes,
        Map<String, Long> distribucionNivel,
        List<ResumenRiesgoParalelo> comparativaParalelos,
        List<RiesgoEstudianteDetalleResponse> estudiantes,
        Instant generadoEn
) {}
