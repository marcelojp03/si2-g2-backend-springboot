package com.uagrm.si2g2.pagos.dto;

import com.uagrm.si2g2.pagos.domain.PlanPago;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class PlanPagoResponse {

    private UUID id;
    private UUID idInstitucion;
    private String nombre;
    private String tipoPeriodo;
    private BigDecimal monto;
    private String moneda;
    private Integer cantidadCuotas;
    private Integer diaVencimiento;
    private String descripcion;
    private Boolean activo;
    private Instant creadoEn;
    private Instant actualizadoEn;

    public static PlanPagoResponse from(PlanPago p) {
        return PlanPagoResponse.builder()
                .id(p.getId())
                .idInstitucion(p.getIdInstitucion())
                .nombre(p.getNombre())
                .tipoPeriodo(p.getTipoPeriodo())
                .monto(p.getMonto())
                .moneda(p.getMoneda())
                .cantidadCuotas(p.getCantidadCuotas())
                .diaVencimiento(p.getDiaVencimiento())
                .descripcion(p.getDescripcion())
                .activo(p.getActivo())
                .creadoEn(p.getCreadoEn())
                .actualizadoEn(p.getActualizadoEn())
                .build();
    }
}
