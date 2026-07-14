package com.uagrm.si2g2.alertas.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record EvolucionNotaResponse(
        String evaluacion,
        Integer periodo,
        BigDecimal nota,
        Instant fecha
) {}
