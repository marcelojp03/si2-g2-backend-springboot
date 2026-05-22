package com.uagrm.si2g2.calificacion.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class CalificacionResumenResponse {
    UUID idAsignacionDocente;
    Integer periodo;
    BigDecimal ponderacionTotal;
    BigDecimal notaMinimaAprobacion;
    List<EvaluacionResponse> evaluaciones;
    List<CalificacionResumenEstudianteResponse> estudiantes;
}
