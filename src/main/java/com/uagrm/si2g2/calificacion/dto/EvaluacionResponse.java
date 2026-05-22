package com.uagrm.si2g2.calificacion.dto;

import com.uagrm.si2g2.calificacion.domain.Evaluacion;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class EvaluacionResponse {
    UUID id;
    UUID idInstitucion;
    UUID idAsignacionDocente;
    UUID creadoPor;
    Integer periodo;
    String tipo;
    String nombre;
    BigDecimal ponderacion;
    String escala;
    String estado;
    Instant creadoEn;
    Instant actualizadoEn;

    public static EvaluacionResponse from(Evaluacion evaluacion) {
        return EvaluacionResponse.builder()
                .id(evaluacion.getId())
                .idInstitucion(evaluacion.getIdInstitucion())
                .idAsignacionDocente(evaluacion.getIdAsignacionDocente())
                .creadoPor(evaluacion.getCreadoPor())
                .periodo(evaluacion.getPeriodo())
                .tipo(evaluacion.getTipo())
                .nombre(evaluacion.getNombre())
                .ponderacion(evaluacion.getPonderacion())
                .escala(evaluacion.getEscala())
                .estado(evaluacion.getEstado())
                .creadoEn(evaluacion.getCreadoEn())
                .actualizadoEn(evaluacion.getActualizadoEn())
                .build();
    }
}
