package com.uagrm.si2g2.calificacion.dto;

import java.math.BigDecimal;
import java.util.List;

public record ConsolidadoTrimestralDirectorResponse(
        int totalMaterias,
        int materiasCompletas,
        int materiasConPendientes,
        int estudiantesSinAutoevaluacion,
        int docentesConSerPendiente,
        int estudiantesEnRiesgo,
        BigDecimal promedioGeneral,
        List<ConsolidadoTrimestralMateriaResponse> materias) {
}