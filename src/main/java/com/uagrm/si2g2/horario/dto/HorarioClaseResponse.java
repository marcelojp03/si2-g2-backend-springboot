package com.uagrm.si2g2.horario.dto;

import com.uagrm.si2g2.horario.domain.HorarioClase;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class HorarioClaseResponse {

    private UUID id;
    private UUID idInstitucion;
    private UUID idAsignacionDocente;
    private UUID idAula;
    private String diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String estado;
    private OffsetDateTime creadoEn;
    private OffsetDateTime actualizadoEn;

    public static HorarioClaseResponse from(HorarioClase horario) {
        return HorarioClaseResponse.builder()
                .id(horario.getId())
                .idInstitucion(horario.getIdInstitucion())
                .idAsignacionDocente(horario.getIdAsignacionDocente())
                .idAula(horario.getIdAula())
                .diaSemana(horario.getDiaSemana())
                .horaInicio(horario.getHoraInicio())
                .horaFin(horario.getHoraFin())
                .estado(horario.getEstado())
                .creadoEn(horario.getCreadoEn())
                .actualizadoEn(horario.getActualizadoEn())
                .build();
    }
}
