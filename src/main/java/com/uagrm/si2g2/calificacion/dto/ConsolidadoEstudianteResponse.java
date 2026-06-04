package com.uagrm.si2g2.calificacion.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ConsolidadoEstudianteResponse(
        UUID idEstudiante,
        String nombreEstudiante,
        BigDecimal saber,
        BigDecimal hacer,
        BigDecimal ser,
        BigDecimal autoevaluacion,
        BigDecimal total,
        boolean aprobado,
        String estado) {

    public static ConsolidadoEstudianteResponse calcular(
            UUID idEstudiante, String nombreEstudiante,
            BigDecimal saber, BigDecimal hacer, BigDecimal ser, BigDecimal autoevaluacion) {
        BigDecimal total = saber.add(hacer).add(ser).add(autoevaluacion);
        boolean aprobado = total.compareTo(new BigDecimal("51")) >= 0;
        String estado = total.compareTo(new BigDecimal("51")) >= 0 ? "APROBADO" : "REPROBADO";
        return new ConsolidadoEstudianteResponse(idEstudiante, nombreEstudiante, saber, hacer, ser, autoevaluacion, total, aprobado, estado);
    }
}