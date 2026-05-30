package com.uagrm.si2g2.saas.plan.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record PlanSuscripcionRequest(

        @NotBlank(message = "El código es obligatorio")
        @Size(max = 30, message = "El código no puede superar 30 caracteres")
        String codigo,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        String descripcion,

        @NotNull(message = "El límite de usuarios es obligatorio")
        @Min(value = 1, message = "El límite de usuarios debe ser mayor a 0")
        Integer maxUsuarios,

        @NotNull(message = "El límite de almacenamiento es obligatorio")
        @Min(value = 1, message = "El almacenamiento debe ser mayor a 0")
        Integer maxAlmacenamientoMb,

        @NotNull(message = "El precio mensual es obligatorio")
        @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
        BigDecimal precioMensual,

        Set<UUID> idModulos
) {}
