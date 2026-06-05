package com.uagrm.si2g2.calificacion.dto;

import com.uagrm.si2g2.calificacion.domain.AutoevaluacionTrimestral;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AutoevaluacionTrimestralResponse(
        UUID id,
        UUID idPeriodoEvaluacion,
        UUID idEstudiante,
        UUID idMateria,
        BigDecimal notaAutoevaluacion,
        String comentario,
        String estado,
        UUID idUsuarioRegistro,
        UUID idUsuarioModificacion,
        Instant creadoEn,
        Instant actualizadoEn) {

    public static AutoevaluacionTrimestralResponse from(AutoevaluacionTrimestral autoevaluacion) {
        return new AutoevaluacionTrimestralResponse(
                autoevaluacion.getId(),
                autoevaluacion.getIdPeriodoEvaluacion(),
                autoevaluacion.getIdEstudiante(),
                autoevaluacion.getIdMateria(),
                autoevaluacion.getNotaAutoevaluacion(),
                autoevaluacion.getComentario(),
                autoevaluacion.getEstado(),
                autoevaluacion.getIdUsuarioRegistro(),
                autoevaluacion.getIdUsuarioModificacion(),
                autoevaluacion.getCreadoEn(),
                autoevaluacion.getActualizadoEn());
    }
}