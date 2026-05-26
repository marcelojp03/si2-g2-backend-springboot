package com.uagrm.si2g2.saas.suscripcion.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record SuscripcionInstitucionRequest(

        @NotNull(message = "El plan es obligatorio")
        UUID idPlan,

        LocalDate fechaInicio,
        LocalDate fechaFin,
        String observacion
) {}
