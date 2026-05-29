package com.uagrm.si2g2.calificacion.dto;

import com.uagrm.si2g2.calificacion.domain.CalificacionSer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CalificacionSerResponse(
        UUID id,
        UUID idGestionAcademica,
        UUID idTrimestre,
        UUID idCurso,
        UUID idParalelo,
        UUID idMateria,
        UUID idDocente,
        UUID idEstudiante,
        BigDecimal notaSer,
        String observacion,
        String estado,
        UUID idUsuarioRegistro,
        UUID idUsuarioModificacion,
        Instant creadoEn,
        Instant actualizadoEn) {

    public static CalificacionSerResponse from(CalificacionSer calificacion) {
        return new CalificacionSerResponse(
                calificacion.getId(),
                calificacion.getIdGestionAcademica(),
                calificacion.getIdTrimestre(),
                calificacion.getIdCurso(),
                calificacion.getIdParalelo(),
                calificacion.getIdMateria(),
                calificacion.getIdDocente(),
                calificacion.getIdEstudiante(),
                calificacion.getNotaSer(),
                calificacion.getObservacion(),
                calificacion.getEstado(),
                calificacion.getIdUsuarioRegistro(),
                calificacion.getIdUsuarioModificacion(),
                calificacion.getCreadoEn(),
                calificacion.getActualizadoEn());
    }
}