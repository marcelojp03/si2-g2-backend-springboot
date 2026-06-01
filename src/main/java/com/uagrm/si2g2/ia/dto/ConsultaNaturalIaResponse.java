package com.uagrm.si2g2.ia.dto;

import java.util.List;

public record ConsultaNaturalIaResponse(
        String sqlGenerado,
        List<String> columnas,
        List<List<Object>> filas,
        int total
) {}
