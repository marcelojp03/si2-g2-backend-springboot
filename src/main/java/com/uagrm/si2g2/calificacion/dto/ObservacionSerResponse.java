package com.uagrm.si2g2.calificacion.dto;

import com.uagrm.si2g2.calificacion.domain.ObservacionSer;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ObservacionSerResponse(
        UUID id,
        UUID idPeriodoEvaluacion,
        UUID idEstudiante,
        UUID idDocente,
        UUID idMateria,
        LocalDate fechaObservacion,
        String comportamiento,
        String descripcion,
        Instant creadoEn) {

    public static ObservacionSerResponse from(ObservacionSer observacion) {
        return new ObservacionSerResponse(
                observacion.getId(),
                observacion.getIdPeriodoEvaluacion(),
                observacion.getIdEstudiante(),
                observacion.getIdDocente(),
                observacion.getIdMateria(),
                observacion.getFechaObservacion(),
                observacion.getComportamiento(),
                observacion.getDescripcion(),
                observacion.getCreadoEn());
    }
}