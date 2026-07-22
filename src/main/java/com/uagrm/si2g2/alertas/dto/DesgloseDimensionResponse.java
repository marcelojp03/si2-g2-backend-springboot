package com.uagrm.si2g2.alertas.dto;

import java.math.BigDecimal;

public record DesgloseDimensionResponse(
        String dimension,
        BigDecimal puntaje,
        BigDecimal peso
) {}
