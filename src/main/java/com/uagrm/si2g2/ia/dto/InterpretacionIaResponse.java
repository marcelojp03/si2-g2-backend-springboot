package com.uagrm.si2g2.ia.dto;

import java.util.List;

public record InterpretacionIaResponse(
        List<FiltroReporteDto> filtros,
        List<String> columnas_sugeridas,
        double confianza,
        String texto_original
) {}
