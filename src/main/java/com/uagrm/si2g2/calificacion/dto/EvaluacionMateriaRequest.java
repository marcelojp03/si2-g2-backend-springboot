package com.uagrm.si2g2.calificacion.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO para crear una evaluación a nivel de materia.
 * 
 * Diferencias con EvaluacionRequest anterior:
 * - idMateria en lugar de idAsignacionDocente
 * - Aplica a TODOS los paralelos que enseñan esta materia en el período
 */
@Data
public class EvaluacionMateriaRequest {

    @NotNull
    private UUID idMateria;

    @NotNull
    @Min(1)
    private Integer periodo;

    @NotBlank
    @Size(max = 40)
    private String tipo;

    @NotBlank
    @Size(max = 120)
    private String nombre;

    @NotNull
    @DecimalMin(value = "0.01")
    @DecimalMax(value = "100.00")
    private BigDecimal ponderacion;

    @Size(max = 15)
    private String escala;

    @Size(max = 15)
    private String estado;
}
