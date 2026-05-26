package com.uagrm.si2g2.reporte.api;

import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.common.dto.ApiResponse;
import com.uagrm.si2g2.reporte.application.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService service;

    @GetMapping("/asistencia")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_INSTITUCION', 'DIRECTOR', 'SECRETARIO')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> reporteAsistencia(
            @RequestParam UUID idGestion,
            @RequestParam(required = false) UUID idParalelo) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        return ResponseEntity.ok(ApiResponse.ok("Reporte de asistencia",
                service.reporteAsistencia(idInstitucion, idGestion, idParalelo)));
    }

    @GetMapping("/calificaciones")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_INSTITUCION', 'DIRECTOR', 'SECRETARIO')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> reporteCalificaciones(
            @RequestParam UUID idGestion,
            @RequestParam(required = false) UUID idParalelo) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        return ResponseEntity.ok(ApiResponse.ok("Reporte de calificaciones",
                service.reporteCalificaciones(idInstitucion, idGestion, idParalelo)));
    }

    @GetMapping("/inscripciones")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_INSTITUCION', 'DIRECTOR', 'SECRETARIO')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> reporteInscripciones(
            @RequestParam UUID idGestion,
            @RequestParam(required = false) UUID idCurso) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        return ResponseEntity.ok(ApiResponse.ok("Reporte de inscripciones",
                service.reporteInscripciones(idInstitucion, idGestion, idCurso)));
    }

    @GetMapping("/gerencial")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_INSTITUCION', 'DIRECTOR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reporteGerencial(
            @RequestParam UUID idGestion) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        return ResponseEntity.ok(ApiResponse.ok("Reporte gerencial",
                service.reporteGerencial(idInstitucion, idGestion)));
    }
}
