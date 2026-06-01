package com.uagrm.si2g2.ia.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConsultaNaturalIaRequest(
        @NotBlank @Size(max = 500) String consulta,
        @Min(1) @Max(200) int limite
) {
    public ConsultaNaturalIaRequest {
        if (limite <= 0) limite = 100;
    }
}
