package com.uagrm.si2g2.saas.plan.dto;

import com.uagrm.si2g2.saas.plan.domain.ModuloSistema;
import com.uagrm.si2g2.saas.plan.domain.PlanSuscripcion;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Builder
public record PlanSuscripcionResponse(
        UUID id,
        String codigo,
        String nombre,
        String descripcion,
        Integer maxUsuarios,
        Integer maxAlmacenamientoMb,
        BigDecimal precioMensual,
        String estado,
        List<ModuloResponse> modulos
) {
    public static PlanSuscripcionResponse from(PlanSuscripcion plan) {
        return PlanSuscripcionResponse.builder()
                .id(plan.getId())
                .codigo(plan.getCodigo())
                .nombre(plan.getNombre())
                .descripcion(plan.getDescripcion())
                .maxUsuarios(plan.getMaxUsuarios())
                .maxAlmacenamientoMb(plan.getMaxAlmacenamientoMb())
                .precioMensual(plan.getPrecioMensual())
                .estado(plan.getEstado())
                .modulos(plan.getModulos().stream()
                        .map(ModuloResponse::from)
                        .sorted(java.util.Comparator.comparingInt(ModuloResponse::ordenVisual))
                        .toList())
                .build();
    }

    @Builder
    public record ModuloResponse(UUID id, String codigo, String nombre, String icono,
                                 String rutaFrontend, Integer ordenVisual) {
        public static ModuloResponse from(ModuloSistema m) {
            return ModuloResponse.builder()
                    .id(m.getId()).codigo(m.getCodigo()).nombre(m.getNombre())
                    .icono(m.getIcono()).rutaFrontend(m.getRutaFrontend())
                    .ordenVisual(m.getOrdenVisual())
                    .build();
        }
    }
}
