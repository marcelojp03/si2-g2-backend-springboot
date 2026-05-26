package com.uagrm.si2g2.ia.dto;

import java.util.List;

public record FiltroReporteDto(
        String campo,
        String operador,   // EQ | CONTAINS | GT | LT | BETWEEN | IN
        Object valor
) {}

// record anidado para la respuesta de interpretación
