package com.uagrm.si2g2.saas.suscripcion.api;

import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.common.dto.ApiResponse;
import com.uagrm.si2g2.saas.suscripcion.application.SuscripcionInstitucionService;
import com.uagrm.si2g2.saas.suscripcion.dto.SuscripcionInstitucionRequest;
import com.uagrm.si2g2.saas.suscripcion.dto.SuscripcionInstitucionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/saas/suscripciones")
@RequiredArgsConstructor
public class SuscripcionInstitucionController {

    private final SuscripcionInstitucionService service;

    /**
     * La institución del JWT se usa automáticamente. ADMIN_INSTITUCION puede suscribirse.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<SuscripcionInstitucionResponse>> suscribir(
            @Valid @RequestBody SuscripcionInstitucionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Suscripción creada", service.suscribir(request)));
    }

    /**
     * Consulta la suscripción activa. ADMIN_INSTITUCION solo ve la suya.
     * SUPER_ADMIN puede especificar el id_institucion como param.
     */
    @GetMapping("/activa")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR')")
    public ResponseEntity<ApiResponse<SuscripcionInstitucionResponse>> obtenerActiva(
            @RequestParam(required = false) UUID idInstitucion) {
        UUID target = idInstitucion != null && SecurityUtils.currentUserHasRole("SUPER_ADMIN")
                ? idInstitucion
                : SecurityUtils.requireCurrentInstitutionId();
        return ResponseEntity.ok(ApiResponse.ok("Suscripción activa", service.obtenerActiva(target)));
    }

    /**
     * Cancelar la suscripción activa de la institución.
     */
    @DeleteMapping("/activa")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> cancelar(
            @RequestParam(required = false) UUID idInstitucion) {
        UUID target = idInstitucion != null && SecurityUtils.currentUserHasRole("SUPER_ADMIN")
                ? idInstitucion
                : SecurityUtils.requireCurrentInstitutionId();
        service.cancelar(target);
        return ResponseEntity.ok(ApiResponse.ok("Suscripción cancelada", null));
    }
}
