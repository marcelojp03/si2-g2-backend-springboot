package com.uagrm.si2g2.curso.dto;

import com.uagrm.si2g2.curso.domain.Paralelo;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ParaleloResponse {

    private UUID id;
    private UUID idInstitucion;
    private UUID idCurso;
    private UUID idGestion;
    private String nombre;
    private Integer capacidad;
    private String estado;
    private Instant creadoEn;
    private Instant actualizadoEn;

    public static ParaleloResponse from(Paralelo p) {
        return ParaleloResponse.builder()
                .id(p.getId())
                .idInstitucion(p.getIdInstitucion())
                .idCurso(p.getIdCurso())
                .idGestion(p.getIdGestion())
                .nombre(p.getNombre())
                .capacidad(p.getCapacidad())
                .estado(p.getEstado())
                .creadoEn(p.getCreadoEn())
                .actualizadoEn(p.getActualizadoEn())
                .build();
    }
}
