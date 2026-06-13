package com.uagrm.si2g2.pagos.dto;

import com.uagrm.si2g2.pagos.domain.Pago;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class PagoResponse {

    private UUID id;
    private UUID idCuota;
    private UUID idUsuarioPaga;
    private BigDecimal monto;
    private String moneda;
    private String metodoPago;
    private String proveedor;
    private String referenciaExterna;
    private UUID tokenPago;
    private String qrBase64;
    private String estado;
    private Instant pagadoEn;
    private Instant creadoEn;

    public static PagoResponse from(Pago p) {
        return PagoResponse.builder()
                .id(p.getId())
                .idCuota(p.getIdCuota())
                .idUsuarioPaga(p.getIdUsuarioPaga())
                .monto(p.getMonto())
                .moneda(p.getMoneda())
                .metodoPago(p.getMetodoPago())
                .proveedor(p.getProveedor())
                .referenciaExterna(p.getReferenciaExterna())
                .tokenPago(p.getTokenPago())
                .qrBase64(p.getQrBase64())
                .estado(p.getEstado())
                .pagadoEn(p.getPagadoEn())
                .creadoEn(p.getCreadoEn())
                .build();
    }
}
