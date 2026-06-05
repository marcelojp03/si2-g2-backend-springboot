package com.uagrm.si2g2.academico.api;

import com.uagrm.si2g2.academico.application.GestionAcademicaService;
import com.uagrm.si2g2.calificacion.application.PeriodoEvaluacionService;
import com.uagrm.si2g2.academico.dto.GestionAcademicaRequest;
import com.uagrm.si2g2.academico.dto.GestionAcademicaResponse;
import com.uagrm.si2g2.calificacion.dto.PeriodoEvaluacionRequest;
import com.uagrm.si2g2.calificacion.dto.PeriodoEvaluacionResponse;
import com.uagrm.si2g2.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/gestiones")
@RequiredArgsConstructor
public class GestionAcademicaController {

    private final GestionAcademicaService service;
    private final PeriodoEvaluacionService periodoService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR') or hasAuthority('GESTIONES_CREATE')")
    public ResponseEntity<ApiResponse<GestionAcademicaResponse>> crear(
            @Valid @RequestBody GestionAcademicaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Gestión académica creada", service.crear(request)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO') or hasAuthority('GESTIONES_READ')")
    public ResponseEntity<ApiResponse<List<GestionAcademicaResponse>>> listar() {
        return ResponseEntity.ok(ApiResponse.ok("Gestiones académicas", service.listar()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO') or hasAuthority('GESTIONES_READ')")
    public ResponseEntity<ApiResponse<GestionAcademicaResponse>> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Gestión académica", service.obtener(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR') or hasAuthority('GESTIONES_UPDATE')")
    public ResponseEntity<ApiResponse<GestionAcademicaResponse>> actualizar(
            @PathVariable UUID id,
            @Valid @RequestBody GestionAcademicaRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Gestión académica actualizada", service.actualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR') or hasAuthority('GESTIONES_DELETE')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable UUID id) {
        service.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Gestión académica anulada", null));
    }

    @GetMapping("/{id}/periodos")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','SECRETARIO') or hasAuthority('GESTIONES_READ')")
    public ResponseEntity<ApiResponse<List<PeriodoEvaluacionResponse>>> listarPeriodos(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Periodos de la gestión", periodoService.listarPorGestion(id)));
    }

    @PostMapping("/{id}/periodos")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR') or hasAuthority('GESTIONES_CREATE')")
    public ResponseEntity<ApiResponse<List<PeriodoEvaluacionResponse>>> crearPeriodos(
            @PathVariable UUID id,
            @Valid @RequestBody List<PeriodoEvaluacionRequest> periodos) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Periodos creados", periodoService.crearPeriodosPorGestion(id, periodos)));
    }

    @PutMapping("/{id}/periodos/{idPeriodo}")
    @PreAuthorize("hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR') or hasAuthority('GESTIONES_UPDATE')")
    public ResponseEntity<ApiResponse<PeriodoEvaluacionResponse>> actualizarFechasPeriodo(
            @PathVariable UUID id,
            @PathVariable UUID idPeriodo,
            @RequestParam java.time.LocalDate fechaInicio,
            @RequestParam java.time.LocalDate fechaFin) {
        return ResponseEntity.ok(ApiResponse.ok("Fechas actualizadas",
                periodoService.actualizarFechas(idPeriodo, fechaInicio, fechaFin)));
    }
}
