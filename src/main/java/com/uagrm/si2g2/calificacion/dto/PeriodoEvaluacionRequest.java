package com.uagrm.si2g2.calificacion.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PeriodoEvaluacionRequest(
        @NotNull Integer numeroPeriodo,
        @NotNull String tipoPeriodo,
        @NotNull LocalDate fechaInicio,
        @NotNull LocalDate fechaFin,
        Integer pesoSer,
        Integer pesoSaber,
        Integer pesoHacer,
        Integer pesoAuto) {

    public PeriodoEvaluacionRequest {
        pesoSer = pesoSer != null ? pesoSer : 10;
        pesoSaber = pesoSaber != null ? pesoSaber : 45;
        pesoHacer = pesoHacer != null ? pesoHacer : 40;
        pesoAuto = pesoAuto != null ? pesoAuto : 5;
    }
}