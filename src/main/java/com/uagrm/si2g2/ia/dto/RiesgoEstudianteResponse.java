package com.uagrm.si2g2.ia.dto;

import java.util.List;

public record RiesgoEstudianteResponse(
        String id_estudiante,
        String nivel_riesgo,          // BAJO | MEDIO | ALTO | CRITICO
        double probabilidad_riesgo,
        List<String> factores_principales
) {}
