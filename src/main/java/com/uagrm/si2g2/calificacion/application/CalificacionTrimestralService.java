package com.uagrm.si2g2.calificacion.application;

import com.uagrm.si2g2.academico.domain.GestionAcademica;
import com.uagrm.si2g2.academico.domain.GestionAcademicaRepository;
import com.uagrm.si2g2.asignacion.domain.AsignacionDocente;
import com.uagrm.si2g2.asignacion.domain.AsignacionDocenteRepository;
import com.uagrm.si2g2.auditoria.application.AuditoriaQueryService;
import com.uagrm.si2g2.auditoria.application.AuditoriaService;
import com.uagrm.si2g2.auditoria.dto.BitacoraAuditoriaFiltro;
import com.uagrm.si2g2.auditoria.dto.BitacoraAuditoriaResponse;
import com.uagrm.si2g2.calificacion.domain.*;
import com.uagrm.si2g2.calificacion.dto.*;
import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.curso.domain.Curso;
import com.uagrm.si2g2.curso.domain.CursoRepository;
import com.uagrm.si2g2.curso.domain.Paralelo;
import com.uagrm.si2g2.curso.domain.ParaleloRepository;
import com.uagrm.si2g2.docente.domain.Docente;
import com.uagrm.si2g2.docente.domain.DocenteRepository;
import com.uagrm.si2g2.estudiante.domain.Estudiante;
import com.uagrm.si2g2.estudiante.domain.EstudianteRepository;
import com.uagrm.si2g2.inscripcion.domain.Inscripcion;
import com.uagrm.si2g2.inscripcion.domain.InscripcionRepository;
import com.uagrm.si2g2.materia.domain.Materia;
import com.uagrm.si2g2.materia.domain.MateriaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalificacionTrimestralService {

    private static final String ESTADO_ACTIVO = "ACTIVA";
    private static final String ESTADO_ABIERTO = "ABIERTO";
    private static final String ESTADO_CERRADO = "CERRADO";
    private static final String ESTADO_BORRADOR = "BORRADOR";
    private static final String DIMENSION_SER = "SER";
    private static final String DIMENSION_SABER = "SABER";
    private static final String DIMENSION_HACER = "HACER";
    private static final String MODULO = "CALIFICACIONES_TRIMESTRALES";
    private static final BigDecimal PESO_SER = new BigDecimal("10");
    private static final BigDecimal PESO_SABER = new BigDecimal("45");
    private static final BigDecimal PESO_HACER = new BigDecimal("40");
    private static final BigDecimal PESO_AUTOEVALUACION = new BigDecimal("5");
    private static final BigDecimal NOTA_APROBACION = new BigDecimal("51");

    private final PeriodoTrimestralRepository periodoRepository;
    private final ActividadEvaluativaRepository actividadRepository;
    private final CalificacionActividadRepository calificacionActividadRepository;
    private final CalificacionSerRepository calificacionSerRepository;
    private final AutoevaluacionTrimestralRepository autoevaluacionRepository;
    private final GestionAcademicaRepository gestionAcademicaRepository;
    private final AsignacionDocenteRepository asignacionDocenteRepository;
    private final InscripcionRepository inscripcionRepository;
    private final EstudianteRepository estudianteRepository;
    private final DocenteRepository docenteRepository;
    private final MateriaRepository materiaRepository;
    private final CursoRepository cursoRepository;
    private final ParaleloRepository paraleloRepository;
    private final AuditoriaService auditoriaService;
    private final AuditoriaQueryService auditoriaQueryService;

    @Transactional
    public List<ActividadEvaluativaResponse> listarActividades(UUID idGestionAcademica, Integer trimestre,
            UUID idCurso, UUID idParalelo, UUID idMateria) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        PeriodoTrimestral periodo = obtenerPeriodo(idInstitucion, idGestionAcademica, trimestre, true);

        List<ActividadEvaluativa> actividades = actividadRepository
                .findAllByIdInstitucionAndIdPeriodoTrimestral(idInstitucion, periodo.getId());

        return actividades.stream()
                .filter(actividad -> idCurso == null || Objects.equals(actividad.getIdCurso(), idCurso))
                .filter(actividad -> idParalelo == null || Objects.equals(actividad.getIdParalelo(), idParalelo))
                .filter(actividad -> idMateria == null || Objects.equals(actividad.getIdMateria(), idMateria))
                .sorted(Comparator.comparing(ActividadEvaluativa::getFechaActividad)
                        .thenComparing(ActividadEvaluativa::getNombreActividad))
                .map(ActividadEvaluativaResponse::from)
                .toList();
    }

    @Transactional
    public ActividadEvaluativaResponse crearActividad(ActividadEvaluativaRequest request) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        validarTrimestre(request.trimestre());
        GestionAcademica gestion = buscarGestion(idInstitucion, request.idGestionAcademica());
        PeriodoTrimestral periodo = obtenerPeriodo(idInstitucion, gestion.getId(), request.trimestre(), true);
        validarDocente(request.idDocente(), idInstitucion);
        validarAccesoDocenteMateria(request.idDocente(), request.idMateria(), request.idParalelo(), gestion.getId(),
                idInstitucion);

        String dimension = normalizarDimension(request.dimension());
        String tipoActividad = normalizarTexto(request.tipoActividad(), "El tipo de actividad es obligatorio");
        String nombreActividad = normalizarTexto(request.nombreActividad(), "El nombre de la actividad es obligatorio");

        if (!DIMENSION_SABER.equals(dimension) && !DIMENSION_HACER.equals(dimension)) {
            throw new IllegalArgumentException("Las actividades solo se permiten para SABER o HACER");
        }
        if (actividadRepository.existsByIdInstitucionAndIdPeriodoTrimestralAndNombreActividadIgnoreCase(idInstitucion,
                periodo.getId(), nombreActividad)) {
            throw new IllegalStateException("Ya existe una actividad con ese nombre en el trimestre");
        }

        ActividadEvaluativa actividad = ActividadEvaluativa.builder()
                .idInstitucion(idInstitucion)
                .idPeriodoTrimestral(periodo.getId())
                .idGestionAcademica(gestion.getId())
                .idCurso(request.idCurso())
                .idParalelo(request.idParalelo())
                .idMateria(request.idMateria())
                .idDocente(request.idDocente())
                .nombreActividad(nombreActividad)
                .tipoActividad(tipoActividad)
                .dimension(dimension)
                .puntajeMaximo(100)
                .fechaActividad(resolverFechaActividad(request.fechaActividad(), Instant.now()))
                .descripcion(request.descripcion())
                .estado(normalizarEstadoActividad(request.estado()))
                .build();

        if (ESTADO_BORRADOR.equals(actividad.getEstado())) {
            actividad.setPublicadoEn(null);
        } else {
            actividad.setPublicadoEn(Instant.now());
        }

        ActividadEvaluativa saved = actividadRepository.save(actividad);
        auditoriaService.registrar(idInstitucion, SecurityUtils.currentUserId(), MODULO, "CREAR_ACTIVIDAD",
                "actividad_evaluativa", saved.getId().toString(), true, "Actividad trimestral creada");
        return ActividadEvaluativaResponse.from(saved);
    }

    @Transactional
    public ActividadEvaluativaResponse actualizarActividad(UUID idActividad, ActividadEvaluativaRequest request) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        ActividadEvaluativa actividad = buscarActividad(idInstitucion, idActividad);
        validarTrimestre(request.trimestre());
        GestionAcademica gestion = buscarGestion(idInstitucion, request.idGestionAcademica());
        PeriodoTrimestral periodo = obtenerPeriodo(idInstitucion, gestion.getId(), request.trimestre(), true);
        validarDocente(request.idDocente(), idInstitucion);
        validarAccesoDocenteMateria(request.idDocente(), request.idMateria(), request.idParalelo(), gestion.getId(),
                idInstitucion);

        Map<String, Object> antes = snapshotActividad(actividad);
        actividad.setIdGestionAcademica(gestion.getId());
        actividad.setIdPeriodoTrimestral(periodo.getId());
        actividad.setIdCurso(request.idCurso());
        actividad.setIdParalelo(request.idParalelo());
        actividad.setIdMateria(request.idMateria());
        actividad.setIdDocente(request.idDocente());
        actividad.setNombreActividad(
                normalizarTexto(request.nombreActividad(), "El nombre de la actividad es obligatorio"));
        actividad.setTipoActividad(normalizarTexto(request.tipoActividad(), "El tipo de actividad es obligatorio"));
        actividad.setDimension(normalizarDimension(request.dimension()));
        actividad.setFechaActividad(resolverFechaActividad(request.fechaActividad(), actividad.getFechaActividad()));
        actividad.setDescripcion(request.descripcion());
        actividad.setEstado(normalizarEstadoActividad(request.estado()));
        if (ESTADO_CERRADO.equals(periodo.getEstado())) {
            throw new IllegalStateException("No se puede modificar una actividad con el trimestre cerrado");
        }

        if (ESTADO_BORRADOR.equals(actividad.getEstado())) {
            actividad.setPublicadoEn(null);
        } else if (actividad.getPublicadoEn() == null) {
            actividad.setPublicadoEn(Instant.now());
        }

        ActividadEvaluativa saved = actividadRepository.save(actividad);
        auditoriaService.registrarDetallado(idInstitucion, SecurityUtils.currentUserId(), MODULO,
                "ACTUALIZAR_ACTIVIDAD", "actividad_evaluativa", saved.getId().toString(), antes,
                snapshotActividad(saved), true, "Actividad actualizada");
        return ActividadEvaluativaResponse.from(saved);
    }

    @Transactional
    public ActividadEvaluativaResponse cambiarEstadoActividad(UUID idActividad, String nuevoEstado) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        ActividadEvaluativa actividad = buscarActividad(idInstitucion, idActividad);
        Map<String, Object> antes = snapshotActividad(actividad);
        actividad.setEstado(normalizarEstadoActividad(nuevoEstado));
        if (ESTADO_BORRADOR.equals(actividad.getEstado())) {
            actividad.setPublicadoEn(null);
        } else if (actividad.getPublicadoEn() == null) {
            actividad.setPublicadoEn(Instant.now());
        }
        ActividadEvaluativa saved = actividadRepository.save(actividad);
        auditoriaService.registrarDetallado(idInstitucion, SecurityUtils.currentUserId(), MODULO,
                "CAMBIAR_ESTADO_ACTIVIDAD", "actividad_evaluativa", saved.getId().toString(), antes,
                snapshotActividad(saved), true, "Estado de actividad actualizado");
        return ActividadEvaluativaResponse.from(saved);
    }

    @Transactional
    public void eliminarActividad(UUID idActividad) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        ActividadEvaluativa actividad = buscarActividad(idInstitucion, idActividad);
        PeriodoTrimestral periodo = buscarPeriodo(idInstitucion, actividad.getIdPeriodoTrimestral());
        validarPeriodoAbierto(periodo);
        validarAccesoDocenteMateria(actividad.getIdDocente(), actividad.getIdMateria(), actividad.getIdParalelo(),
                actividad.getIdGestionAcademica(), idInstitucion);

        if (calificacionActividadRepository.existsByIdActividad(idActividad)) {
            throw new IllegalStateException(
                    "No se puede eliminar la actividad porque ya tiene calificaciones registradas");
        }

        Map<String, Object> antes = snapshotActividad(actividad);
        actividadRepository.delete(actividad);
        auditoriaService.registrarDetallado(idInstitucion, SecurityUtils.currentUserId(), MODULO,
                "ELIMINAR_ACTIVIDAD", "actividad_evaluativa", idActividad.toString(), antes, Map.of(), true,
                "Actividad trimestral eliminada");
    }

    @Transactional(readOnly = true)
    public List<CalificacionActividadResponse> listarCalificacionesActividad(UUID idActividad) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        ActividadEvaluativa actividad = buscarActividad(idInstitucion, idActividad);
        List<Inscripcion> inscripciones = obtenerInscripcionesActivas(actividad.getIdGestionAcademica(),
                actividad.getIdParalelo(), idInstitucion);
        Map<UUID, CalificacionActividad> calificaciones = calificacionActividadRepository
                .findAllByIdActividad(idActividad)
                .stream()
                .collect(Collectors.toMap(CalificacionActividad::getIdEstudiante, calificacion -> calificacion));

        return inscripciones.stream()
                .map(Inscripcion::getIdEstudiante)
                .map(idEstudiante -> calificaciones.getOrDefault(idEstudiante,
                        CalificacionActividad.builder()
                                .idActividad(idActividad)
                                .idEstudiante(idEstudiante)
                                .idInstitucion(idInstitucion)
                                .estado("PENDIENTE")
                                .build()))
                .map(CalificacionActividadResponse::from)
                .toList();
    }

    @Transactional
    public List<CalificacionActividadResponse> guardarCalificacionesActividad(
            CalificacionActividadRegistroRequest request) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        ActividadEvaluativa actividad = buscarActividad(idInstitucion, request.idActividad());
        PeriodoTrimestral periodo = buscarPeriodo(idInstitucion, actividad.getIdPeriodoTrimestral());
        validarPeriodoAbierto(periodo);
        validarAccesoDocenteMateria(actividad.getIdDocente(), actividad.getIdMateria(), actividad.getIdParalelo(),
                actividad.getIdGestionAcademica(), idInstitucion);

        List<Inscripcion> inscripciones = obtenerInscripcionesActivas(actividad.getIdGestionAcademica(),
                actividad.getIdParalelo(), idInstitucion);
        Map<UUID, Inscripcion> inscripcionesPorEstudiante = inscripciones.stream()
                .collect(Collectors.toMap(Inscripcion::getIdEstudiante, inscripcion -> inscripcion));

        List<CalificacionActividadResponse> respuestas = new ArrayList<>();
        for (CalificacionActividadDetalleRequest detalle : request.detalles()) {
            if (!inscripcionesPorEstudiante.containsKey(detalle.idEstudiante())) {
                throw new IllegalArgumentException("El estudiante no pertenece al paralelo de la actividad");
            }
            validarRangoNota(detalle.notaObtenida(), actividad.getPuntajeMaximo());
            CalificacionActividad calificacion = calificacionActividadRepository
                    .findByIdActividadAndIdEstudiante(request.idActividad(), detalle.idEstudiante())
                    .orElseGet(() -> CalificacionActividad.builder()
                            .idInstitucion(idInstitucion)
                            .idActividad(request.idActividad())
                            .idEstudiante(detalle.idEstudiante())
                            .build());

            calificacion.setNotaObtenida(detalle.notaObtenida());
            calificacion.setObservacion(detalle.observacion());
            calificacion.setEstado(detalle.notaObtenida() == null ? "PENDIENTE" : "REGISTRADA");
            calificacion.setIdUsuarioRegistro(
                    calificacion.getId() == null ? SecurityUtils.currentUserId() : calificacion.getIdUsuarioRegistro());
            calificacion.setIdUsuarioModificacion(SecurityUtils.currentUserId());

            CalificacionActividad saved = calificacionActividadRepository.save(calificacion);
            respuestas.add(CalificacionActividadResponse.from(saved));
        }

        auditoriaService.registrar(idInstitucion, SecurityUtils.currentUserId(), MODULO, "REGISTRAR_NOTAS_ACTIVIDAD",
                "calificacion_actividad", request.idActividad().toString(), true,
                "Notas de actividad guardadas: " + respuestas.size());
        return respuestas;
    }

    @Transactional
    public CalificacionSerResponse guardarSer(CalificacionSerRequest request) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        validarTrimestre(request.trimestre());
        GestionAcademica gestion = buscarGestion(idInstitucion, request.idGestionAcademica());
        PeriodoTrimestral periodo = obtenerPeriodo(idInstitucion, gestion.getId(), request.trimestre(), true);
        validarDocente(request.idDocente(), idInstitucion);
        validarAccesoDocenteMateria(request.idDocente(), request.idMateria(), request.idParalelo(), gestion.getId(),
                idInstitucion);
        validarPeriodoAbierto(periodo);
        validarRangoDirecto(request.notaSer(), PESO_SER);

        CalificacionSer calificacion = calificacionSerRepository
                .findByIdInstitucionAndIdGestionAcademicaAndIdTrimestreAndIdMateriaAndIdEstudiante(idInstitucion,
                        gestion.getId(), periodo.getId(), request.idMateria(), request.idEstudiante())
                .orElseGet(() -> CalificacionSer.builder()
                        .idInstitucion(idInstitucion)
                        .idGestionAcademica(gestion.getId())
                        .idTrimestre(periodo.getId())
                        .idCurso(request.idCurso())
                        .idParalelo(request.idParalelo())
                        .idMateria(request.idMateria())
                        .idDocente(request.idDocente())
                        .idEstudiante(request.idEstudiante())
                        .build());
        calificacion.setIdCurso(request.idCurso());
        calificacion.setIdParalelo(request.idParalelo());
        calificacion.setNotaSer(request.notaSer());
        calificacion.setObservacion(request.observacion());
        calificacion.setEstado("REGISTRADA");
        calificacion.setIdUsuarioRegistro(
                calificacion.getId() == null ? SecurityUtils.currentUserId() : calificacion.getIdUsuarioRegistro());
        calificacion.setIdUsuarioModificacion(SecurityUtils.currentUserId());

        CalificacionSer saved = calificacionSerRepository.save(calificacion);
        auditoriaService.registrar(idInstitucion, SecurityUtils.currentUserId(), MODULO, "REGISTRAR_SER",
                "calificacion_ser", saved.getId().toString(), true, "SER registrado");
        return CalificacionSerResponse.from(saved);
    }

    @Transactional
    public List<CalificacionSerResponse> listarSer(UUID idGestionAcademica, Integer trimestre, UUID idMateria) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        PeriodoTrimestral periodo = obtenerPeriodo(idInstitucion, idGestionAcademica, trimestre, true);
        return calificacionSerRepository
                .findAllByIdInstitucionAndIdGestionAcademicaAndIdTrimestreAndIdMateria(idInstitucion,
                        idGestionAcademica, periodo.getId(), idMateria)
                .stream()
                .map(CalificacionSerResponse::from)
                .toList();
    }

    @Transactional
    public AutoevaluacionTrimestralResponse guardarAutoevaluacion(AutoevaluacionTrimestralRequest request) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        validarTrimestre(request.trimestre());
        GestionAcademica gestion = buscarGestion(idInstitucion, request.idGestionAcademica());
        PeriodoTrimestral periodo = obtenerPeriodo(idInstitucion, gestion.getId(), request.trimestre(), true);
        validarPeriodoAbierto(periodo);

        Estudiante estudiante = buscarEstudiante(idInstitucion, request.idEstudiante());
        validarAccesoAutoevaluacion(estudiante.getId());
        validarRangoDirecto(request.notaAutoevaluacion(), PESO_AUTOEVALUACION);

        AutoevaluacionTrimestral autoevaluacion = autoevaluacionRepository
                .findByIdInstitucionAndIdGestionAcademicaAndIdTrimestreAndIdMateriaAndIdEstudiante(idInstitucion,
                        gestion.getId(), periodo.getId(), request.idMateria(), request.idEstudiante())
                .orElseGet(() -> AutoevaluacionTrimestral.builder()
                        .idInstitucion(idInstitucion)
                        .idGestionAcademica(gestion.getId())
                        .idTrimestre(periodo.getId())
                        .idMateria(request.idMateria())
                        .idEstudiante(request.idEstudiante())
                        .build());
        autoevaluacion.setNotaAutoevaluacion(request.notaAutoevaluacion());
        autoevaluacion.setComentario(request.comentario());
        autoevaluacion.setEstado("REGISTRADA");
        autoevaluacion.setIdUsuarioRegistro(
                autoevaluacion.getId() == null ? SecurityUtils.currentUserId() : autoevaluacion.getIdUsuarioRegistro());
        autoevaluacion.setIdUsuarioModificacion(SecurityUtils.currentUserId());

        AutoevaluacionTrimestral saved = autoevaluacionRepository.save(autoevaluacion);
        auditoriaService.registrar(idInstitucion, SecurityUtils.currentUserId(), MODULO, "REGISTRAR_AUTOEVALUACION",
                "autoevaluacion_trimestral", saved.getId().toString(), true, "Autoevaluacion registrada");
        return AutoevaluacionTrimestralResponse.from(saved);
    }

    @Transactional
    public List<AutoevaluacionTrimestralResponse> listarAutoevaluaciones(UUID idGestionAcademica, Integer trimestre,
            UUID idMateria) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        PeriodoTrimestral periodo = obtenerPeriodo(idInstitucion, idGestionAcademica, trimestre, true);
        return autoevaluacionRepository
                .findAllByIdInstitucionAndIdGestionAcademicaAndIdTrimestreAndIdMateria(idInstitucion,
                        idGestionAcademica, periodo.getId(), idMateria)
                .stream()
                .map(AutoevaluacionTrimestralResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConsolidadoTrimestralEstudianteResponse> obtenerConsolidadoEstudiante(UUID idGestionAcademica,
            Integer trimestre, UUID idEstudiante) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        validarTrimestre(trimestre);
        GestionAcademica gestion = buscarGestion(idInstitucion, idGestionAcademica);
        PeriodoTrimestral periodo = obtenerPeriodo(idInstitucion, gestion.getId(), trimestre, false);
        Estudiante estudiante = buscarEstudiante(idInstitucion, idEstudiante);

        List<Inscripcion> inscripciones = inscripcionRepository
                .findAllByIdInstitucionAndIdEstudiante(idInstitucion, estudiante.getId())
                .stream()
                .filter(inscripcion -> gestion.getId().equals(inscripcion.getIdGestion()))
                .filter(inscripcion -> ESTADO_ACTIVO.equals(inscripcion.getEstado()))
                .toList();

        List<ConsolidadoTrimestralEstudianteResponse> respuestas = new ArrayList<>();
        for (Inscripcion inscripcion : inscripciones) {
            respuestas.add(construirConsolidadoEstudianteRow(idInstitucion, gestion, periodo, estudiante, inscripcion));
        }
        return respuestas;
    }

    @Transactional(readOnly = true)
    public List<ConsolidadoTrimestralMateriaResponse> obtenerConsolidadoDocente(UUID idGestionAcademica,
            Integer trimestre) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        validarTrimestre(trimestre);
        GestionAcademica gestion = buscarGestion(idInstitucion, idGestionAcademica);
        PeriodoTrimestral periodo = obtenerPeriodo(idInstitucion, gestion.getId(), trimestre, false);

        Docente docente = resolverDocenteActual(idInstitucion);
        List<AsignacionDocente> asignaciones = asignacionDocenteRepository
                .findAllByIdInstitucionAndIdDocente(idInstitucion,
                        docente.getId())
                .stream()
                .filter(asignacion -> gestion.getId().equals(asignacion.getIdGestion()))
                .filter(asignacion -> ESTADO_ACTIVO.equals(asignacion.getEstado()))
                .toList();

        return asignaciones.stream()
                .map(asignacion -> construirConsolidadoMateriaRow(idInstitucion, gestion, periodo, asignacion))
                .toList();
    }

    @Transactional(readOnly = true)
    public ConsolidadoTrimestralDirectorResponse obtenerConsolidadoDirector(UUID idGestionAcademica,
            Integer trimestre) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        validarTrimestre(trimestre);
        GestionAcademica gestion = buscarGestion(idInstitucion, idGestionAcademica);
        PeriodoTrimestral periodo = obtenerPeriodo(idInstitucion, gestion.getId(), trimestre, false);

        List<AsignacionDocente> asignaciones = asignacionDocenteRepository
                .findAllByIdInstitucionAndIdGestion(idInstitucion,
                        gestion.getId())
                .stream()
                .filter(asignacion -> ESTADO_ACTIVO.equals(asignacion.getEstado()))
                .toList();

        List<ConsolidadoTrimestralMateriaResponse> materias = asignaciones.stream()
                .map(asignacion -> construirConsolidadoMateriaRow(idInstitucion, gestion, periodo, asignacion))
                .toList();

        int materiasCompletas = (int) materias.stream()
                .filter(m -> m.estudiantes().stream().noneMatch(r -> "PENDIENTE".equals(r.estado()))).count();
        int materiasConPendientes = materias.size() - materiasCompletas;
        int estudiantesSinAutoevaluacion = materias.stream()
                .mapToInt(ConsolidadoTrimestralMateriaResponse::estudiantesPendientesAutoevaluacion).sum();
        int docentesConSerPendiente = (int) materias.stream().filter(
                m -> m.estudiantes().stream().anyMatch(r -> r.ser() == null || r.ser().compareTo(BigDecimal.ZERO) == 0))
                .count();
        int estudiantesEnRiesgo = (int) materias.stream().flatMap(m -> m.estudiantes().stream())
                .filter(r -> "EN_RIESGO".equals(r.estado())).count();
        BigDecimal promedioGeneral = materias.stream().flatMap(m -> m.estudiantes().stream())
                .map(ConsolidadoTrimestralEstudianteResponse::totalFinal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int cantidad = materias.stream().mapToInt(m -> m.estudiantes().size()).sum();
        if (cantidad > 0) {
            promedioGeneral = promedioGeneral.divide(BigDecimal.valueOf(cantidad), 2, RoundingMode.HALF_UP);
        }

        return new ConsolidadoTrimestralDirectorResponse(materias.size(), materiasCompletas, materiasConPendientes,
                estudiantesSinAutoevaluacion, docentesConSerPendiente, estudiantesEnRiesgo, promedioGeneral, materias);
    }

    @Transactional
    public PeriodoTrimestral cerrarTrimestre(UUID idGestionAcademica, Integer trimestre, String justificacion) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        validarTrimestre(trimestre);
        GestionAcademica gestion = buscarGestion(idInstitucion, idGestionAcademica);
        PeriodoTrimestral periodo = obtenerPeriodo(idInstitucion, gestion.getId(), trimestre, true);
        Map<String, Object> antes = snapshotPeriodo(periodo);
        periodo.setEstado(ESTADO_CERRADO);
        periodo.setFechaCierre(Instant.now());
        periodo.setJustificacionCierre(justificacion);
        periodo.setIdUsuarioCierre(SecurityUtils.currentUserId());
        PeriodoTrimestral saved = periodoRepository.save(periodo);
        auditoriaService.registrarDetallado(idInstitucion, SecurityUtils.currentUserId(), MODULO, "CERRAR_TRIMESTRE",
                "periodo_trimestral", saved.getId().toString(), antes, snapshotPeriodo(saved), true,
                "Trimestre cerrado");
        return saved;
    }

    @Transactional
    public PeriodoTrimestral reabrirTrimestre(UUID idGestionAcademica, Integer trimestre, String justificacion) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        validarTrimestre(trimestre);
        GestionAcademica gestion = buscarGestion(idInstitucion, idGestionAcademica);
        PeriodoTrimestral periodo = obtenerPeriodo(idInstitucion, gestion.getId(), trimestre, true);
        Map<String, Object> antes = snapshotPeriodo(periodo);
        periodo.setEstado(ESTADO_ABIERTO);
        periodo.setFechaReapertura(Instant.now());
        periodo.setJustificacionReapertura(justificacion);
        periodo.setIdUsuarioReapertura(SecurityUtils.currentUserId());
        PeriodoTrimestral saved = periodoRepository.save(periodo);
        auditoriaService.registrarDetallado(idInstitucion, SecurityUtils.currentUserId(), MODULO, "REABRIR_TRIMESTRE",
                "periodo_trimestral", saved.getId().toString(), antes, snapshotPeriodo(saved), true,
                "Trimestre reabierto");
        return saved;
    }

    @Transactional(readOnly = true)
    public List<BitacoraAuditoriaResponse> listarBitacora(String modulo, String tipoOperacion, Boolean exito) {
        BitacoraAuditoriaFiltro filtro = new BitacoraAuditoriaFiltro();
        filtro.setModulo(modulo);
        filtro.setTipoOperacion(tipoOperacion);
        filtro.setExito(exito);
        return auditoriaQueryService.listar(filtro);
    }

    private ConsolidadoTrimestralEstudianteResponse construirConsolidadoEstudianteRow(UUID idInstitucion,
            GestionAcademica gestion, PeriodoTrimestral periodo, Estudiante estudiante, Inscripcion inscripcion) {
        AsignacionDocente asignacion = asignacionDocenteRepository.findAllByIdInstitucionAndIdParalelo(idInstitucion,
                inscripcion.getIdParalelo()).stream()
                .filter(a -> gestion.getId().equals(a.getIdGestion()))
                .filter(a -> ESTADO_ACTIVO.equals(a.getEstado()))
                .findFirst()
                .orElse(null);

        if (asignacion == null) {
            return new ConsolidadoTrimestralEstudianteResponse(estudiante.getId(), estudiante.getCodigoEstudiante(),
                    estudiante.getNombres() + " " + estudiante.getApellidos(), BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "PENDIENTE",
                    "Sin asignacion activa");
        }

        return construirRowDesdeAsignacion(idInstitucion, gestion, periodo, asignacion, estudiante);
    }

    private ConsolidadoTrimestralMateriaResponse construirConsolidadoMateriaRow(UUID idInstitucion,
            GestionAcademica gestion, PeriodoTrimestral periodo, AsignacionDocente asignacion) {
        Materia materia = buscarMateria(idInstitucion, asignacion.getIdMateria());
        Docente docente = buscarDocente(idInstitucion, asignacion.getIdDocente());
        List<Inscripcion> inscripciones = obtenerInscripcionesActivas(gestion.getId(), asignacion.getIdParalelo(),
                idInstitucion);
        List<ConsolidadoTrimestralEstudianteResponse> estudiantes = inscripciones.stream()
                .map(Inscripcion::getIdEstudiante)
                .map(idEstudiante -> buscarEstudiante(idInstitucion, idEstudiante))
                .map(estudiante -> construirRowDesdeAsignacion(idInstitucion, gestion, periodo, asignacion, estudiante))
                .toList();
        int pendientesAutoevaluacion = (int) estudiantes.stream()
                .filter(r -> r.autoevaluacion() == null || r.autoevaluacion().compareTo(BigDecimal.ZERO) == 0).count();
        BigDecimal promedioGeneral = estudiantes.stream().map(ConsolidadoTrimestralEstudianteResponse::totalFinal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (!estudiantes.isEmpty()) {
            promedioGeneral = promedioGeneral.divide(BigDecimal.valueOf(estudiantes.size()), 2, RoundingMode.HALF_UP);
        }

        return new ConsolidadoTrimestralMateriaResponse(materia.getId(), materia.getCodigo(), materia.getNombre(),
                docente.getId(), docente.getNombres() + " " + docente.getApellidos(),
                contarActividades(asignacion, periodo, DIMENSION_SABER),
                contarActividades(asignacion, periodo, DIMENSION_HACER), pendientesAutoevaluacion, promedioGeneral,
                periodo.getEstado(), estudiantes);
    }

    private ConsolidadoTrimestralEstudianteResponse construirRowDesdeAsignacion(UUID idInstitucion,
            GestionAcademica gestion, PeriodoTrimestral periodo, AsignacionDocente asignacion, Estudiante estudiante) {
        List<ActividadEvaluativa> actividades = actividadRepository
                .findAllByIdInstitucionAndIdPeriodoTrimestralAndIdMateriaAndIdCursoAndIdParalelo(idInstitucion,
                        periodo.getId(), asignacion.getIdMateria(), idCursoDeAsignacion(asignacion), asignacion.getIdParalelo())
                .stream()
                .filter(actividad -> ESTADO_ACTIVO.equals(actividad.getEstado())
                        || ESTADO_BORRADOR.equals(actividad.getEstado()))
                .toList();

        CalificacionSer ser = calificacionSerRepository
                .findByIdInstitucionAndIdGestionAcademicaAndIdTrimestreAndIdMateriaAndIdEstudiante(idInstitucion,
                        gestion.getId(), periodo.getId(), asignacion.getIdMateria(), estudiante.getId())
                .orElse(null);
        AutoevaluacionTrimestral autoevaluacion = autoevaluacionRepository
                .findByIdInstitucionAndIdGestionAcademicaAndIdTrimestreAndIdMateriaAndIdEstudiante(idInstitucion,
                        gestion.getId(), periodo.getId(), asignacion.getIdMateria(), estudiante.getId())
                .orElse(null);

        BigDecimal saber = calcularContribucion(actividades, estudiante.getId(), DIMENSION_SABER, PESO_SABER);
        BigDecimal hacer = calcularContribucion(actividades, estudiante.getId(), DIMENSION_HACER, PESO_HACER);
        BigDecimal valorSer = ser != null ? normalizarSer(ser.getNotaSer()) : BigDecimal.ZERO;
        BigDecimal valorAuto = autoevaluacion != null ? normalizarAutoevaluacion(autoevaluacion.getNotaAutoevaluacion())
                : BigDecimal.ZERO;
        BigDecimal total = valorSer.add(saber).add(hacer).add(valorAuto).setScale(2, RoundingMode.HALF_UP);

        boolean pendientes = ser == null || autoevaluacion == null
                || actividades.stream()
                        .anyMatch(actividad -> calificacionActividadRepository
                                .findByIdActividadAndIdEstudiante(actividad.getId(), estudiante.getId())
                                .map(CalificacionActividad::getNotaObtenida).isEmpty());
        String estado = pendientes ? "PENDIENTE" : (total.compareTo(NOTA_APROBACION) >= 0 ? "APROBADO" : "EN_RIESGO");
        String observacion = pendientes ? "Faltan registros para consolidar" : "Consolidado completo";

        return new ConsolidadoTrimestralEstudianteResponse(estudiante.getId(), estudiante.getCodigoEstudiante(),
                estudiante.getNombres() + " " + estudiante.getApellidos(), valorSer, saber, hacer, valorAuto,
                total, total, estado, observacion);
    }

    private BigDecimal calcularContribucion(List<ActividadEvaluativa> actividades, UUID idEstudiante, String dimension,
            BigDecimal pesoDimension) {
        List<ActividadEvaluativa> filtradas = actividades.stream()
                .filter(actividad -> dimension.equals(actividad.getDimension()))
                .toList();
        if (filtradas.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal sumaPorcentajes = BigDecimal.ZERO;
        int conteo = 0;
        for (ActividadEvaluativa actividad : filtradas) {
            Optional<CalificacionActividad> calificacion = calificacionActividadRepository
                    .findByIdActividadAndIdEstudiante(actividad.getId(), idEstudiante);
            if (calificacion.isEmpty() || calificacion.get().getNotaObtenida() == null
                    || actividad.getPuntajeMaximo() == null || actividad.getPuntajeMaximo() == 0) {
                continue;
            }
            BigDecimal porcentaje = calificacion.get().getNotaObtenida()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(actividad.getPuntajeMaximo()), 4, RoundingMode.HALF_UP);
            sumaPorcentajes = sumaPorcentajes.add(porcentaje);
            conteo++;
        }

        if (conteo == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal promedioPorcentaje = sumaPorcentajes.divide(BigDecimal.valueOf(conteo), 4, RoundingMode.HALF_UP);
        return promedioPorcentaje.multiply(pesoDimension)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizarSer(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : valor.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizarAutoevaluacion(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : valor.setScale(2, RoundingMode.HALF_UP);
    }

    private void validarTrimestre(Integer trimestre) {
        if (trimestre == null || trimestre < 1 || trimestre > 3) {
            throw new IllegalArgumentException("El trimestre debe estar entre 1 y 3");
        }
    }

    private GestionAcademica buscarGestion(UUID idInstitucion, UUID idGestion) {
        return gestionAcademicaRepository.findByIdAndIdInstitucion(idGestion, idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Gestion academica no encontrada"));
    }

    private PeriodoTrimestral buscarPeriodo(UUID idInstitucion, UUID idPeriodo) {
        return periodoRepository.findByIdAndIdInstitucion(idPeriodo, idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Periodo trimestral no encontrado"));
    }

    private PeriodoTrimestral obtenerPeriodo(UUID idInstitucion, UUID idGestion, Integer trimestre,
            boolean crearSiNoExiste) {
        validarTrimestre(trimestre);
        return periodoRepository
                .findByIdInstitucionAndIdGestionAcademicaAndNumeroTrimestre(idInstitucion, idGestion, trimestre)
                .orElseGet(() -> {
                    if (!crearSiNoExiste) {
                        throw new EntityNotFoundException("Periodo trimestral no encontrado");
                    }
                    return periodoRepository.save(PeriodoTrimestral.builder()
                            .idInstitucion(idInstitucion)
                            .idGestionAcademica(idGestion)
                            .numeroTrimestre(trimestre)
                            .estado(ESTADO_ABIERTO)
                            .build());
                });
    }

    private ActividadEvaluativa buscarActividad(UUID idInstitucion, UUID idActividad) {
        return actividadRepository.findByIdAndIdInstitucion(idActividad, idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Actividad evaluativa no encontrada"));
    }

    private Docente resolverDocenteActual(UUID idInstitucion) {
        return docenteRepository.findByIdUsuarioAndIdInstitucion(SecurityUtils.currentUserId(), idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("No existe un docente asociado al usuario autenticado"));
    }

    private Estudiante buscarEstudiante(UUID idInstitucion, UUID idEstudiante) {
        return estudianteRepository.findByIdAndIdInstitucion(idEstudiante, idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Estudiante no encontrado"));
    }

    private Materia buscarMateria(UUID idInstitucion, UUID idMateria) {
        return materiaRepository.findByIdAndIdInstitucion(idMateria, idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Materia no encontrada"));
    }

    private Curso buscarCurso(UUID idInstitucion, UUID idCurso) {
        return cursoRepository.findByIdAndIdInstitucion(idCurso, idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Curso no encontrado"));
    }

    private Paralelo buscarParalelo(UUID idInstitucion, UUID idParalelo) {
        return paraleloRepository.findByIdAndIdInstitucion(idParalelo, idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Paralelo no encontrado"));
    }

    private Docente buscarDocente(UUID idInstitucion, UUID idDocente) {
        return docenteRepository.findByIdAndIdInstitucion(idDocente, idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Docente no encontrado"));
    }

    private void validarDocente(UUID idDocente, UUID idInstitucion) {
        buscarDocente(idInstitucion, idDocente);
    }

    private void validarAccesoDocenteMateria(UUID idDocente, UUID idMateria, UUID idParalelo, UUID idGestion,
            UUID idInstitucion) {
        if (SecurityUtils.currentUserHasRole("SUPER_ADMIN") || SecurityUtils.currentUserHasRole("ADMIN_INSTITUCION")
                || SecurityUtils.currentUserHasRole("DIRECTOR")) {
            return;
        }
        if (!SecurityUtils.currentUserHasRole("DOCENTE")) {
            throw new AccessDeniedException("No tiene permisos para operar esta materia");
        }
        Docente docenteActual = resolverDocenteActual(idInstitucion);
        if (!Objects.equals(docenteActual.getId(), idDocente)) {
            throw new AccessDeniedException("Solo puede operar sus propias asignaciones");
        }
        boolean asignacionValida = asignacionDocenteRepository
                .existsByIdInstitucionAndIdDocenteAndIdMateriaAndIdParaleloAndIdGestion(
                        idInstitucion, idDocente, idMateria, idParalelo, idGestion);
        if (!asignacionValida) {
            throw new AccessDeniedException("La asignacion docente no corresponde a la materia y paralelo indicados");
        }
    }

    private void validarAccesoAutoevaluacion(UUID idEstudiante) {
        if (SecurityUtils.currentUserHasRole("SUPER_ADMIN") || SecurityUtils.currentUserHasRole("ADMIN_INSTITUCION")
                || SecurityUtils.currentUserHasRole("DIRECTOR")) {
            return;
        }
        if (!SecurityUtils.currentUserHasRole("ESTUDIANTE")) {
            throw new AccessDeniedException("No tiene permisos para registrar autoevaluacion");
        }
        Estudiante estudianteActual = estudianteRepository
                .findAllByIdInstitucion(SecurityUtils.requireCurrentInstitutionId())
                .stream()
                .filter(estudiante -> Objects.equals(estudiante.getIdUsuario(), SecurityUtils.currentUserId()))
                .findFirst()
                .orElseThrow(
                        () -> new AccessDeniedException("No existe un estudiante asociado al usuario autenticado"));
        if (!Objects.equals(estudianteActual.getId(), idEstudiante)) {
            throw new AccessDeniedException("Solo puede registrar su propia autoevaluacion");
        }
    }

    private void validarPeriodoAbierto(PeriodoTrimestral periodo) {
        if (!ESTADO_ABIERTO.equals(periodo.getEstado())) {
            throw new IllegalStateException("El trimestre esta cerrado");
        }
    }

    private List<Inscripcion> obtenerInscripcionesActivas(UUID idGestion, UUID idParalelo, UUID idInstitucion) {
        return inscripcionRepository.findAllByIdInstitucionAndIdParalelo(idInstitucion, idParalelo).stream()
                .filter(inscripcion -> idGestion.equals(inscripcion.getIdGestion()))
                .filter(inscripcion -> ESTADO_ACTIVO.equals(inscripcion.getEstado()))
                .toList();
    }

    private List<Inscripcion> obtenerInscripcionesActivas(UUID idGestion, UUID idParalelo) {
        return obtenerInscripcionesActivas(idGestion, idParalelo, SecurityUtils.requireCurrentInstitutionId());
    }

    private void validarRangoNota(BigDecimal notaObtenida, Integer puntajeMaximo) {
        if (notaObtenida == null) {
            return;
        }
        if (notaObtenida.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("La nota no puede ser negativa");
        }
        if (puntajeMaximo != null && notaObtenida.compareTo(BigDecimal.valueOf(puntajeMaximo)) > 0) {
            throw new IllegalArgumentException("La nota no puede superar el puntaje maximo de la actividad");
        }
    }

    private void validarRangoDirecto(BigDecimal nota, BigDecimal maximo) {
        if (nota == null) {
            throw new IllegalArgumentException("La nota es obligatoria");
        }
        if (nota.compareTo(BigDecimal.ZERO) < 0 || nota.compareTo(maximo) > 0) {
            throw new IllegalArgumentException("La nota esta fuera de rango");
        }
    }

    private String normalizarDimension(String dimension) {
        return normalizarTexto(dimension, "La dimension es obligatoria").toUpperCase();
    }

    private String normalizarEstadoActividad(String estado) {
        if (estado == null || estado.isBlank()) {
            return ESTADO_BORRADOR;
        }
        String normalizado = estado.trim().toUpperCase();
        if (!ESTADO_BORRADOR.equals(normalizado) && !ESTADO_ACTIVO.equals(normalizado)
                && !ESTADO_CERRADO.equals(normalizado) && !"CERRADA".equals(normalizado)
                && !"PUBLICADA".equals(normalizado)) {
            throw new IllegalArgumentException("Estado de actividad invalido");
        }
        if (ESTADO_CERRADO.equals(normalizado)) {
            return "CERRADA";
        }
        return "PUBLICADA".equals(normalizado) ? ESTADO_ACTIVO : normalizado;
    }

    private Instant resolverFechaActividad(String fechaActividad, Instant fallback) {
        if (fechaActividad == null || fechaActividad.isBlank()) {
            return fallback;
        }

        String valor = fechaActividad.trim();
        try {
            return Instant.parse(valor);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(valor).atStartOfDay(ZoneOffset.UTC).toInstant();
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("La fecha de actividad debe tener formato ISO o yyyy-MM-dd");
            }
        }
    }

    private String normalizarTexto(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensaje);
        }
        return valor.trim();
    }

    private Map<String, Object> snapshotActividad(ActividadEvaluativa actividad) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("idPeriodoTrimestral", actividad.getIdPeriodoTrimestral());
        snapshot.put("idGestionAcademica", actividad.getIdGestionAcademica());
        snapshot.put("idCurso", actividad.getIdCurso());
        snapshot.put("idParalelo", actividad.getIdParalelo());
        snapshot.put("idMateria", actividad.getIdMateria());
        snapshot.put("idDocente", actividad.getIdDocente());
        snapshot.put("nombreActividad", actividad.getNombreActividad());
        snapshot.put("tipoActividad", actividad.getTipoActividad());
        snapshot.put("dimension", actividad.getDimension());
        snapshot.put("puntajeMaximo", actividad.getPuntajeMaximo());
        snapshot.put("fechaActividad", actividad.getFechaActividad());
        snapshot.put("descripcion", actividad.getDescripcion());
        snapshot.put("estado", actividad.getEstado());
        snapshot.put("publicadoEn", actividad.getPublicadoEn());
        return snapshot;
    }

    private Map<String, Object> snapshotPeriodo(PeriodoTrimestral periodo) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("idGestionAcademica", periodo.getIdGestionAcademica());
        snapshot.put("numeroTrimestre", periodo.getNumeroTrimestre());
        snapshot.put("estado", periodo.getEstado());
        snapshot.put("fechaCierre", periodo.getFechaCierre());
        snapshot.put("justificacionCierre", periodo.getJustificacionCierre());
        snapshot.put("fechaReapertura", periodo.getFechaReapertura());
        snapshot.put("justificacionReapertura", periodo.getJustificacionReapertura());
        return snapshot;
    }

    private int contarActividades(AsignacionDocente asignacion, PeriodoTrimestral periodo, String dimension) {
        return (int) actividadRepository
                .findAllByIdInstitucionAndIdPeriodoTrimestralAndIdMateriaAndIdCursoAndIdParalelo(
                        SecurityUtils.requireCurrentInstitutionId(), periodo.getId(), asignacion.getIdMateria(),
                        idCursoDeAsignacion(asignacion), asignacion.getIdParalelo())
                .stream()
                .filter(actividad -> dimension.equals(actividad.getDimension()))
                .count();
    }

    private UUID idCursoDeAsignacion(AsignacionDocente asignacion) {
        return paraleloRepository.findByIdAndIdInstitucion(asignacion.getIdParalelo(), asignacion.getIdInstitucion())
                .map(Paralelo::getIdCurso)
                .orElseThrow(() -> new EntityNotFoundException("Paralelo no encontrado: " + asignacion.getIdParalelo()));
    }
}
