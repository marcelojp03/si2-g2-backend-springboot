package com.uagrm.si2g2.calificacion.dto;

import com.uagrm.si2g2.calificacion.domain.EvaluacionMateria;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class EvaluacionMateriaResponse {
    UUID id;
    UUID idInstitucion;
    UUID idMateria;
    UUID creadoPor;
    Integer periodo;
    String tipo;
    String nombre;
    BigDecimal ponderacion;
    String escala;
    String estado;
    Instant creadoEn;
    Instant actualizadoEn;

    public static EvaluacionMateriaResponse from(EvaluacionMateria evaluacion) {
        return EvaluacionMateriaResponse.builder()
                .id(evaluacion.getId())
                .idInstitucion(evaluacion.getIdInstitucion())
                .idMateria(evaluacion.getIdMateria())
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
