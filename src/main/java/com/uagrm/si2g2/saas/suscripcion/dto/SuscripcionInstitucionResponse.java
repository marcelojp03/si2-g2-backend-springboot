package com.uagrm.si2g2.saas.suscripcion.dto;

import com.uagrm.si2g2.saas.plan.dto.PlanSuscripcionResponse;
import com.uagrm.si2g2.saas.suscripcion.domain.SuscripcionInstitucion;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

@Builder
public record SuscripcionInstitucionResponse(
        UUID id,
        UUID idInstitucion,
        PlanSuscripcionResponse plan,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String estado,
        Boolean simulada,
        String observacion
) {
    public static SuscripcionInstitucionResponse from(SuscripcionInstitucion s) {
        return SuscripcionInstitucionResponse.builder()
                .id(s.getId())
                .idInstitucion(s.getIdInstitucion())
                .plan(PlanSuscripcionResponse.from(s.getPlan()))
                .fechaInicio(s.getFechaInicio())
                .fechaFin(s.getFechaFin())
                .estado(s.getEstado())
                .simulada(s.getSimulada())
                .observacion(s.getObservacion())
                .build();
    }
}
