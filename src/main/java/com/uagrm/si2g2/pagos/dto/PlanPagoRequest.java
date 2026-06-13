package com.uagrm.si2g2.pagos.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PlanPagoRequest {

    @NotBlank
    @Size(max = 120)
    private String nombre;

    @NotBlank
    @Size(max = 20)
    private String tipoPeriodo;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal monto;

    @Size(max = 3)
    private String moneda;

    @NotNull
    @Min(1)
    private Integer cantidadCuotas;

    @Min(1)
    @Max(28)
    private Integer diaVencimiento;

    private String descripcion;
}
