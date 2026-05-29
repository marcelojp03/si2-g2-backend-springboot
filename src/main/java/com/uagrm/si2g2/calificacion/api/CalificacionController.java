package com.uagrm.si2g2.calificacion.api;

import com.uagrm.si2g2.auditoria.dto.BitacoraAuditoriaResponse;
import com.uagrm.si2g2.calificacion.application.CalificacionTrimestralService;
import com.uagrm.si2g2.calificacion.application.CalificacionService;
import com.uagrm.si2g2.calificacion.domain.PeriodoTrimestral;
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
 * - Listar asignaciones docentes disponibles como punto de entrada para
 * seleccionar una materia
 * - CRUD de evaluaciones a nivel de materia (parciales, exámenes, trabajos,
 * proyectos)
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
    private final CalificacionTrimestralService trimestralService;

    /**
     * GET /api/calificaciones/mis-asignaciones
     * 
     * Lista las asignaciones docentes activas donde el usuario actual puede
     * registrar calificaciones. En esta versión la UI usa la asignación solo
     * para derivar la materia activa.
     * Para docentes, muestra solo sus asignaciones. Para administradores,
     * muestra todas las asignaciones de la institución.
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
     * Obtiene todas las evaluaciones de una materia específica.
     * Opcionalmente filtra por período académico (1, 2, 3, 4, 5, ó 6).
     * 
     * SEGURIDAD: Solo acceso a evaluaciones propias (docente) o institución (admin)
     * 
     * @param idMateria UUID de la materia (requerido)
     * @param periodo   Número de período (1-6). Si es nulo, lista todas las
     *                  evaluaciones
     * @return Lista ordenada de evaluaciones por período y nombre
     */
    @GetMapping("/evaluaciones")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_READ','CALIFICACIONES_WRITE','CALIFICACIONES_READ_ALL','MI_AREA_READ') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<List<EvaluacionResponse>>> listarEvaluaciones(
            @RequestParam UUID idMateria,
            @RequestParam(required = false) Integer periodo) {
        return ResponseEntity
                .ok(ApiResponse.ok("Evaluaciones", service.listarEvaluacionesPorMateria(idMateria, periodo)));
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
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_WRITE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
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
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_WRITE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<EvaluacionResponse>> actualizarEvaluacion(
            @PathVariable UUID id,
            @Valid @RequestBody EvaluacionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Evaluacion actualizada", service.actualizarEvaluacion(id, request)));
    }

    /**
     * DELETE /api/calificaciones/evaluaciones/{id}
     * 
     * Elimina una evaluación. Solo se permite si no tiene calificaciones registradas.
     * Si ya tiene notas, cambia su estado a ANULADA mediante el endpoint PUT.
     *
     * @param id UUID de la evaluación a eliminar
     * @return Mensaje de confirmación
     */
    @DeleteMapping("/evaluaciones/{id}")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_WRITE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DOCENTE')")
    public ResponseEntity<ApiResponse<Void>> eliminarEvaluacion(@PathVariable UUID id) {
        service.eliminarEvaluacion(id);
        return ResponseEntity.ok(ApiResponse.ok("Evaluacion eliminada", null));
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
     * SEGURIDAD: Valida acceso a la materia asociada a la evaluación
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
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_WRITE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
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
     * SEGURIDAD: Valida acceso a la materia asociada a las evaluaciones
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

    @GetMapping("/trimestres/actividades")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_READ','CALIFICACIONES_WRITE','CALIFICACIONES_READ_ALL','MI_AREA_READ') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<List<ActividadEvaluativaResponse>>> listarActividadesTrimestrales(
            @RequestParam UUID idGestionAcademica,
            @RequestParam Integer trimestre,
            @RequestParam(required = false) UUID idCurso,
            @RequestParam(required = false) UUID idParalelo,
            @RequestParam(required = false) UUID idMateria) {
        return ResponseEntity.ok(ApiResponse.ok("Actividades trimestrales",
                trimestralService.listarActividades(idGestionAcademica, trimestre, idCurso, idParalelo, idMateria)));
    }

    @PostMapping("/trimestres/actividades")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_WRITE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<ActividadEvaluativaResponse>> crearActividadTrimestral(
            @Valid @RequestBody ActividadEvaluativaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Actividad trimestral creada", trimestralService.crearActividad(request)));
    }

    @PutMapping("/trimestres/actividades/{id}")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_WRITE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<ActividadEvaluativaResponse>> actualizarActividadTrimestral(
            @PathVariable UUID id,
            @Valid @RequestBody ActividadEvaluativaRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Actividad trimestral actualizada",
                trimestralService.actualizarActividad(id, request)));
    }

    @PatchMapping("/trimestres/actividades/{id}/estado")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_WRITE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<ActividadEvaluativaResponse>> cambiarEstadoActividadTrimestral(
            @PathVariable UUID id,
            @RequestParam String estado) {
        return ResponseEntity.ok(ApiResponse.ok("Estado de actividad actualizado",
                trimestralService.cambiarEstadoActividad(id, estado)));
    }

    @GetMapping("/trimestres/actividades/{id}/calificaciones")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_READ','CALIFICACIONES_WRITE','CALIFICACIONES_READ_ALL','MI_AREA_READ') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<List<CalificacionActividadResponse>>> listarCalificacionesActividadTrimestral(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Calificaciones de actividad",
                trimestralService.listarCalificacionesActividad(id)));
    }

    @PostMapping("/trimestres/calificaciones-actividad")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_WRITE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<List<CalificacionActividadResponse>>> guardarCalificacionesActividadTrimestral(
            @Valid @RequestBody CalificacionActividadRegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Calificaciones de actividad guardadas",
                        trimestralService.guardarCalificacionesActividad(request)));
    }

    @GetMapping("/trimestres/ser")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_READ','CALIFICACIONES_WRITE','CALIFICACIONES_READ_ALL','MI_AREA_READ') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<List<CalificacionSerResponse>>> listarSerTrimestral(
            @RequestParam UUID idGestionAcademica,
            @RequestParam Integer trimestre,
            @RequestParam UUID idMateria) {
        return ResponseEntity.ok(ApiResponse.ok("Calificaciones SER",
                trimestralService.listarSer(idGestionAcademica, trimestre, idMateria)));
    }

    @PostMapping("/trimestres/ser")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_WRITE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<CalificacionSerResponse>> guardarSerTrimestral(
            @Valid @RequestBody CalificacionSerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("SER registrado", trimestralService.guardarSer(request)));
    }

    @GetMapping("/trimestres/autoevaluacion")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_READ','CALIFICACIONES_WRITE','CALIFICACIONES_READ_ALL','MI_AREA_READ') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE','ESTUDIANTE')")
    public ResponseEntity<ApiResponse<List<AutoevaluacionTrimestralResponse>>> listarAutoevaluacionTrimestral(
            @RequestParam UUID idGestionAcademica,
            @RequestParam Integer trimestre,
            @RequestParam UUID idMateria) {
        return ResponseEntity.ok(ApiResponse.ok("Autoevaluaciones trimestrales",
                trimestralService.listarAutoevaluaciones(idGestionAcademica, trimestre, idMateria)));
    }

    @PostMapping("/trimestres/autoevaluacion")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_WRITE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','ESTUDIANTE')")
    public ResponseEntity<ApiResponse<AutoevaluacionTrimestralResponse>> guardarAutoevaluacionTrimestral(
            @Valid @RequestBody AutoevaluacionTrimestralRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Autoevaluacion registrada",
                        trimestralService.guardarAutoevaluacion(request)));
    }

    @GetMapping("/trimestres/consolidado/estudiante")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_READ','CALIFICACIONES_READ_ALL','MI_AREA_READ') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE','ESTUDIANTE')")
    public ResponseEntity<ApiResponse<List<ConsolidadoTrimestralEstudianteResponse>>> consolidadoEstudiante(
            @RequestParam UUID idGestionAcademica,
            @RequestParam Integer trimestre,
            @RequestParam UUID idEstudiante) {
        return ResponseEntity.ok(ApiResponse.ok("Consolidado del estudiante",
                trimestralService.obtenerConsolidadoEstudiante(idGestionAcademica, trimestre, idEstudiante)));
    }

    @GetMapping("/trimestres/consolidado/docente")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_READ','CALIFICACIONES_READ_ALL','MI_AREA_READ') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR','DOCENTE')")
    public ResponseEntity<ApiResponse<List<ConsolidadoTrimestralMateriaResponse>>> consolidadoDocente(
            @RequestParam UUID idGestionAcademica,
            @RequestParam Integer trimestre) {
        return ResponseEntity.ok(ApiResponse.ok("Consolidado del docente",
                trimestralService.obtenerConsolidadoDocente(idGestionAcademica, trimestre)));
    }

    @GetMapping("/trimestres/consolidado/director")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_READ_ALL','MI_AREA_READ') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR')")
    public ResponseEntity<ApiResponse<ConsolidadoTrimestralDirectorResponse>> consolidadoDirector(
            @RequestParam UUID idGestionAcademica,
            @RequestParam Integer trimestre) {
        return ResponseEntity.ok(ApiResponse.ok("Consolidado directivo",
                trimestralService.obtenerConsolidadoDirector(idGestionAcademica, trimestre)));
    }

    @PostMapping("/trimestres/cerrar")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_WRITE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR')")
    public ResponseEntity<ApiResponse<PeriodoTrimestral>> cerrarTrimestre(
            @Valid @RequestBody TrimestreCierreRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Trimestre cerrado",
                trimestralService.cerrarTrimestre(request.idGestionAcademica(), request.trimestre(),
                        request.justificacion())));
    }

    @PostMapping("/trimestres/reabrir")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_WRITE') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR')")
    public ResponseEntity<ApiResponse<PeriodoTrimestral>> reabrirTrimestre(
            @Valid @RequestBody TrimestreCierreRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Trimestre reabierto",
                trimestralService.reabrirTrimestre(request.idGestionAcademica(), request.trimestre(),
                        request.justificacion())));
    }

    @GetMapping("/trimestres/bitacora")
    @PreAuthorize("hasAnyAuthority('CALIFICACIONES_READ_ALL') or hasAnyRole('ADMIN_INSTITUCION','SUPER_ADMIN','DIRECTOR')")
    public ResponseEntity<ApiResponse<List<BitacoraAuditoriaResponse>>> bitacoraTrimestral(
            @RequestParam(required = false) String modulo,
            @RequestParam(required = false) String tipoOperacion,
            @RequestParam(required = false) Boolean exito) {
        return ResponseEntity.ok(ApiResponse.ok("Bitacora trimestral",
                trimestralService.listarBitacora(modulo, tipoOperacion, exito)));
    }
}
