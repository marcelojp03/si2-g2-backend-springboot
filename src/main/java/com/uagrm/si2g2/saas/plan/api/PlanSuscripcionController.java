package com.uagrm.si2g2.saas.plan.api;

import com.uagrm.si2g2.common.dto.ApiResponse;
import com.uagrm.si2g2.saas.plan.application.PlanSuscripcionService;
import com.uagrm.si2g2.saas.plan.dto.PlanSuscripcionRequest;
import com.uagrm.si2g2.saas.plan.dto.PlanSuscripcionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/saas/planes")
@RequiredArgsConstructor
public class PlanSuscripcionController {

    private final PlanSuscripcionService service;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PlanSuscripcionResponse>> crear(
            @Valid @RequestBody PlanSuscripcionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Plan creado", service.crear(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PlanSuscripcionResponse>>> listar(
            @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(ApiResponse.ok("Planes", service.listar(estado)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_INSTITUCION','DIRECTOR')")
    public ResponseEntity<ApiResponse<PlanSuscripcionResponse>> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Plan", service.obtener(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PlanSuscripcionResponse>> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody PlanSuscripcionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Plan actualizado", service.actualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> desactivar(@PathVariable UUID id) {
        service.desactivar(id);
        return ResponseEntity.ok(ApiResponse.ok("Plan desactivado", null));
    }
}
