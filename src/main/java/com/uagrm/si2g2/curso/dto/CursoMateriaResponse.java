package com.uagrm.si2g2.curso.dto;

import com.uagrm.si2g2.curso.domain.CursoMateria;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class CursoMateriaResponse {

    private UUID id;
    private UUID idInstitucion;
    private UUID idCurso;
    private UUID idMateria;
    private String estado;
    private Instant creadoEn;

    public static CursoMateriaResponse from(CursoMateria cm) {
        return CursoMateriaResponse.builder()
                .id(cm.getId())
                .idInstitucion(cm.getIdInstitucion())
                .idCurso(cm.getIdCurso())
                .idMateria(cm.getIdMateria())
                .estado(cm.getEstado())
                .creadoEn(cm.getCreadoEn())
                .build();
    }
}
