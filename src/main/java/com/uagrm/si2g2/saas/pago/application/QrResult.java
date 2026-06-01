package com.uagrm.si2g2.saas.pago.application;

/**
 * Resultado de generar un QR de cobro en Vpay.
 *
 * @param idQr      identificador del QR (referencia externa para consultar el estado)
 * @param qrBase64  imagen del QR en base64 (PNG, sin prefijo data:)
 */
public record QrResult(String idQr, String qrBase64) {
}
