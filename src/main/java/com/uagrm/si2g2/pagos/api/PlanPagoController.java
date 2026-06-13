package com.uagrm.si2g2.pagos.api;

import com.uagrm.si2g2.common.dto.ApiResponse;
import com.uagrm.si2g2.pagos.application.PlanPagoService;
import com.uagrm.si2g2.pagos.dto.PlanPagoRequest;
import com.uagrm.si2g2.pagos.dto.PlanPagoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/planes-pago")
@RequiredArgsConstructor
public class PlanPagoController {

    private final PlanPagoService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PlanPagoResponse>> crear(@Valid @RequestBody PlanPagoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Plan de pago creado", service.crear(request)));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<PlanPagoResponse>>> listar(
            @RequestParam(defaultValue = "true") boolean soloActivos) {
        return ResponseEntity.ok(ApiResponse.ok("Planes de pago", service.listar(soloActivos)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PlanPagoResponse>> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Plan de pago", service.obtener(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PlanPagoResponse>> actualizar(
            @PathVariable UUID id, @Valid @RequestBody PlanPagoRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Plan actualizado", service.actualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> desactivar(@PathVariable UUID id) {
        service.desactivar(id);
        return ResponseEntity.ok(ApiResponse.ok("Plan desactivado", null));
    }
}
