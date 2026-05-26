package com.uagrm.si2g2.saas.solicitud.api;

import com.uagrm.si2g2.common.dto.ApiResponse;
import com.uagrm.si2g2.saas.solicitud.application.SolicitudOnboardingService;
import com.uagrm.si2g2.saas.solicitud.dto.SolicitudAdminRequest;
import com.uagrm.si2g2.saas.solicitud.dto.SolicitudOnboardingRequest;
import com.uagrm.si2g2.saas.solicitud.dto.SolicitudOnboardingResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class SolicitudOnboardingController {

    private final SolicitudOnboardingService service;

    // ──────────────────────────────────────────────────────────────────────────────
    // ENDPOINT PÚBLICO (landing page)
    // ──────────────────────────────────────────────────────────────────────────────

    @PostMapping("/api/public/solicitudes")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SolicitudOnboardingResponse> enviar(
            @Valid @RequestBody SolicitudOnboardingRequest request) {
        return ApiResponse.created("Solicitud enviada correctamente. Nos pondremos en contacto pronto.", service.enviar(request));
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // ENDPOINTS SUPER_ADMIN
    // ──────────────────────────────────────────────────────────────────────────────

    @GetMapping("/api/saas/solicitudes")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<List<SolicitudOnboardingResponse>> listar(
            @RequestParam(required = false) String estado) {
        return ApiResponse.ok("OK", service.listar(estado));
    }

    @GetMapping("/api/saas/solicitudes/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<SolicitudOnboardingResponse> obtener(@PathVariable UUID id) {
        return ApiResponse.ok("OK", service.obtener(id));
    }

    @PutMapping("/api/saas/solicitudes/{id}/aprobar")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<SolicitudOnboardingResponse> aprobar(
            @PathVariable UUID id,
            @Valid @RequestBody SolicitudAdminRequest request) {
        return ApiResponse.ok("Solicitud aprobada", service.aprobar(id, request));
    }

    @PutMapping("/api/saas/solicitudes/{id}/rechazar")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<SolicitudOnboardingResponse> rechazar(
            @PathVariable UUID id,
            @Valid @RequestBody SolicitudAdminRequest request) {
        return ApiResponse.ok("Solicitud rechazada", service.rechazar(id, request));
    }

    @PutMapping("/api/saas/solicitudes/{id}/pago")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<SolicitudOnboardingResponse> confirmarPago(@PathVariable UUID id) {
        return ApiResponse.ok("Pago confirmado", service.confirmarPago(id));
    }

    @PostMapping("/api/saas/solicitudes/{id}/activar")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<SolicitudOnboardingResponse> activar(@PathVariable UUID id) {
        return ApiResponse.ok("Institución activada correctamente", service.activar(id));
    }
}
