package com.uagrm.si2g2.saas.pago.dto;

import java.util.UUID;

/**
 * Resultado de consultar el estado de un pago contra Vpay.
 *
 * @param idPago      id interno del pago
 * @param idQr        referencia externa (id del QR en Vpay)
 * @param estadoVpay  código devuelto por Vpay: "PEN" (pendiente) o "PAG" (pagado)
 * @param pagado      true si el pago quedó confirmado
 * @param estadoPago  estado interno del pago tras la consulta (PENDIENTE/PAGADO/...)
 */
public record EstadoPagoResponse(
        UUID idPago,
        String idQr,
        String estadoVpay,
        boolean pagado,
        String estadoPago
) {
}
