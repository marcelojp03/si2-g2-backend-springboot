package com.uagrm.si2g2.calificacion.dto;

import jakarta.validation.constraints.NotNull;

<<<<<<< HEAD
=======
import java.time.LocalDate;
>>>>>>> d46e179 (feat: dimensiones dinámicas, validación de períodos, permisos granulares, seed sintético)
import java.util.UUID;

public record ActividadEvaluativaRequest(
        @NotNull UUID idMateria,
        @NotNull UUID idDocente,
        @NotNull String nombreActividad,
        @NotNull String dimension,
<<<<<<< HEAD
        String fechaActividad,
        String descripcion,
=======
        @NotNull LocalDate fechaActividad,
        String descripcionEvidencia,
        Integer puntajeMaximo,
>>>>>>> d46e179 (feat: dimensiones dinámicas, validación de períodos, permisos granulares, seed sintético)
        String estado) {
}
