package com.uagrm.si2g2.calificacion.dto;

import com.uagrm.si2g2.calificacion.domain.ActividadEvaluativa;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ActividadEvaluativaResponse(
        UUID id,
        UUID idPeriodoEvaluacion,
        UUID idMateria,
        UUID idDocente,
        String nombreActividad,
        String dimension,
        LocalDate fechaActividad,
        String descripcionEvidencia,
        Integer puntajeMaximo,
        String estado,
        Instant publicadoEn,
        Instant creadoEn,
        Instant actualizadoEn) {

    public static ActividadEvaluativaResponse from(ActividadEvaluativa actividad) {
        return new ActividadEvaluativaResponse(
                actividad.getId(),
                actividad.getIdPeriodoEvaluacion(),
                actividad.getIdMateria(),
                actividad.getIdDocente(),
                actividad.getNombreActividad(),
                actividad.getDimension(),
                actividad.getFechaActividad(),
                actividad.getDescripcionEvidencia(),
                actividad.getPuntajeMaximo(),
                actividad.getEstado(),
                actividad.getPublicadoEn(),
                actividad.getCreadoEn(),
                actividad.getActualizadoEn());
    }
}