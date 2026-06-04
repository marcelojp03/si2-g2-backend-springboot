package com.uagrm.si2g2.dimension.dto;

import com.uagrm.si2g2.dimension.domain.SolicitudEliminacionDimension;

import java.time.Instant;
import java.util.UUID;

public record SolicitudEliminacionResponse(
        UUID id,
        UUID idInstitucion,
        UUID idPeriodoEvaluacion,
        UUID idDimension,
        String estado,
        UUID idUsuarioSolicitud,
        Instant fechaSolicitud,
        UUID idUsuarioResolucion,
        Instant fechaResolucion,
        String observacion
) {
    public static SolicitudEliminacionResponse from(SolicitudEliminacionDimension s) {
        return new SolicitudEliminacionResponse(
                s.getId(), s.getIdInstitucion(),
                s.getIdPeriodoEvaluacion(), s.getIdDimension(),
                s.getEstado(), s.getIdUsuarioSolicitud(),
                s.getFechaSolicitud(), s.getIdUsuarioResolucion(),
                s.getFechaResolucion(), s.getObservacion()
        );
    }
}
