package com.uagrm.si2g2.seed.dto;

import java.util.Map;

public record AcademicRiskSeedResult(
        String institutionCode,
        int periodos,
        int evaluaciones,
        int calificaciones,
        int registrosAsistencia,
        int detallesAsistencia,
        Map<String, Integer> perfilesEsperados
) {
}
