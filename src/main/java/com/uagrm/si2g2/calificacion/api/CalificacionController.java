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
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_READ','CALIFICACIONES_WRITE','CALIFICACIONES_READ_ALL','MI_AREA_READ') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<List<CalificacionAsignacionResponse>>> listarMisAsignaciones() {
        return ResponseEntity.ok(ApiResponse.ok("Asignaciones disponibles", service.listarMisAsignaciones()));
    }

    @GetMapping("/evaluaciones")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_READ','CALIFICACIONES_WRITE','CALIFICACIONES_READ_ALL','MI_AREA_READ') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<List<EvaluacionResponse>>> listarEvaluaciones(
            @RequestParam UUID idAsignacionDocente,
            @RequestParam(required = false) Integer periodo) {
        return ResponseEntity.ok(ApiResponse.ok("Evaluaciones", service.listarEvaluaciones(idAsignacionDocente, periodo)));
    }

    @PostMapping("/evaluaciones")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_WRITE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DOCENTE')")
    public ResponseEntity<ApiResponse<EvaluacionResponse>> crearEvaluacion(@Valid @RequestBody EvaluacionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Evaluacion creada", service.crearEvaluacion(request)));
    }

    @PutMapping("/evaluaciones/{id}")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_WRITE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DOCENTE')")
    public ResponseEntity<ApiResponse<EvaluacionResponse>> actualizarEvaluacion(
            @PathVariable UUID id,
            @Valid @RequestBody EvaluacionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Evaluacion actualizada", service.actualizarEvaluacion(id, request)));
    }

    @GetMapping("/plantilla")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_READ','CALIFICACIONES_WRITE','CALIFICACIONES_READ_ALL','MI_AREA_READ') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<CalificacionPlantillaResponse>> obtenerPlantilla(@RequestParam UUID idEvaluacion) {
        return ResponseEntity.ok(ApiResponse.ok("Plantilla de calificaciones", service.obtenerPlantilla(idEvaluacion)));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_WRITE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DOCENTE')")
    public ResponseEntity<ApiResponse<CalificacionPlantillaResponse>> guardarCalificaciones(
            @Valid @RequestBody CalificacionRegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Calificaciones guardadas", service.guardarCalificaciones(request)));
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_READ','CALIFICACIONES_WRITE','CALIFICACIONES_READ_ALL','MI_AREA_READ') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<CalificacionResumenResponse>> obtenerResumen(
            @RequestParam UUID idAsignacionDocente,
            @RequestParam Integer periodo) {
        return ResponseEntity.ok(ApiResponse.ok("Resumen de calificaciones", service.obtenerResumen(idAsignacionDocente, periodo)));
    }
}
