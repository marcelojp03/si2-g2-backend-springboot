package com.uagrm.si2g2.calificacion.api;

import com.uagrm.si2g2.calificacion.application.CalificacionService;
import com.uagrm.si2g2.calificacion.dto.*;
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
@RequestMapping("/api/calificaciones")
@RequiredArgsConstructor
public class CalificacionController {

    private final CalificacionService service;

    @GetMapping("/mis-asignaciones")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_READ','CALIFICACIONES_READ_ALL','MI_AREA_READ') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<List<CalificacionAsignacionResponse>>> listarMisAsignaciones() {
        return ResponseEntity.ok(ApiResponse.ok("Asignaciones disponibles", service.listarMisAsignaciones()));
    }

    @GetMapping("/evaluaciones")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_READ','CALIFICACIONES_READ_ALL','MI_AREA_READ') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<List<EvaluacionResponse>>> listarEvaluaciones(
            @RequestParam UUID idMateria,
            @RequestParam(required = false) Integer periodo) {
        return ResponseEntity.ok(ApiResponse.ok("Evaluaciones", service.listarEvaluacionesPorMateria(idMateria, periodo)));
    }

    @PostMapping("/evaluaciones")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_CREATE','CALIFICACIONES_UPDATE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<EvaluacionResponse>> crearEvaluacion(
            @Valid @RequestBody EvaluacionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Evaluación creada", service.crearEvaluacion(request)));
    }

    @PutMapping("/evaluaciones/{id}")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_CREATE','CALIFICACIONES_UPDATE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<EvaluacionResponse>> actualizarEvaluacion(
            @PathVariable UUID id,
            @Valid @RequestBody EvaluacionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Evaluación actualizada", service.actualizarEvaluacion(id, request)));
    }

    @DeleteMapping("/evaluaciones/{id}")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_DELETE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DOCENTE')")
    public ResponseEntity<ApiResponse<Void>> eliminarEvaluacion(@PathVariable UUID id) {
        service.eliminarEvaluacion(id);
        return ResponseEntity.ok(ApiResponse.ok("Evaluacion eliminada", null));
    }

    @GetMapping("/plantilla")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_READ','CALIFICACIONES_READ_ALL','MI_AREA_READ') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<CalificacionPlantillaResponse>> obtenerPlantilla(
            @RequestParam UUID idEvaluacion) {
        return ResponseEntity.ok(ApiResponse.ok("Plantilla de calificaciones", service.obtenerPlantilla(idEvaluacion)));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_CREATE','CALIFICACIONES_UPDATE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<CalificacionPlantillaResponse>> guardarCalificaciones(
            @Valid @RequestBody CalificacionRegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Calificaciones guardadas", service.guardarCalificaciones(request)));
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_READ','CALIFICACIONES_READ_ALL','MI_AREA_READ') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<CalificacionResumenResponse>> obtenerResumen(
            @RequestParam UUID idAsignacionDocente,
            @RequestParam Integer periodo) {
        return ResponseEntity.ok(ApiResponse.ok("Resumen de calificaciones", service.obtenerResumen(idAsignacionDocente, periodo)));
    }
}
