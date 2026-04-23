package com.uagrm.si2g2.curso.dto;

import com.uagrm.si2g2.curso.domain.Curso;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class CursoResponse {

    private UUID id;
    private UUID idInstitucion;
    private String codigo;
    private String nombre;
    private String nivel;
    private String estado;
    private Instant creadoEn;
    private Instant actualizadoEn;

    public static CursoResponse from(Curso c) {
        return CursoResponse.builder()
                .id(c.getId())
                .idInstitucion(c.getIdInstitucion())
                .codigo(c.getCodigo())
                .nombre(c.getNombre())
                .nivel(c.getNivel())
                .estado(c.getEstado())
                .creadoEn(c.getCreadoEn())
                .actualizadoEn(c.getActualizadoEn())
                .build();
    }
}
