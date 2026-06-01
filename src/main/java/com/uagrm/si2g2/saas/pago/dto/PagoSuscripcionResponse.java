package com.uagrm.si2g2.saas.pago.dto;

import com.uagrm.si2g2.saas.pago.domain.PagoSuscripcion;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Datos del pago expuestos al panel SUPER_ADMIN (incluye el QR para mostrarlo en pantalla).
 */
public record PagoSuscripcionResponse(
        UUID id,
        UUID idSolicitud,
        UUID idPlan,
        BigDecimal monto,
        String moneda,
        String metodoPago,
        String proveedor,
        String referenciaExterna,
        String qrBase64,
        String estado,
        String glosa,
        LocalDate fechaExpiracion,
        Instant pagadoEn,
        Instant creadoEn
) {
    public static PagoSuscripcionResponse from(PagoSuscripcion p) {
        return new PagoSuscripcionResponse(
                p.getId(),
                p.getIdSolicitud(),
                p.getIdPlan(),
                p.getMonto(),
                p.getMoneda(),
                p.getMetodoPago(),
                p.getProveedor(),
                p.getReferenciaExterna(),
                p.getQrBase64(),
                p.getEstado(),
                p.getGlosa(),
                p.getFechaExpiracion(),
                p.getPagadoEn(),
                p.getCreadoEn()
        );
    }
}
