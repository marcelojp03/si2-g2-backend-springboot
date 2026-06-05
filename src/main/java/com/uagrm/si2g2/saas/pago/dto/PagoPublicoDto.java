package com.uagrm.si2g2.saas.pago.dto;

import com.uagrm.si2g2.saas.pago.domain.PagoSuscripcion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO público expuesto en endpoints sin autenticación (/api/public/pago/**).
 * No incluye IDs internos, idSolicitud ni datos sensibles.
 */
public record PagoPublicoDto(
        UUID tokenPago,
        BigDecimal monto,
        String moneda,
        String glosa,
        String estado,
        LocalDate fechaExpiracion,
        String proveedor,
        /** idQr/referencia externa del proveedor (Vpay) */
        String idQr,
        /** null hasta que el QR sea generado lazily */
        String qrBase64
) {
    public static PagoPublicoDto from(PagoSuscripcion p) {
        return new PagoPublicoDto(
                p.getTokenPago(),
                p.getMonto(),
                p.getMoneda(),
                p.getGlosa(),
                p.getEstado(),
                p.getFechaExpiracion(),
                p.getProveedor(),
                p.getReferenciaExterna(),
                p.getQrBase64()
        );
    }
}
