package com.uagrm.si2g2.dimension.api;

import com.uagrm.si2g2.common.dto.ApiResponse;
import com.uagrm.si2g2.dimension.application.SolicitudEliminacionDimensionService;
import com.uagrm.si2g2.dimension.dto.SolicitudEliminacionRequest;
import com.uagrm.si2g2.dimension.dto.SolicitudEliminacionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/solicitudes-dimension")
@RequiredArgsConstructor
public class SolicitudEliminacionDimensionController {

    private final SolicitudEliminacionDimensionService service;

    @PostMapping("/periodos/{idPeriodo}")
    @PreAuthorize("hasAuthority('GESTIONES_CREATE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR')")
    public ApiResponse<SolicitudEliminacionResponse> crear(@PathVariable UUID idPeriodo,
                                                            @Valid @RequestBody SolicitudEliminacionRequest request) {
        return ApiResponse.ok("Solicitud creada", service.crearSolicitud(idPeriodo, request));
    }

    @GetMapping("/pendientes")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR')")
    public ApiResponse<List<SolicitudEliminacionResponse>> listarPendientes() {
        return ApiResponse.ok("Solicitudes pendientes", service.listarPendientes());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR')")
    public ApiResponse<List<SolicitudEliminacionResponse>> listarPorEstado(@RequestParam(defaultValue = "PENDIENTE") String estado) {
        return ApiResponse.ok("Solicitudes", service.listarPorEstado(estado));
    }

    @PostMapping("/{id}/aprobar")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR')")
    public ApiResponse<SolicitudEliminacionResponse> aprobar(@PathVariable UUID id,
                                                              @RequestBody(required = false) String observacion) {
        return ApiResponse.ok("Solicitud aprobada", service.aprobar(id, observacion));
    }

    @PostMapping("/{id}/rechazar")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR')")
    public ApiResponse<SolicitudEliminacionResponse> rechazar(@PathVariable UUID id,
                                                               @RequestBody String observacion) {
        return ApiResponse.ok("Solicitud rechazada", service.rechazar(id, observacion));
    }
}
