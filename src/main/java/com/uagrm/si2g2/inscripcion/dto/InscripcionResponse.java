package com.uagrm.si2g2.inscripcion.dto;

import com.uagrm.si2g2.inscripcion.domain.Inscripcion;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class InscripcionResponse {

    private UUID id;
    private UUID idInstitucion;
    private UUID idEstudiante;
    private UUID idGestion;
    private UUID idCurso;
    private UUID idParalelo;
    private LocalDate fechaInscripcion;
    private String estado;
    private Instant creadoEn;

    public static InscripcionResponse from(Inscripcion i) {
        return InscripcionResponse.builder()
                .id(i.getId())
                .idInstitucion(i.getIdInstitucion())
                .idEstudiante(i.getIdEstudiante())
                .idGestion(i.getIdGestion())
                .idCurso(i.getIdCurso())
                .idParalelo(i.getIdParalelo())
                .fechaInscripcion(i.getFechaInscripcion())
                .estado(i.getEstado())
                .creadoEn(i.getCreadoEn())
                .build();
    }
}
