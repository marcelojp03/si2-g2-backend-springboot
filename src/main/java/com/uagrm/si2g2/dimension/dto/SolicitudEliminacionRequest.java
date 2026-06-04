package com.uagrm.si2g2.dimension.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SolicitudEliminacionRequest(
        @NotNull UUID idDimension,
        @Size(max = 500) String observacion
) {}
