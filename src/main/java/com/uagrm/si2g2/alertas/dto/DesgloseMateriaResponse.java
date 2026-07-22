package com.uagrm.si2g2.alertas.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record DesgloseMateriaResponse(
        UUID idMateria,
        String nombreMateria,
        BigDecimal notaTotal,
        boolean aprobada,
        List<DesgloseDimensionResponse> dimensiones,
        List<NotaPeriodoResponse> notasPorPeriodo
) {}
