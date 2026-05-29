package com.uagrm.si2g2.calificacion.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ConsolidadoTrimestralEstudianteResponse(
        UUID idEstudiante,
        String codigoEstudiante,
        String nombreCompleto,
        BigDecimal ser,
        BigDecimal promedioSaber,
        BigDecimal promedioHacer,
        BigDecimal autoevaluacion,
        BigDecimal totalParcial,
        BigDecimal totalFinal,
        String estado,
        String observacion) {
}