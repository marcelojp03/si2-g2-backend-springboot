package com.uagrm.si2g2.calificacion.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record ObservacionSerRequest(
        @NotNull UUID idEstudiante,
        @NotNull UUID idMateria,
        @NotNull LocalDate fechaObservacion,
        @NotNull String comportamiento,
        String descripcion) {
}