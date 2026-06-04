package com.uagrm.si2g2.dimension.dto;

import com.uagrm.si2g2.dimension.domain.PeriodoDimension;

import java.util.UUID;

public record PeriodoDimensionResponse(
        UUID id,
        UUID idPeriodoEvaluacion,
        UUID idDimension,
        String nombreDimension,
        Integer ponderacion
) {
    public static PeriodoDimensionResponse from(PeriodoDimension pd) {
        return new PeriodoDimensionResponse(
                pd.getId(), pd.getIdPeriodoEvaluacion(),
                pd.getDimension().getId(), pd.getDimension().getNombre(),
                pd.getPonderacion()
        );
    }
}
