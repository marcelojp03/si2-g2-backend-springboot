package com.uagrm.si2g2.estudiante.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record HistorialEvaluacionResponse(
        UUID idEvaluacion,
        String nombre,
        String tipo,
        Integer periodo,
        BigDecimal ponderacion,
        BigDecimal notaNumerica,
        String notaLiteral
) {}
