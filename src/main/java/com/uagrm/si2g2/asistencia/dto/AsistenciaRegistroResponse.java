package com.uagrm.si2g2.asistencia.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class AsistenciaRegistroResponse {

    private UUID id;
    private UUID idInstitucion;
    private UUID idAsignacionDocente;
    private UUID registradoPor;

    private LocalDate fecha;
    private String estado;

    private AsistenciaAsignacionResponse asignacion;
    private List<AsistenciaEstudianteResponse> detalles;

    private long totalPresentes;
    private long totalAusentes;
    private long totalTardanzas;
    private long totalJustificados;
    private int totalEstudiantes;

    private Instant creadoEn;
    private Instant actualizadoEn;
}