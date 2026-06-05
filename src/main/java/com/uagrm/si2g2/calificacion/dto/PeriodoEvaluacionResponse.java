package com.uagrm.si2g2.calificacion.dto;

import com.uagrm.si2g2.calificacion.domain.PeriodoEvaluacion;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PeriodoEvaluacionResponse(
        UUID id,
        UUID idInstitucion,
        UUID idGestionAcademica,
        Integer numeroPeriodo,
        String tipoPeriodo,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String estado,
        Integer pesoSer,
        Integer pesoSaber,
        Integer pesoHacer,
        Integer pesoAuto,
        Instant fechaCierre,
        String justificacionCierre,
        Instant fechaReapertura,
        String justificacionReapertura,
        Instant creadoEn,
        Instant actualizadoEn) {

    public static PeriodoEvaluacionResponse from(PeriodoEvaluacion periodo) {
        return new PeriodoEvaluacionResponse(
                periodo.getId(),
                periodo.getIdInstitucion(),
                periodo.getIdGestionAcademica(),
                periodo.getNumeroPeriodo(),
                periodo.getTipoPeriodo(),
                periodo.getFechaInicio(),
                periodo.getFechaFin(),
                periodo.getEstado(),
                periodo.getPesoSer(),
                periodo.getPesoSaber(),
                periodo.getPesoHacer(),
                periodo.getPesoAuto(),
                periodo.getFechaCierre(),
                periodo.getJustificacionCierre(),
                periodo.getFechaReapertura(),
                periodo.getJustificacionReapertura(),
                periodo.getCreadoEn(),
                periodo.getActualizadoEn());
    }
}