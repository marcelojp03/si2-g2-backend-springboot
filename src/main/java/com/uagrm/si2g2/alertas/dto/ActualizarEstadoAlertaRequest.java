package com.uagrm.si2g2.alertas.dto;

import jakarta.validation.constraints.NotBlank;

public record ActualizarEstadoAlertaRequest(@NotBlank String estado, String observacion) {}
