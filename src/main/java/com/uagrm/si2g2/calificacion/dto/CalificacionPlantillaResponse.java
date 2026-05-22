package com.uagrm.si2g2.calificacion.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class CalificacionPlantillaResponse {
    UUID idEvaluacion;
    EvaluacionResponse evaluacion;
    CalificacionAsignacionResponse asignacion;
    List<CalificacionEstudianteResponse> estudiantes;
    int totalEstudiantes;
    BigDecimal escalaMaxima;
    boolean puedeEditar;
}
