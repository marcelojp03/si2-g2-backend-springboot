package com.uagrm.si2g2.dimension.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DimensionRequest(
        @NotBlank @Size(max = 50) String nombre,
        @Size(max = 500) String descripcion,
        @Min(0) @Max(100) Integer pesoDefault
) {}
