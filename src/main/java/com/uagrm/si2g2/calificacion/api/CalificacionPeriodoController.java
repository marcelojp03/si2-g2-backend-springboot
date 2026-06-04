package com.uagrm.si2g2.calificacion.api;

import com.uagrm.si2g2.calificacion.application.CalificacionDimensionService;
import com.uagrm.si2g2.calificacion.application.PeriodoEvaluacionService;
import com.uagrm.si2g2.calificacion.dto.*;
import com.uagrm.si2g2.common.dto.ApiResponse;
import com.uagrm.si2g2.common.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/calificaciones/periodos")
@RequiredArgsConstructor
public class CalificacionPeriodoController {

    private final PeriodoEvaluacionService periodoService;
    private final CalificacionDimensionService dimensionService;

    @GetMapping("/{idPeriodo}")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_READ','CALIFICACIONES_READ_ALL','MI_AREA_READ') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<PeriodoEvaluacionResponse>> obtenerPeriodo(@PathVariable UUID idPeriodo) {
        return ResponseEntity.ok(ApiResponse.ok("Periodo encontrado", periodoService.obtener(idPeriodo)));
    }

    @GetMapping("/{idPeriodo}/actividades")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_READ','CALIFICACIONES_READ_ALL','MI_AREA_READ') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<List<ActividadEvaluativaResponse>>> listarActividades(
            @PathVariable UUID idPeriodo,
            @RequestParam(required = false) String dimension) {
        return ResponseEntity.ok(ApiResponse.ok("Actividades del periodo",
                dimensionService.listarActividadesPorPeriodo(idPeriodo, dimension)));
    }

    @PostMapping("/{idPeriodo}/actividades")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_CREATE','CALIFICACIONES_UPDATE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<ActividadEvaluativaResponse>> crearActividad(
            @PathVariable UUID idPeriodo,
            @Valid @RequestBody ActividadEvaluativaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Actividad creada", dimensionService.crearActividad(idPeriodo, request)));
    }

    @PutMapping("/{idPeriodo}/actividades/{idActividad}")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_CREATE','CALIFICACIONES_UPDATE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<ActividadEvaluativaResponse>> actualizarActividad(
            @PathVariable UUID idPeriodo,
            @PathVariable UUID idActividad,
            @Valid @RequestBody ActividadEvaluativaRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Actividad actualizada",
                dimensionService.actualizarActividad(idActividad, request)));
    }

    @DeleteMapping("/{idPeriodo}/actividades/{idActividad}")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_DELETE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<Void>> eliminarActividad(
            @PathVariable UUID idPeriodo,
            @PathVariable UUID idActividad) {
        dimensionService.eliminarActividad(idActividad);
        return ResponseEntity.ok(ApiResponse.ok("Actividad eliminada", null));
    }

    @GetMapping("/{idPeriodo}/actividades/{idActividad}/calificaciones")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_READ','CALIFICACIONES_READ_ALL','MI_AREA_READ') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<List<CalificacionActividadResponse>>> listarCalificacionesActividad(
            @PathVariable UUID idPeriodo,
            @PathVariable UUID idActividad) {
        return ResponseEntity.ok(ApiResponse.ok("Calificaciones de actividad",
                dimensionService.listarCalificacionesActividad(idActividad)));
    }

    @PostMapping("/{idPeriodo}/calificaciones-actividad")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_CREATE','CALIFICACIONES_UPDATE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<List<CalificacionActividadResponse>>> guardarCalificacionesActividad(
            @PathVariable UUID idPeriodo,
            @Valid @RequestBody CalificacionActividadRegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Calificaciones guardadas",
                        dimensionService.registrarCalificacionesActividad(request)));
    }

    @GetMapping("/{idPeriodo}/ser/{idEstudiante}/{idMateria}")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_READ','CALIFICACIONES_READ_ALL','MI_AREA_READ') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<CalificacionSerResponse>> obtenerSer(
            @PathVariable UUID idPeriodo,
            @PathVariable UUID idEstudiante,
            @PathVariable UUID idMateria) {
        CalificacionSerResponse response = dimensionService.obtenerSer(idPeriodo, idEstudiante, idMateria);
        return ResponseEntity.ok(ApiResponse.ok("Calificacion SER", response));
    }

    @PostMapping("/{idPeriodo}/ser")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_CREATE','CALIFICACIONES_UPDATE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<CalificacionSerResponse>> guardarSer(
            @PathVariable UUID idPeriodo,
            @Valid @RequestBody CalificacionSerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("SER registrado", dimensionService.guardarSer(idPeriodo, request)));
    }

    @GetMapping("/{idPeriodo}/ser/{idEstudiante}/{idMateria}/observaciones")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_READ','CALIFICACIONES_READ_ALL','MI_AREA_READ') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<List<ObservacionSerResponse>>> listarObservacionesSer(
            @PathVariable UUID idPeriodo,
            @PathVariable UUID idEstudiante,
            @PathVariable UUID idMateria) {
        return ResponseEntity.ok(ApiResponse.ok("Observaciones SER",
                dimensionService.listarObservacionesSer(idPeriodo, idEstudiante, idMateria)));
    }

    @PostMapping("/{idPeriodo}/ser/observaciones")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_CREATE','CALIFICACIONES_UPDATE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<ObservacionSerResponse>> agregarObservacionSer(
            @PathVariable UUID idPeriodo,
            @Valid @RequestBody ObservacionSerRequest request) {
        UUID idDocente = SecurityUtils.currentUserId() != null ?
                dimensionService.obtenerDocenteId(SecurityUtils.currentUserId()) : null;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Observacion agregada",
                        dimensionService.agregarObservacionSer(idPeriodo, idDocente, request)));
    }

    @GetMapping("/{idPeriodo}/autoevaluacion/{idEstudiante}/{idMateria}")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_READ','CALIFICACIONES_READ_ALL','MI_AREA_READ') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE','ESTUDIANTE')")
    public ResponseEntity<ApiResponse<AutoevaluacionTrimestralResponse>> obtenerAutoevaluacion(
            @PathVariable UUID idPeriodo,
            @PathVariable UUID idEstudiante,
            @PathVariable UUID idMateria) {
        AutoevaluacionTrimestralResponse response = dimensionService.obtenerAutoevaluacion(idPeriodo, idEstudiante, idMateria);
        return ResponseEntity.ok(ApiResponse.ok("Autoevaluacion", response));
    }

    @PostMapping("/{idPeriodo}/autoevaluacion")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_CREATE','CALIFICACIONES_UPDATE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','ESTUDIANTE')")
    public ResponseEntity<ApiResponse<AutoevaluacionTrimestralResponse>> guardarAutoevaluacion(
            @PathVariable UUID idPeriodo,
            @Valid @RequestBody AutoevaluacionTrimestralRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Autoevaluacion registrada",
                        dimensionService.guardarAutoevaluacion(idPeriodo, request)));
    }

    @GetMapping("/{idPeriodo}/consolidado/{idEstudiante}/{idMateria}")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_READ','CALIFICACIONES_READ_ALL','MI_AREA_READ') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE','ESTUDIANTE')")
    public ResponseEntity<ApiResponse<ConsolidadoEstudianteResponse>> consolidadoEstudiante(
            @PathVariable UUID idPeriodo,
            @PathVariable UUID idEstudiante,
            @PathVariable UUID idMateria) {
        return ResponseEntity.ok(ApiResponse.ok("Consolidado del estudiante",
                dimensionService.obtenerConsolidadoEstudiante(idPeriodo, idEstudiante, idMateria)));
    }

    @PostMapping("/{idPeriodo}/cerrar")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_UPDATE','CALIFICACIONES_OVERRIDE_CIERRE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR')")
    public ResponseEntity<ApiResponse<PeriodoEvaluacionResponse>> cerrarPeriodo(
            @PathVariable UUID idPeriodo,
            @RequestBody PeriodoCierreRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Periodo cerrado",
                periodoService.cerrarPeriodo(idPeriodo, request.justificacion())));
    }

    @PostMapping("/{idPeriodo}/reabrir")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_UPDATE','CALIFICACIONES_OVERRIDE_CIERRE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR')")
    public ResponseEntity<ApiResponse<PeriodoEvaluacionResponse>> reopenPeriodo(
            @PathVariable UUID idPeriodo,
            @RequestBody PeriodoCierreRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Periodo reabierto",
                periodoService.reopenPeriodo(idPeriodo, request.justificacion())));
    }
}