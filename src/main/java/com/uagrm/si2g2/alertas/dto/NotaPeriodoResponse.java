package com.uagrm.si2g2.alertas.dto;

import java.math.BigDecimal;

public record NotaPeriodoResponse(
        int periodo,
        BigDecimal nota
) {}
