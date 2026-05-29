package com.uagrm.si2g2.estudiante.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record HistorialMateriaResponse(
        UUID idMateria,
        String codigoMateria,
        String nombreMateria,
        UUID idAsignacion,
        BigDecimal promedioGeneral,
        List<HistorialEvaluacionResponse> evaluaciones,
        int totalSesiones,
        int sesionesPresente,
        BigDecimal porcentajeAsistencia
) {}
