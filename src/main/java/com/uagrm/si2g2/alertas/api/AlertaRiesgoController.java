package com.uagrm.si2g2.alertas.api;

import com.uagrm.si2g2.alertas.application.AlertaRiesgoService;
import com.uagrm.si2g2.alertas.application.RecomendacionIaService;
import com.uagrm.si2g2.alertas.dto.AlertaRiesgoResponse;
import com.uagrm.si2g2.alertas.dto.RecomendacionIaResponse;
import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/alertas-riesgo")
@RequiredArgsConstructor
public class AlertaRiesgoController {

    private final AlertaRiesgoService alertaService;
    private final RecomendacionIaService recomendacionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','DIRECTOR','SECRETARIO')")
    public ResponseEntity<ApiResponse<List<AlertaRiesgoResponse>>> listar(
            @RequestParam(required = false) UUID idGestion,
            @RequestParam(required = false) String nivel) {
        return ResponseEntity.ok(ApiResponse.ok("Alertas de riesgo",
                alertaService.listar(SecurityUtils.requireCurrentInstitutionId(), idGestion, nivel)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','DIRECTOR','SECRETARIO')")
    public ResponseEntity<ApiResponse<AlertaRiesgoResponse>> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Alerta",
                alertaService.obtener(id, SecurityUtils.requireCurrentInstitutionId())));
    }

    @GetMapping("/{id}/recomendaciones")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','DIRECTOR','SECRETARIO')")
    public ResponseEntity<ApiResponse<List<RecomendacionIaResponse>>> recomendaciones(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Recomendaciones", alertaService.recomendaciones(id)));
    }
}
