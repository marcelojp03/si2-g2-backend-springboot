package com.uagrm.si2g2.calificacion.dto;

import com.uagrm.si2g2.calificacion.domain.CalificacionSer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CalificacionSerResponse(
        UUID id,
        UUID idPeriodoEvaluacion,
        UUID idEstudiante,
        UUID idMateria,
        BigDecimal notaSer,
        String observacionFinal,
        String estado,
        UUID idUsuarioRegistro,
        UUID idUsuarioModificacion,
        Instant creadoEn,
        Instant actualizadoEn) {

    public static CalificacionSerResponse from(CalificacionSer calificacion) {
        return new CalificacionSerResponse(
                calificacion.getId(),
                calificacion.getIdPeriodoEvaluacion(),
                calificacion.getIdEstudiante(),
                calificacion.getIdMateria(),
                calificacion.getNotaSer(),
                calificacion.getObservacionFinal(),
                calificacion.getEstado(),
                calificacion.getIdUsuarioRegistro(),
                calificacion.getIdUsuarioModificacion(),
                calificacion.getCreadoEn(),
                calificacion.getActualizadoEn());
    }
}