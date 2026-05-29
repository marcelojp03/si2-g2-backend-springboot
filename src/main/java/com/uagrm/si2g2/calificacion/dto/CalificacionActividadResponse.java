package com.uagrm.si2g2.calificacion.dto;

import com.uagrm.si2g2.calificacion.domain.CalificacionActividad;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CalificacionActividadResponse(
        UUID id,
        UUID idActividad,
        UUID idEstudiante,
        BigDecimal notaObtenida,
        String observacion,
        String estado,
        UUID idUsuarioRegistro,
        UUID idUsuarioModificacion,
        Instant creadoEn,
        Instant actualizadoEn) {

    public static CalificacionActividadResponse from(CalificacionActividad calificacion) {
        return new CalificacionActividadResponse(
                calificacion.getId(),
                calificacion.getIdActividad(),
                calificacion.getIdEstudiante(),
                calificacion.getNotaObtenida(),
                calificacion.getObservacion(),
                calificacion.getEstado(),
                calificacion.getIdUsuarioRegistro(),
                calificacion.getIdUsuarioModificacion(),
                calificacion.getCreadoEn(),
                calificacion.getActualizadoEn());
    }
}