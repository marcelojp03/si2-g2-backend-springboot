package com.uagrm.si2g2.alertas.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AnalizarRiesgoRequest(
        @NotNull UUID idParalelo,
        @NotNull UUID idGestion,
        UUID idPeriodo,
        UUID idMateria
) {}
