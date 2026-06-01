package com.uagrm.si2g2.saas.pago.api;

import com.uagrm.si2g2.common.dto.ApiResponse;
import com.uagrm.si2g2.saas.pago.application.PagoSuscripcionService;
import com.uagrm.si2g2.saas.pago.dto.EstadoPagoResponse;
import com.uagrm.si2g2.saas.pago.dto.PagoPublicoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Endpoints públicos (sin autenticación) para el flujo de pago del onboarding.
 *
 * <p>Accedidos por el cliente desde la página /pagar/{tokenPago} que recibe vía correo.
 * No exponen datos sensibles ni IDs internos.</p>
 */
@RestController
@RequestMapping("/api/public/pago")
@RequiredArgsConstructor
public class PagoPublicoController {

    private final PagoSuscripcionService pagoService;

    /**
     * Retorna los datos del pago e incluye el QR (generado lazily si es la primera visita).
     * GET /api/public/pago/{tokenPago}
     */
    @GetMapping("/{tokenPago}")
    public ResponseEntity<ApiResponse<PagoPublicoDto>> obtenerPago(
            @PathVariable UUID tokenPago) {
        PagoPublicoDto dto = pagoService.obtenerYGenerarQrPublico(tokenPago);
        return ResponseEntity.ok(ApiResponse.ok("OK", dto));
    }

    /**
     * Consulta el estado del pago contra Vpay. Si pagado, activa la institución.
     * Usado por el frontend para polling cada 5 segundos.
     * GET /api/public/pago/{tokenPago}/estado
     */
    @GetMapping("/{tokenPago}/estado")
    public ResponseEntity<ApiResponse<EstadoPagoResponse>> consultarEstado(
            @PathVariable UUID tokenPago) {
        EstadoPagoResponse estado = pagoService.consultarEstadoPublico(tokenPago);
        return ResponseEntity.ok(ApiResponse.ok("OK", estado));
    }
}
