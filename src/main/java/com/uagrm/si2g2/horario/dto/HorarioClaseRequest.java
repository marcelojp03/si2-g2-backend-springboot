package com.uagrm.si2g2.horario.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;
import java.util.UUID;

@Data
public class HorarioClaseRequest {

    @NotNull
    private UUID idInstitucion;

    @NotNull
    private UUID idAsignacionDocente;

    @NotNull
    private UUID idAula;

    @NotNull
    private String diaSemana;

    @NotNull
    private LocalTime horaInicio;

    @NotNull
    private LocalTime horaFin;
}
