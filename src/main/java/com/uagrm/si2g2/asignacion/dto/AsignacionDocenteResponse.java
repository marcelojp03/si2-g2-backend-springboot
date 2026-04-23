package com.uagrm.si2g2.asignacion.dto;

import com.uagrm.si2g2.asignacion.domain.AsignacionDocente;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class AsignacionDocenteResponse {

    private UUID id;
    private UUID idInstitucion;
    private UUID idDocente;
    private UUID idMateria;
    private UUID idParalelo;
    private UUID idGestion;
    private String estado;
    private Instant creadoEn;

    public static AsignacionDocenteResponse from(AsignacionDocente a) {
        return AsignacionDocenteResponse.builder()
                .id(a.getId())
                .idInstitucion(a.getIdInstitucion())
                .idDocente(a.getIdDocente())
                .idMateria(a.getIdMateria())
                .idParalelo(a.getIdParalelo())
                .idGestion(a.getIdGestion())
                .estado(a.getEstado())
                .creadoEn(a.getCreadoEn())
                .build();
    }
}
