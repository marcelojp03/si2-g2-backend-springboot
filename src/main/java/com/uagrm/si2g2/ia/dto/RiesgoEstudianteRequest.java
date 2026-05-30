package com.uagrm.si2g2.ia.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record RiesgoEstudianteRequest(
        @NotBlank String id_estudiante,
        @DecimalMin("0") @DecimalMax("100") double porcentaje_asistencia,
        @DecimalMin("0") @DecimalMax("100") double promedio_calificaciones,
        @Min(0) int evaluaciones_pendientes,
        @Min(0) int materias_reprobadas_historial
) {}
