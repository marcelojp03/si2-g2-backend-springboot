package com.uagrm.si2g2.calificacion.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record ActividadEvaluativaRequest(
        @NotNull UUID idGestionAcademica,
        @NotNull Integer trimestre,
        @NotNull UUID idCurso,
        @NotNull UUID idParalelo,
        @NotNull UUID idMateria,
        @NotNull UUID idDocente,
        @NotNull String nombreActividad,
        @NotNull String tipoActividad,
        @NotNull String dimension,
        Instant fechaActividad,
        String descripcion,
        String estado) {
}