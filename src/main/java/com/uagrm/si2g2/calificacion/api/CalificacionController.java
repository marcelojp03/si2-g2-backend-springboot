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

/**
 * ====================================================================
 * HU-S2-18: GESTIONAR CALIFICACIONES
 * ====================================================================
 * 
 * Controlador REST para la gestión de evaluaciones y calificaciones.
 * Proporciona endpoints para:
 * - Listar asignaciones docentes disponibles
 * - CRUD de evaluaciones (parciales, exámenes, trabajos, proyectos)
 * - Registrar calificaciones numéricas (0-100) o literales (A-F)
 * - Generar reportes de desempeño de estudiantes
 * 
 * SEGURIDAD:
 * - Restringe acceso por roles: DOCENTE, ADMIN_INSTITUCION, SUPER_ADMIN,
 * DIRECTOR
 * - Valida permisos específicos: CALIFICACIONES_READ, CALIFICACIONES_WRITE,
 * etc.
 * - Aislamiento por institución (id_institucion) automático via TenantContext
 * - Solo docentes ven sus propias asignaciones
 * 
 * ARQUITECTURA:
 * - Patrón Controlador → Servicio → Repositorio
 * - Validación delegada al CalificacionService
 * - Respuestas estandarizadas en ApiResponse<T>
 * ====================================================================
 */
@RestController
@RequestMapping("/api/calificaciones")
@RequiredArgsConstructor
public class CalificacionController {

    private final CalificacionService service;

    /**
     * GET /api/calificaciones/mis-asignaciones
     * 
     * Lista las asignaciones docentes activas donde el usuario actual puede
     * registrar calificaciones. Para docentes, muestra solo sus asignaciones.
     * Para administradores, muestra todas las asignaciones de la institución.
     * 
     * SEGURIDAD: Lee datos sin modificar
     * 
     * @return Lista de asignaciones docentes con información de materia, curso y
     *         paralelo
     */
    @GetMapping("/mis-asignaciones")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_READ','CALIFICACIONES_WRITE','CALIFICACIONES_READ_ALL','MI_AREA_READ') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<List<CalificacionAsignacionResponse>>> listarMisAsignaciones() {
        return ResponseEntity.ok(ApiResponse.ok("Asignaciones disponibles", service.listarMisAsignaciones()));
    }

    /**
     * GET /api/calificaciones/evaluaciones
     * 
     * Obtiene todas las evaluaciones de una asignación docente específica.
     * Opcionalmente filtra por período académico (1, 2, 3, 4, 5, ó 6).
     * 
     * SEGURIDAD: Solo acceso a evaluaciones propias (docente) o institución (admin)
     * 
     * @param idAsignacionDocente UUID de la asignación docente (requerido)
     * @param periodo             Número de período (1-6). Si es nulo, lista todas
     *                            las evaluaciones
     * @return Lista ordenada de evaluaciones por período y nombre
     */
    @GetMapping("/evaluaciones")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_READ','CALIFICACIONES_WRITE','CALIFICACIONES_READ_ALL','MI_AREA_READ') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<List<EvaluacionResponse>>> listarEvaluaciones(
            @RequestParam UUID idAsignacionDocente,
            @RequestParam(required = false) Integer periodo) {
        return ResponseEntity
                .ok(ApiResponse.ok("Evaluaciones", service.listarEvaluaciones(idAsignacionDocente, periodo)));
    }

    /**
     * POST /api/calificaciones/evaluaciones
     * 
     * Crea una nueva evaluación (parcial, examen, trabajo práctico, proyecto,
     * participación).
     * Valida que:
     * - La ponderación no supere 100% en el período
     * - No exista otra evaluación con el mismo nombre
     * - El período sea válido (1-6)
     * 
     * SEGURIDAD: Solo docentes propietarios o administradores
     * AUDITORÍA: Registra creación de evaluación
     * 
     * @param request Datos de la evaluación (nombre, tipo, ponderación, escala)
     * @return Evaluación creada con ID asignado
     */
    @PostMapping("/evaluaciones")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_WRITE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DOCENTE')")
    public ResponseEntity<ApiResponse<EvaluacionResponse>> crearEvaluacion(
            @Valid @RequestBody EvaluacionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Evaluacion creada", service.crearEvaluacion(request)));
    }

    /**
     * PUT /api/calificaciones/evaluaciones/{id}
     * 
     * Actualiza una evaluación existente (nombre, tipo, ponderación, estado).
     * Si está CERRADA, solo SUPER_ADMIN/ADMIN_INSTITUCION pueden cambiarla
     * con permiso CALIFICACIONES_OVERRIDE_CIERRE.
     * 
     * AUDITORÍA: Registra cambios con valores anteriores y nuevos
     * 
     * @param id      UUID de la evaluación a actualizar
     * @param request Nuevos datos de la evaluación
     * @return Evaluación actualizada
     */
    @PutMapping("/evaluaciones/{id}")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_WRITE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DOCENTE')")
    public ResponseEntity<ApiResponse<EvaluacionResponse>> actualizarEvaluacion(
            @PathVariable UUID id,
            @Valid @RequestBody EvaluacionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Evaluacion actualizada", service.actualizarEvaluacion(id, request)));
    }

    /**
     * GET /api/calificaciones/plantilla
     * 
     * Obtiene la plantilla de calificaciones para una evaluación específica.
     * Incluye:
     * - Lista de estudiantes inscritos en el paralelo
     * - Calificaciones ya registradas (si existen)
     * - Información de escala (numérica 0-100 o literal A-F)
     * - Indicador si la evaluación es editable
     * 
     * SEGURIDAD: Valida acceso a la asignación docente
     * 
     * @param idEvaluacion UUID de la evaluación
     * @return Plantilla con estudiantes y sus notas actuales
     */
    @GetMapping("/plantilla")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_READ','CALIFICACIONES_WRITE','CALIFICACIONES_READ_ALL','MI_AREA_READ') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<CalificacionPlantillaResponse>> obtenerPlantilla(
            @RequestParam UUID idEvaluacion) {
        return ResponseEntity.ok(ApiResponse.ok("Plantilla de calificaciones", service.obtenerPlantilla(idEvaluacion)));
    }

    /**
     * POST /api/calificaciones
     * 
     * Guarda o actualiza calificaciones de múltiples estudiantes para una
     * evaluación.
     * Características:
     * - Soporta notas numéricas (0-100) o literales (A-F)
     * - Si modifica nota existente, requiere "razonCambio" (auditoría)
     * - Registra cambios en tabla CalificacionCambio para trazabilidad
     * - Valida que evaluación esté ABIERTA
     * 
     * AUDITORÍA: Registra cantidad de calificaciones guardadas y cambios realizados
     * 
     * @param request Evaluación + lista de notas por estudiante
     * @return Plantilla actualizada después de guardar
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_WRITE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DOCENTE')")
    public ResponseEntity<ApiResponse<CalificacionPlantillaResponse>> guardarCalificaciones(
            @Valid @RequestBody CalificacionRegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Calificaciones guardadas", service.guardarCalificaciones(request)));
    }

    /**
     * GET /api/calificaciones/resumen
     * 
     * Genera un resumen de desempeño de todos los estudiantes en un período.
     * Calcula:
     * - Nota consolidada = Σ(nota × ponderación) / 100
     * - Estado académico: APROBADO (≥ nota mínima) o EN_RIESGO (< nota mínima)
     * - Ponderación total registrada vs esperada
     * 
     * SEGURIDAD: Valida acceso a la asignación docente
     * 
     * @param idAsignacionDocente UUID de la asignación docente
     * @param periodo             Número de período (1-6)
     * @return Resumen con estadísticas por estudiante
     */
    @GetMapping("/resumen")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_READ','CALIFICACIONES_WRITE','CALIFICACIONES_READ_ALL','MI_AREA_READ') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<CalificacionResumenResponse>> obtenerResumen(
            @RequestParam UUID idAsignacionDocente,
            @RequestParam Integer periodo) {
        return ResponseEntity
                .ok(ApiResponse.ok("Resumen de calificaciones", service.obtenerResumen(idAsignacionDocente, periodo)));
    }
}
