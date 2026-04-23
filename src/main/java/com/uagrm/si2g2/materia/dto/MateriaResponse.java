package com.uagrm.si2g2.materia.dto;

import com.uagrm.si2g2.materia.domain.Materia;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class MateriaResponse {

    private UUID id;
    private UUID idInstitucion;
    private String codigo;
    private String nombre;
    private String descripcion;
    private Integer cargaHoraria;
    private String estado;
    private Instant creadoEn;
    private Instant actualizadoEn;

    public static MateriaResponse from(Materia m) {
        return MateriaResponse.builder()
                .id(m.getId())
                .idInstitucion(m.getIdInstitucion())
                .codigo(m.getCodigo())
                .nombre(m.getNombre())
                .descripcion(m.getDescripcion())
                .cargaHoraria(m.getCargaHoraria())
                .estado(m.getEstado())
                .creadoEn(m.getCreadoEn())
                .actualizadoEn(m.getActualizadoEn())
                .build();
    }
}
