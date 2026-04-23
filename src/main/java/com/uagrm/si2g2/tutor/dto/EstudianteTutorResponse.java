package com.uagrm.si2g2.tutor.dto;

import com.uagrm.si2g2.tutor.domain.EstudianteTutor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class EstudianteTutorResponse {

    private UUID id;
    private UUID idInstitucion;
    private UUID idEstudiante;
    private UUID idTutor;
    private String parentesco;
    private boolean esPrincipal;
    private String estado;
    private Instant creadoEn;

    public static EstudianteTutorResponse from(EstudianteTutor et) {
        return EstudianteTutorResponse.builder()
                .id(et.getId())
                .idInstitucion(et.getIdInstitucion())
                .idEstudiante(et.getIdEstudiante())
                .idTutor(et.getIdTutor())
                .parentesco(et.getParentesco())
                .esPrincipal(et.isEsPrincipal())
                .estado(et.getEstado())
                .creadoEn(et.getCreadoEn())
                .build();
    }
}
