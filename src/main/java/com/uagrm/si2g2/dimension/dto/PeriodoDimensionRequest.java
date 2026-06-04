package com.uagrm.si2g2.dimension.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PeriodoDimensionRequest(
        @NotNull java.util.UUID idDimension,
        @NotNull @Min(0) @Max(100) Integer ponderacion
) {}
