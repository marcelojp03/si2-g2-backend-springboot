package com.uagrm.si2g2.academico.dto;

import com.uagrm.si2g2.academico.domain.GestionAcademica;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class GestionAcademicaResponse {

    private UUID id;
    private UUID idInstitucion;
    private String nombre;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private boolean activa;
    private String estado;
    private Instant creadoEn;
    private Instant actualizadoEn;

    public static GestionAcademicaResponse from(GestionAcademica g) {
        return GestionAcademicaResponse.builder()
                .id(g.getId())
                .idInstitucion(g.getIdInstitucion())
                .nombre(g.getNombre())
                .fechaInicio(g.getFechaInicio())
                .fechaFin(g.getFechaFin())
                .activa(g.isActiva())
                .estado(g.getEstado())
                .creadoEn(g.getCreadoEn())
                .actualizadoEn(g.getActualizadoEn())
                .build();
    }
}
