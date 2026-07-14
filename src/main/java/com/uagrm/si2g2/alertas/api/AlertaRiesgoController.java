package com.uagrm.si2g2.alertas.api;

import com.uagrm.si2g2.alertas.application.AlertaRiesgoService;
import com.uagrm.si2g2.alertas.application.RecomendacionIaService;
import com.uagrm.si2g2.alertas.dto.AlertaRiesgoResponse;
import com.uagrm.si2g2.alertas.dto.RecomendacionIaResponse;
import com.uagrm.si2g2.alertas.dto.ActualizarEstadoAlertaRequest;
import com.uagrm.si2g2.alertas.dto.AnalisisRiesgoResponse;
import com.uagrm.si2g2.alertas.dto.AnalizarRiesgoRequest;
import com.uagrm.si2g2.alertas.dto.RiesgoEstudianteDetalleResponse;
import com.uagrm.si2g2.alertas.application.RiesgoAcademicoService;
import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/alertas-riesgo")
@RequiredArgsConstructor
public class AlertaRiesgoController {

    private final AlertaRiesgoService alertaService;
    private final RecomendacionIaService recomendacionService;
    private final RiesgoAcademicoService riesgoAcademicoService;

    @PostMapping("/analizar")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','DIRECTOR','SECRETARIO','DOCENTE')")
    public ResponseEntity<ApiResponse<AnalisisRiesgoResponse>> analizar(@Valid @RequestBody AnalizarRiesgoRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Análisis de riesgo completado",
                riesgoAcademicoService.analizarParalelo(request.idParalelo(), request.idGestion())));
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','DIRECTOR','SECRETARIO')")
    public ResponseEntity<ApiResponse<AnalisisRiesgoResponse>> resumen(@RequestParam UUID idGestion) {
        return ResponseEntity.ok(ApiResponse.ok("Resumen institucional de riesgo",
                riesgoAcademicoService.analizarInstitucion(idGestion)));
    }

    @PostMapping("/analizar/institucion")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','DIRECTOR','SECRETARIO')")
    public ResponseEntity<ApiResponse<AnalisisRiesgoResponse>> analizarInstitucion(@RequestBody Map<String, UUID> request) {
        UUID idGestion = request.get("idGestion");
        if (idGestion == null) {
            throw new IllegalArgumentException("La gestión académica es obligatoria");
        }
        return ResponseEntity.ok(ApiResponse.ok("Análisis institucional completado",
                riesgoAcademicoService.analizarInstitucion(idGestion)));
    }

    @GetMapping("/estudiante/{idEstudiante}/detalle")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','DIRECTOR','SECRETARIO','DOCENTE')")
    public ResponseEntity<ApiResponse<RiesgoEstudianteDetalleResponse>> detalleEstudiante(
            @PathVariable UUID idEstudiante, @RequestParam UUID idGestion) {
        return ResponseEntity.ok(ApiResponse.ok("Detalle de riesgo del estudiante",
                riesgoAcademicoService.detalleEstudiante(idEstudiante, idGestion)));
    }

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

    @PutMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','DIRECTOR','SECRETARIO','DOCENTE')")
    public ResponseEntity<ApiResponse<AlertaRiesgoResponse>> actualizarEstado(
            @PathVariable UUID id, @Valid @RequestBody ActualizarEstadoAlertaRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Estado de alerta actualizado",
                alertaService.actualizarEstado(id, request.estado())));
    }
}
