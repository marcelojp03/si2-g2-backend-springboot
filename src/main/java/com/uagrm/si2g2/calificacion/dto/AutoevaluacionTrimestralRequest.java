package com.uagrm.si2g2.calificacion.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record AutoevaluacionTrimestralRequest(
        @NotNull UUID idEstudiante,
        @NotNull UUID idMateria,
        @NotNull BigDecimal notaAutoevaluacion,
        String comentario) {
}