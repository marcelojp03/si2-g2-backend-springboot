package com.uagrm.si2g2.asistencia.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class AsistenciaPlantillaResponse {

    private UUID idAsistenciaRegistro;
    private UUID idAsignacionDocente;
    private LocalDate fecha;
    private String estadoRegistro;
    private boolean registrada;

    private AsistenciaAsignacionResponse asignacion;
    private List<AsistenciaEstudianteResponse> estudiantes;

    private int totalEstudiantes;
}