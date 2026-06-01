package com.uagrm.si2g2.saas.pago.api;

import com.uagrm.si2g2.common.dto.ApiResponse;
import com.uagrm.si2g2.saas.pago.application.PagoSuscripcionService;
import com.uagrm.si2g2.saas.pago.dto.EstadoPagoResponse;
import com.uagrm.si2g2.saas.pago.dto.PagoSuscripcionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Endpoints SUPER_ADMIN para el pago de onboarding vía Vpay.
 */
@RestController
@RequiredArgsConstructor
public class PagoSuscripcionController {

    private final PagoSuscripcionService service;

    /** Genera el QR de pago para una solicitud aprobada y notifica al contacto. */
    @PostMapping("/api/saas/solicitudes/{idSolicitud}/generar-qr")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<PagoSuscripcionResponse> generarQr(@PathVariable UUID idSolicitud) {
        return ApiResponse.created("QR de pago generado", service.generarQrParaSolicitud(idSolicitud));
    }

    /** Consulta el estado del pago contra Vpay (usado por el panel para hacer polling). */
    @GetMapping("/api/saas/pagos/{idPago}/estado")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<EstadoPagoResponse> estado(@PathVariable UUID idPago) {
        return ApiResponse.ok("OK", service.consultarEstado(idPago));
    }

    /** Devuelve el pago vigente de una solicitud (o null si no tiene). */
    @GetMapping("/api/saas/solicitudes/{idSolicitud}/pago")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<PagoSuscripcionResponse> obtenerPorSolicitud(@PathVariable UUID idSolicitud) {
        return ApiResponse.ok("OK", service.obtenerPorSolicitud(idSolicitud));
    }
}
