package com.uagrm.si2g2.calificacion.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PeriodoCierreRequest(
        String justificacion,
        boolean cerrar) {
}