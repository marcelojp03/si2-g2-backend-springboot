package com.uagrm.si2g2.alertas.dto;

import com.uagrm.si2g2.alertas.domain.RecomendacionIa;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class RecomendacionIaResponse {

    private UUID id;
    private UUID idAlertaRiesgo;
    private String descripcion;
    private String tipoAccion;
    private Instant creadoEn;

    public static RecomendacionIaResponse from(RecomendacionIa r) {
        return RecomendacionIaResponse.builder()
                .id(r.getId())
                .idAlertaRiesgo(r.getIdAlertaRiesgo())
                .descripcion(r.getDescripcion())
                .tipoAccion(r.getTipoAccion())
                .creadoEn(r.getCreadoEn())
                .build();
    }
}
