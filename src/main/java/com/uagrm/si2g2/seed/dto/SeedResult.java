package com.uagrm.si2g2.seed.dto;

import java.util.List;
import java.util.Map;

public record SeedResult(
        String institucionCodigo,
        String gestionAcademica,
        Map<String, Integer> creados,
        Map<String, Integer> existentes,
        List<SeedUser> usuarios
) {
}
