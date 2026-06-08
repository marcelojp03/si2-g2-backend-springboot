package com.uagrm.si2g2.calificacion.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record ActividadEvaluativaRequest(
        @NotNull UUID idMateria,
        @NotNull UUID idDocente,
        @NotNull String nombreActividad,
        @NotNull String dimension,
        @NotNull LocalDate fechaActividad,
        String descripcionEvidencia,
        Integer puntajeMaximo,
        String estado) {
}
