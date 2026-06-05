package com.uagrm.si2g2.reporte.api;

import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.common.dto.ApiResponse;
import com.uagrm.si2g2.reporte.application.ReporteNaturalLanguageService;
import com.uagrm.si2g2.reporte.application.ReporteQbeService;
import com.uagrm.si2g2.reporte.application.ReporteService;
import com.uagrm.si2g2.reporte.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
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
    private final ReporteNaturalLanguageService naturalLanguageService;
    private final ReporteQbeService qbeService;

    @GetMapping("/asistencia")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_INSTITUCION', 'DIRECTOR', 'SECRETARIO')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> reporteAsistencia(@RequestParam UUID idGestion, @RequestParam(required = false) UUID idParalelo) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        return ResponseEntity.ok(ApiResponse.ok("Reporte de asistencia", service.reporteAsistencia(idInstitucion, idGestion, idParalelo)));
    }

    @GetMapping("/calificaciones")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_INSTITUCION', 'DIRECTOR', 'SECRETARIO')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> reporteCalificaciones(@RequestParam UUID idGestion, @RequestParam(required = false) UUID idParalelo) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        return ResponseEntity.ok(ApiResponse.ok("Reporte de calificaciones", service.reporteCalificaciones(idInstitucion, idGestion, idParalelo)));
    }

    @GetMapping("/inscripciones")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_INSTITUCION', 'DIRECTOR', 'SECRETARIO')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> reporteInscripciones(@RequestParam UUID idGestion, @RequestParam(required = false) UUID idCurso) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        return ResponseEntity.ok(ApiResponse.ok("Reporte de inscripciones", service.reporteInscripciones(idInstitucion, idGestion, idCurso)));
    }

    @GetMapping("/gerencial")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN_INSTITUCION', 'DIRECTOR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reporteGerencial(@RequestParam UUID idGestion) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        return ResponseEntity.ok(ApiResponse.ok("Reporte gerencial", service.reporteGerencial(idInstitucion, idGestion)));
    }

    @GetMapping("/catalogo")
    @PreAuthorize("hasAnyAuthority('REPORTES_READ','REPORTES_EXPORT','REPORTES_WRITE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO','DOCENTE')")
    public ResponseEntity<ApiResponse<List<ReporteMetadataResponse>>> catalogo() {
        return ResponseEntity.ok(ApiResponse.ok("Catálogo de reportes", service.catalogo()));
    }

    @PostMapping("/preview")
    @PreAuthorize("hasAnyAuthority('REPORTES_READ','REPORTES_EXPORT') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO','DOCENTE')")
    public ResponseEntity<ApiResponse<ReportePreviewResponse>> preview(@Valid @RequestBody ReportePreviewRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Reporte generado", service.preview(request)));
    }

    @PostMapping("/export/{formato}")
    @PreAuthorize("hasAnyAuthority('REPORTES_EXPORT') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO')")
    public ResponseEntity<byte[]> exportar(@PathVariable String formato, @Valid @RequestBody ReportePreviewRequest request) {
        ReporteService.ExportedReport report = service.exportar(request, formato);
        return fileResponse(report);
    }

    @PostMapping("/nl/preview")
    @PreAuthorize("hasAnyAuthority('REPORTES_READ','REPORTES_EXPORT') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO','DOCENTE')")
    public ResponseEntity<ApiResponse<ReportePreviewResponse>> previewNaturalLanguage(
            @Valid @RequestBody ReporteNaturalLanguageRequest request,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(ApiResponse.ok("Reporte NL generado", naturalLanguageService.preview(request, authHeader)));
    }

    @PostMapping("/nl/export/{formato}")
    @PreAuthorize("hasAnyAuthority('REPORTES_EXPORT') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO')")
    public ResponseEntity<byte[]> exportarNaturalLanguage(
            @PathVariable String formato,
            @Valid @RequestBody ReporteNaturalLanguageRequest request,
            @RequestHeader("Authorization") String authHeader) {
        return fileResponse(naturalLanguageService.exportar(request, formato, authHeader));
    }

    @GetMapping("/qbe/catalogo")
    @PreAuthorize("hasAnyAuthority('REPORTES_READ','REPORTES_EXPORT') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO','DOCENTE')")
    public ResponseEntity<ApiResponse<List<QbeEntityDefinitionResponse>>> qbeCatalogo() {
        return ResponseEntity.ok(ApiResponse.ok("Catálogo QBE", qbeService.catalogo()));
    }

    @PostMapping("/qbe/preview")
    @PreAuthorize("hasAnyAuthority('REPORTES_READ','REPORTES_EXPORT') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO','DOCENTE')")
    public ResponseEntity<ApiResponse<ReportePreviewResponse>> previewQbe(@Valid @RequestBody QbePreviewRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Reporte QBE generado", qbeService.preview(request)));
    }

    @PostMapping("/qbe/export/{formato}")
    @PreAuthorize("hasAnyAuthority('REPORTES_EXPORT') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO')")
    public ResponseEntity<byte[]> exportarQbe(@PathVariable String formato, @Valid @RequestBody QbePreviewRequest request) {
        return fileResponse(qbeService.exportar(request, formato));
    }

    private ResponseEntity<byte[]> fileResponse(ReporteService.ExportedReport report) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, report.contentType())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(report.fileName()).build().toString())
                .body(report.bytes());
    }
}
