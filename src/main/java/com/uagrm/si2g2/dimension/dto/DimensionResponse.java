package com.uagrm.si2g2.dimension.dto;

import com.uagrm.si2g2.dimension.domain.Dimension;

import java.time.Instant;
import java.util.UUID;

public record DimensionResponse(
        UUID id,
        UUID idInstitucion,
        String nombre,
        String descripcion,
        Integer pesoDefault,
        String estado,
        Boolean esGlobal,
        Instant creadoEn,
        Instant actualizadoEn
) {
    public static DimensionResponse from(Dimension d) {
        return new DimensionResponse(
                d.getId(), d.getIdInstitucion(),
                d.getNombre(), d.getDescripcion(),
                d.getPesoDefault(), d.getEstado(), d.getEsGlobal(),
                d.getCreadoEn(), d.getActualizadoEn()
        );
    }
}
