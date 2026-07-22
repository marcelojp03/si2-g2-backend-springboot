package com.uagrm.si2g2.alertas.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uagrm.si2g2.academico.domain.GestionAcademicaRepository;
import com.uagrm.si2g2.alertas.domain.*;
import com.uagrm.si2g2.alertas.dto.*;
import com.uagrm.si2g2.asignacion.domain.AsignacionDocente;
import com.uagrm.si2g2.asignacion.domain.AsignacionDocenteRepository;
import com.uagrm.si2g2.asistencia.domain.*;
import com.uagrm.si2g2.calificacion.domain.*;
import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.curso.domain.*;
import com.uagrm.si2g2.docente.domain.Docente;
import com.uagrm.si2g2.docente.domain.DocenteRepository;
import com.uagrm.si2g2.dimension.domain.PeriodoDimension;
import com.uagrm.si2g2.dimension.domain.PeriodoDimensionRepository;
import com.uagrm.si2g2.estudiante.domain.*;
import com.uagrm.si2g2.inscripcion.domain.*;
import com.uagrm.si2g2.institucion.application.ConfiguracionService;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RiesgoAcademicoService {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private final InscripcionRepository inscripcionRepository;
    private final EstudianteRepository estudianteRepository;
    private final ParaleloRepository paraleloRepository;
    private final CursoRepository cursoRepository;
    private final MateriaRepository materiaRepository;
    private final GestionAcademicaRepository gestionRepository;
    private final AsignacionDocenteRepository asignacionRepository;
    private final DocenteRepository docenteRepository;
    private final AsistenciaRegistroRepository asistenciaRegistroRepository;
    private final AsistenciaDetalleRepository asistenciaDetalleRepository;
    private final EvaluacionRepository evaluacionRepository;
    private final PeriodoEvaluacionRepository periodoEvaluacionRepository;
    private final CalificacionRepository calificacionRepository;
    private final ActividadEvaluativaRepository actividadEvaluativaRepository;
    private final CalificacionActividadRepository calificacionActividadRepository;
    private final CalificacionSerRepository calificacionSerRepository;
    private final AutoevaluacionTrimestralRepository autoevaluacionRepository;
    private final PeriodoDimensionRepository periodoDimensionRepository;
    private final AlertaRiesgoRepository alertaRepository;
    private final RecomendacionIaRepository recomendacionRepository;
    private final AlertaRiesgoSeguimientoRepository seguimientoRepository;
    private final ConfiguracionService configuracionService;
    private final ObjectMapper objectMapper;

    @Transactional
    public AnalisisRiesgoResponse analizarParalelo(UUID idParalelo, UUID idGestion) {
        return analizarParalelo(idParalelo, idGestion, null, null);
    }

    @Transactional
    public AnalisisRiesgoResponse analizarParalelo(UUID idParalelo, UUID idGestion, UUID idPeriodo) {
        return analizarParalelo(idParalelo, idGestion, idPeriodo, null);
    }

    @Transactional
    public AnalisisRiesgoResponse analizarParalelo(
            UUID idParalelo, UUID idGestion, UUID idPeriodo, UUID idMateria) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        Paralelo paralelo = validarParaleloGestion(idInstitucion, idParalelo, idGestion);
        validarPeriodoGestion(idInstitucion, idGestion, idPeriodo);
        validarAccesoDocente(paralelo.getId(), idGestion);
        validarMateriaParalelo(idInstitucion, idGestion, paralelo.getId(), idMateria);
        boolean analisisFiltrado = idPeriodo != null || idMateria != null;
        List<RiesgoEstudianteDetalleResponse> estudiantes = analizarParaleloInterno(
                idInstitucion, paralelo, idGestion,
                !analisisFiltrado, idPeriodo, idMateria);
        if (analisisFiltrado) {
            analizarParaleloInterno(idInstitucion, paralelo, idGestion, true, null, null);
        }
        return response(estudiantes, List.of(resumenParalelo(paralelo, estudiantes)));
    }

    @Transactional
    public AnalisisRiesgoResponse analizarInstitucion(UUID idGestion) {
        return analizarInstitucion(idGestion, null, null, null);
    }

    @Transactional
    public AnalisisRiesgoResponse analizarInstitucion(UUID idGestion, UUID idCurso, UUID idPeriodo) {
        return analizarInstitucion(idGestion, idCurso, idPeriodo, null);
    }

    @Transactional
    public AnalisisRiesgoResponse analizarInstitucion(
            UUID idGestion, UUID idCurso, UUID idPeriodo, UUID idMateria) {
        return analizarInstitucion(
                SecurityUtils.requireCurrentInstitutionId(), idGestion, idCurso, idPeriodo, idMateria,
                idPeriodo == null && idMateria == null, idPeriodo != null || idMateria != null);
    }

    @Transactional(readOnly = true)
    public AnalisisRiesgoResponse resumenInstitucion(UUID idGestion) {
        return analizarInstitucion(
                SecurityUtils.requireCurrentInstitutionId(), idGestion, null, null, null, false, false);
    }

    /** Procesamiento interno de un paralelo; la transaccion la define el coordinador scheduler. */
    public void procesarParalelo(UUID idInstitucion, UUID idParalelo, UUID idGestion) {
        Paralelo paralelo = validarParaleloGestion(idInstitucion, idParalelo, idGestion);
        analizarParaleloInterno(idInstitucion, paralelo, idGestion, true, null, null);
    }

    private AnalisisRiesgoResponse analizarInstitucion(UUID idInstitucion, UUID idGestion,
                                                       UUID idCurso, UUID idPeriodo,
                                                       UUID idMateria, boolean persistir,
                                                       boolean refrescarAlertas) {
        validarGestion(idInstitucion, idGestion);
        validarPeriodoGestion(idInstitucion, idGestion, idPeriodo);
        if (idCurso != null) {
            cursoRepository.findByIdAndIdInstitucion(idCurso, idInstitucion)
                    .orElseThrow(() -> new EntityNotFoundException("Curso no encontrado"));
        }
        if (idMateria != null) {
            materiaRepository.findByIdAndIdInstitucion(idMateria, idInstitucion)
                    .orElseThrow(() -> new EntityNotFoundException("Materia no encontrada"));
        }
        Set<UUID> paralelosPermitidos = tieneAccesoInstitucional()
                ? null
                : new HashSet<>(paralelosDisponibles(idGestion));
        List<RiesgoEstudianteDetalleResponse> estudiantes = new ArrayList<>();
        List<ResumenRiesgoParalelo> comparativa = new ArrayList<>();
        paraleloRepository.findAllByIdInstitucion(idInstitucion).stream()
                .filter(p -> idGestion.equals(p.getIdGestionAcademica()))
                .filter(p -> idCurso == null || idCurso.equals(p.getIdCurso()))
                .filter(p -> paralelosPermitidos == null || paralelosPermitidos.contains(p.getId()))
                .filter(p -> idMateria == null || asignacionRepository
                        .findAllByIdInstitucionAndIdParalelo(idInstitucion, p.getId()).stream()
                        .anyMatch(a -> idGestion.equals(a.getIdGestion())
                                && idMateria.equals(a.getIdMateria()) && "ACTIVA".equals(a.getEstado())))
                .filter(p -> "ACTIVO".equals(p.getEstado()))
                .forEach(paralelo -> {
                    List<RiesgoEstudianteDetalleResponse> resultado = analizarParaleloInterno(
                            idInstitucion, paralelo, idGestion, persistir, idPeriodo, idMateria);
                    if (refrescarAlertas) {
                        analizarParaleloInterno(idInstitucion, paralelo, idGestion, true, null, null);
                    }
                    estudiantes.addAll(resultado);
                    comparativa.add(resumenParalelo(paralelo, resultado));
                });
        return response(estudiantes, comparativa);
    }

    @Transactional(readOnly = true)
    public RiesgoEstudianteDetalleResponse detalleEstudiante(UUID idEstudiante, UUID idGestion) {
        return detalleEstudiante(idEstudiante, idGestion, null, null);
    }

    @Transactional(readOnly = true)
    public RiesgoEstudianteDetalleResponse detalleEstudiante(
            UUID idEstudiante, UUID idGestion, UUID idPeriodo) {
        return detalleEstudiante(idEstudiante, idGestion, idPeriodo, null);
    }

    @Transactional(readOnly = true)
    public RiesgoEstudianteDetalleResponse detalleEstudiante(
            UUID idEstudiante, UUID idGestion, UUID idPeriodo, UUID idMateria) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        validarGestion(idInstitucion, idGestion);
        validarPeriodoGestion(idInstitucion, idGestion, idPeriodo);
        Inscripcion inscripcion = inscripcionRepository.findAllByIdInstitucionAndIdEstudiante(idInstitucion, idEstudiante)
                .stream()
                .filter(i -> idGestion.equals(i.getIdGestion()))
                .filter(i -> "ACTIVA".equals(i.getEstado()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("El estudiante no tiene una inscripcion activa en la gestion"));
        validarAccesoDocente(inscripcion.getIdParalelo(), idGestion);
        validarMateriaParalelo(idInstitucion, idGestion, inscripcion.getIdParalelo(), idMateria);
        return analizarInscripcion(idInstitucion, inscripcion, false, idPeriodo, idMateria);
    }

    @Transactional(readOnly = true)
    public List<UUID> paralelosDisponibles(UUID idGestion) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        validarGestion(idInstitucion, idGestion);
        if (tieneAccesoInstitucional()) {
            return paraleloRepository.findAllByIdInstitucion(idInstitucion).stream()
                    .filter(paralelo -> idGestion.equals(paralelo.getIdGestionAcademica()))
                    .filter(paralelo -> "ACTIVO".equals(paralelo.getEstado()))
                    .map(Paralelo::getId).toList();
        }
        if (!SecurityUtils.currentUserHasRole("DOCENTE")) {
            throw new AccessDeniedException("No tienes acceso a los paralelos de riesgo academico");
        }
        Docente docente = docenteRepository
                .findByIdUsuarioAndIdInstitucion(SecurityUtils.currentUserId(), idInstitucion)
                .orElseThrow(() -> new AccessDeniedException("El usuario autenticado no tiene docente asociado"));
        return asignacionRepository.findAllByIdInstitucionAndIdDocente(idInstitucion, docente.getId()).stream()
                .filter(asignacion -> idGestion.equals(asignacion.getIdGestion()))
                .filter(asignacion -> "ACTIVA".equals(asignacion.getEstado()))
                .map(AsignacionDocente::getIdParalelo).distinct().toList();
    }

    private Paralelo validarParaleloGestion(UUID idInstitucion, UUID idParalelo, UUID idGestion) {
        validarGestion(idInstitucion, idGestion);
        Paralelo paralelo = paraleloRepository.findByIdAndIdInstitucion(idParalelo, idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Paralelo no encontrado"));
        if (!idGestion.equals(paralelo.getIdGestionAcademica())) {
            throw new EntityNotFoundException("Paralelo no encontrado en la gestion indicada");
        }
        return paralelo;
    }

    private void validarGestion(UUID idInstitucion, UUID idGestion) {
        gestionRepository.findByIdAndIdInstitucion(idGestion, idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Gestion academica no encontrada"));
    }

    private void validarPeriodoGestion(UUID idInstitucion, UUID idGestion, UUID idPeriodo) {
        if (idPeriodo == null) return;
        PeriodoEvaluacion periodo = periodoEvaluacionRepository
                .findByIdAndIdInstitucion(idPeriodo, idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Periodo de evaluacion no encontrado"));
        if (!idGestion.equals(periodo.getIdGestionAcademica())) {
            throw new IllegalArgumentException("El periodo no pertenece a la gestion seleccionada");
        }
    }

    private void validarMateriaParalelo(
            UUID idInstitucion, UUID idGestion, UUID idParalelo, UUID idMateria) {
        if (idMateria == null) return;
        materiaRepository.findByIdAndIdInstitucion(idMateria, idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Materia no encontrada"));
        boolean asignada = asignacionRepository.findAllByIdInstitucionAndIdParalelo(idInstitucion, idParalelo)
                .stream().anyMatch(a -> idGestion.equals(a.getIdGestion())
                        && idMateria.equals(a.getIdMateria()) && "ACTIVA".equals(a.getEstado()));
        if (!asignada) {
            throw new IllegalArgumentException("La materia no esta asignada al paralelo seleccionado");
        }
    }

    private List<RiesgoEstudianteDetalleResponse> analizarParaleloInterno(
            UUID idInstitucion, Paralelo paralelo, UUID idGestion,
            boolean persistir, UUID idPeriodo, UUID idMateria) {
        return inscripcionRepository.findAllByIdInstitucionAndIdParalelo(idInstitucion, paralelo.getId()).stream()
                .filter(i -> idGestion.equals(i.getIdGestion()))
                .filter(i -> "ACTIVA".equals(i.getEstado()))
                .map(i -> analizarInscripcion(idInstitucion, i, persistir, idPeriodo, idMateria))
                .sorted(Comparator.comparing(RiesgoEstudianteDetalleResponse::score).reversed())
                .toList();
    }

    private RiesgoEstudianteDetalleResponse analizarInscripcion(
            UUID idInstitucion, Inscripcion inscripcion, boolean persistir,
            UUID idPeriodo, UUID idMateria) {
        Estudiante estudiante = (persistir
                ? estudianteRepository.findByIdAndIdInstitucionForUpdate(inscripcion.getIdEstudiante(), idInstitucion)
                : estudianteRepository.findByIdAndIdInstitucion(inscripcion.getIdEstudiante(), idInstitucion))
                .orElseThrow(() -> new EntityNotFoundException("Estudiante no encontrado"));
        Paralelo paralelo = paraleloRepository.findByIdAndIdInstitucion(inscripcion.getIdParalelo(), idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Paralelo no encontrado"));
        Curso curso = cursoRepository.findByIdAndIdInstitucion(paralelo.getIdCurso(), idInstitucion).orElse(null);
        UUID idDocenteVisible = idDocenteVisible(idInstitucion);
        List<AsignacionDocente> todasAsignaciones = asignacionRepository
                .findAllByIdInstitucionAndIdParalelo(idInstitucion, paralelo.getId()).stream()
                .filter(a -> inscripcion.getIdGestion().equals(a.getIdGestion()))
                .filter(a -> "ACTIVA".equals(a.getEstado()))
                .filter(a -> idDocenteVisible == null || idDocenteVisible.equals(a.getIdDocente()))
                .toList();
        List<AsignacionDocente> asignaciones = todasAsignaciones.stream()
                .filter(a -> idMateria == null || idMateria.equals(a.getIdMateria()))
                .toList();

        Metricas metricas = calcularMetricas(
                idInstitucion, inscripcion, asignaciones, todasAsignaciones, idPeriodo);
        boolean calculado = metricas.tieneAsistencia && metricas.tieneNotaNumerica;
        List<FactorContribuyente> factores = calculado ? construirFactores(idInstitucion, metricas) : List.of();
        BigDecimal score = factores.stream().map(f -> BigDecimal.valueOf(f.impacto()))
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        String nivel = calculado ? nivel(score) : "SIN_DATOS";
        List<String> recomendaciones = calculado ? recomendaciones(idInstitucion, metricas, nivel) : List.of();
        Optional<AlertaRiesgo> activa = persistir
                ? alertaRepository.findActivaForUpdate(idInstitucion, estudiante.getId(), inscripcion.getIdGestion())
                : alertaRepository.findByIdInstitucionAndIdEstudianteAndIdGestionAcademicaAndActivaTrue(
                        idInstitucion, estudiante.getId(), inscripcion.getIdGestion());

        AlertaRiesgo alerta = activa.orElse(null);
        if (persistir) {
            if (!calculado) {
                // La ausencia temporal de evidencia no invalida la ultima evaluacion calculable.
                if (alerta != null) {
                    alerta.setDatosVigentes(false);
                    alertaRepository.save(alerta);
                }
            } else if ("BAJO".equals(nivel)) {
                if (alerta != null) {
                    String estadoAnterior = alerta.getEstadoAlerta();
                    actualizarMetricasAlerta(alerta, nivel, score, metricas, factores);
                    alerta.setEstadoAlerta("CERRADA");
                    alerta.setActiva(false);
                    alerta = alertaRepository.save(alerta);
                    seguimientoRepository.save(AlertaRiesgoSeguimiento.builder()
                            .idAlertaRiesgo(alerta.getId()).idInstitucion(idInstitucion)
                            .estadoAnterior(estadoAnterior).estadoNuevo("CERRADA")
                            .observacion("Cierre automatico por riesgo bajo calculable").build());
                }
            } else {
                boolean nueva = alerta == null;
                if (nueva) {
                    alerta = AlertaRiesgo.builder()
                            .idInstitucion(idInstitucion)
                            .idEstudiante(estudiante.getId())
                            .idGestionAcademica(inscripcion.getIdGestion())
                            .estadoAlerta("ABIERTA")
                            .activa(true)
                            .build();
                }
                actualizarMetricasAlerta(alerta, nivel, score, metricas, factores);
                alerta = alertaRepository.save(alerta);
                recomendacionRepository.deleteAllByIdAlertaRiesgo(alerta.getId());
                if (!recomendaciones.isEmpty()) {
                    UUID alertaId = alerta.getId();
                    recomendacionRepository.saveAll(recomendaciones.stream()
                            .map(texto -> RecomendacionIa.builder().idAlertaRiesgo(alertaId)
                                    .descripcion(texto).tipoAccion("SEGUIMIENTO").build())
                            .toList());
                }
            }
        }

        BigDecimal scoreRespuesta = score;
        String nivelRespuesta = nivel;
        BigDecimal asistenciaRespuesta = metricas.porcentajeAsistencia;
        BigDecimal promedioRespuesta = metricas.promedio;
        String tendenciaRespuesta = metricas.tendencia;
        int pendientesRespuesta = metricas.evaluacionesPendientes;
        int materiasRespuesta = metricas.materiasBajoRendimiento;
        List<FactorContribuyente> factoresRespuesta = factores;
        if (!calculado && alerta != null) {
            scoreRespuesta = alerta.getScoreIa() == null ? BigDecimal.ZERO
                    : alerta.getScoreIa().multiply(HUNDRED).setScale(2, RoundingMode.HALF_UP);
            nivelRespuesta = alerta.getNivelRiesgo();
            asistenciaRespuesta = valorOZero(alerta.getPorcentajeAsistencia());
            promedioRespuesta = valorOZero(alerta.getPromedioCalificaciones());
            tendenciaRespuesta = alerta.getTendenciaNotas();
            pendientesRespuesta = alerta.getEvaluacionesPendientes() == null ? 0 : alerta.getEvaluacionesPendientes();
            materiasRespuesta = alerta.getMateriasReprobadasHistorial() == null
                    ? 0 : alerta.getMateriasReprobadasHistorial();
            factoresRespuesta = deserializarFactores(alerta.getFactoresJson());
        }
        return new RiesgoEstudianteDetalleResponse(
                alerta != null ? alerta.getId() : null,
                estudiante.getId(), estudiante.getCodigoEstudiante(), estudiante.getNombres(), estudiante.getApellidos(),
                curso != null ? curso.getNombre() : "Sin curso", paralelo.getNombre(), scoreRespuesta, nivelRespuesta,
                asistenciaRespuesta, promedioRespuesta, tendenciaRespuesta, pendientesRespuesta,
                materiasRespuesta, alerta != null ? alerta.getEstadoAlerta() : "SIN_ALERTA",
                 calculado ? "CALCULADO" : "DATOS_INSUFICIENTES", factoresRespuesta, recomendaciones,
                 metricas.evolucion, calculado ? metricas.desgloseMaterias : List.of());
    }

    private void actualizarMetricasAlerta(AlertaRiesgo alerta, String nivel, BigDecimal score,
                                           Metricas metricas, List<FactorContribuyente> factores) {
        alerta.setNivelRiesgo(nivel);
        alerta.setMotivo(factores.stream().filter(f -> f.impacto() > 0)
                .map(FactorContribuyente::descripcion).collect(Collectors.joining(". ")));
        alerta.setScoreIa(score.divide(HUNDRED, 4, RoundingMode.HALF_UP));
        alerta.setPorcentajeAsistencia(metricas.porcentajeAsistencia);
        alerta.setPromedioCalificaciones(metricas.promedio);
        alerta.setTendenciaNotas(metricas.tendencia);
        alerta.setEvaluacionesPendientes(metricas.evaluacionesPendientes);
        alerta.setMateriasReprobadasHistorial(metricas.materiasBajoRendimiento);
        alerta.setFactoresJson(serializarFactores(factores));
        Instant now = Instant.now();
        alerta.setProcesadoEn(now);
        alerta.setDatosVigentes(true);
        alerta.setUltimaEvaluacionValidaEn(now);
    }

    private Metricas calcularMetricas(UUID idInstitucion, Inscripcion inscripcion,
                                      List<AsignacionDocente> asignaciones,
                                      List<AsignacionDocente> asignacionesParalelo,
                                      UUID idPeriodo) {
        Map<Integer, PeriodoEvaluacion> periodos = periodoEvaluacionRepository
                .findAllByIdInstitucionAndIdGestionAcademica(idInstitucion, inscripcion.getIdGestion()).stream()
                .filter(periodo -> idPeriodo == null || idPeriodo.equals(periodo.getId()))
                .collect(Collectors.toMap(PeriodoEvaluacion::getNumeroPeriodo, periodo -> periodo));
        PeriodoEvaluacion periodoSeleccionado = idPeriodo == null || periodos.isEmpty()
                ? null
                : periodos.values().iterator().next();
        List<AsistenciaRegistro> registrosGestion = asignaciones.stream().map(AsignacionDocente::getId)
                .flatMap(id -> asistenciaRegistroRepository
                        .findAllByIdInstitucionAndIdAsignacionDocente(idInstitucion, id).stream())
                .filter(r -> !"ANULADA".equals(r.getEstado()))
                .filter(r -> !r.getFecha().isBefore(inscripcion.getFechaInscripcion()))
                .toList();
        if (registrosGestion.isEmpty() && asignaciones.size() < asignacionesParalelo.size()) {
            registrosGestion = asignacionesParalelo.stream().map(AsignacionDocente::getId)
                    .flatMap(id -> asistenciaRegistroRepository
                            .findAllByIdInstitucionAndIdAsignacionDocente(idInstitucion, id).stream())
                    .filter(r -> !"ANULADA".equals(r.getEstado()))
                    .filter(r -> !r.getFecha().isBefore(inscripcion.getFechaInscripcion()))
                    .toList();
        }
        List<AsistenciaRegistro> registros = periodoSeleccionado == null
                ? registrosGestion
                : registrosGestion.stream()
                        .filter(r -> !r.getFecha().isBefore(periodoSeleccionado.getFechaInicio())
                                && !r.getFecha().isAfter(periodoSeleccionado.getFechaFin()))
                        .toList();
        if (periodoSeleccionado != null && registros.isEmpty()) {
            // Si el periodo no tiene sesiones propias, usa la asistencia acumulada disponible
            // hasta su cierre para no descartar calificaciones validas del mismo periodo.
            registros = registrosGestion.stream()
                    .filter(r -> !r.getFecha().isAfter(periodoSeleccionado.getFechaFin()))
                    .toList();
        }
        List<AsistenciaDetalle> detalles = registros.isEmpty() ? List.of() : asistenciaDetalleRepository
                .findAllByIdAsistenciaRegistroIn(registros.stream().map(AsistenciaRegistro::getId).toList()).stream()
                .filter(d -> inscripcion.getId().equals(d.getIdInscripcion())).toList();
        Set<String> estadosAsistidos = Set.of("PRESENTE", "TARDANZA", "JUSTIFICADO");
        long asistidas = detalles.stream().filter(d -> estadosAsistidos.contains(d.getEstadoAsistencia())).count();
        BigDecimal asistencia = registros.isEmpty() ? BigDecimal.ZERO : BigDecimal.valueOf(
                asistidas * 100.0 / registros.size()).setScale(2, RoundingMode.HALF_UP);

        LocalDate hoy = LocalDate.now();
        MetricasAcademicas dimensionales = calcularMetricasDimensionales(
                idInstitucion, inscripcion.getIdEstudiante(), asignaciones,
                periodos.values(), hoy, idPeriodo != null);
        if (dimensionales.tieneDatos()) {
            return new Metricas(asistencia, dimensionales.promedio(), dimensionales.tendencia(),
                    dimensionales.pendientes(), dimensionales.materiasBajoRendimiento(),
                    dimensionales.evolucion(), dimensionales.desgloseMaterias(),
                    !registros.isEmpty(), true);
        }

        // Compatibilidad para instituciones que todavía no migraron al modelo por dimensiones.
        Comparator<Evaluacion> ordenEvaluacion = Comparator.comparing(Evaluacion::getPeriodo)
                .thenComparing(Evaluacion::getCreadoEn, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Evaluacion::getId);
        // Evaluacion es una plantilla global por materia; la gestion se delimita con sus periodos
        // y las materias provienen exclusivamente de asignaciones activas del paralelo/gestion.
        List<Evaluacion> evaluaciones = asignaciones.stream().map(AsignacionDocente::getIdMateria).distinct()
                .flatMap(idMateria -> evaluacionRepository.findAllByIdInstitucionAndIdMateria(idInstitucion, idMateria).stream())
                .filter(e -> !"ANULADA".equals(e.getEstado()))
                .filter(e -> periodos.containsKey(e.getPeriodo()))
                .filter(e -> idPeriodo != null || !periodos.get(e.getPeriodo()).getFechaInicio().isAfter(hoy))
                .sorted(ordenEvaluacion).toList();
        Map<UUID, Evaluacion> evaluacionPorId = evaluaciones.stream()
                .collect(Collectors.toMap(Evaluacion::getId, e -> e));
        List<Calificacion> todas = calificacionRepository
                .findAllByIdInstitucionAndIdInscripcion(idInstitucion, inscripcion.getId()).stream()
                .filter(c -> evaluacionPorId.containsKey(c.getIdEvaluacion())).toList();
        Map<UUID, Calificacion> calificacionPorEvaluacion = todas.stream()
                .collect(Collectors.toMap(Calificacion::getIdEvaluacion, c -> c, (a, b) -> a));
        List<Calificacion> numericas = todas.stream()
                .filter(c -> c.getNotaNumerica() != null)
                .filter(c -> "NUMERICA".equals(evaluacionPorId.get(c.getIdEvaluacion()).getEscala()))
                .sorted(Comparator.comparing(c -> evaluacionPorId.get(c.getIdEvaluacion()), ordenEvaluacion)).toList();

        Map<MateriaPeriodo, BigDecimal> consolidados = consolidarPorMateriaPeriodo(
                evaluaciones, numericas, periodos, hoy);
        BigDecimal promedio = consolidados.isEmpty() ? BigDecimal.ZERO : consolidados.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(consolidados.size()), 2, RoundingMode.HALF_UP);
        int pendientes = (int) evaluaciones.stream()
                .filter(e -> periodos.get(e.getPeriodo()).getFechaFin().isBefore(hoy))
                .filter(e -> {
                    Calificacion c = calificacionPorEvaluacion.get(e.getId());
                    return c == null || (c.getNotaNumerica() == null && c.getNotaLiteral() == null);
                }).count();
        String tendencia = tendenciaPorMateria(numericas, evaluacionPorId, ordenEvaluacion);
        int materiasBajoRendimiento = materiasBajoRendimiento(consolidados, idInstitucion);
        List<EvolucionNotaResponse> evolucion = numericas.stream().map(c -> {
            Evaluacion e = evaluacionPorId.get(c.getIdEvaluacion());
            return new EvolucionNotaResponse(e.getNombre(), e.getPeriodo(), c.getNotaNumerica(), e.getCreadoEn());
        }).toList();
        return new Metricas(asistencia, promedio, tendencia, pendientes, materiasBajoRendimiento, evolucion,
                 List.of(), !registros.isEmpty(), !numericas.isEmpty());
    }

    private MetricasAcademicas calcularMetricasDimensionales(
            UUID idInstitucion, UUID idEstudiante, List<AsignacionDocente> asignaciones,
            Collection<PeriodoEvaluacion> periodosGestion, LocalDate hoy,
            boolean incluirPeriodoSeleccionado) {
        Set<UUID> materias = asignaciones.stream().map(AsignacionDocente::getIdMateria).collect(Collectors.toSet());
        List<PeriodoEvaluacion> periodos = periodosGestion.stream()
                .filter(periodo -> incluirPeriodoSeleccionado || !periodo.getFechaInicio().isAfter(hoy)).toList();
        if (materias.isEmpty() || periodos.isEmpty()) return MetricasAcademicas.sinDatos();

        Map<UUID, PeriodoEvaluacion> periodoPorId = periodos.stream()
                .collect(Collectors.toMap(PeriodoEvaluacion::getId, periodo -> periodo));
        List<UUID> idsPeriodo = periodos.stream().map(PeriodoEvaluacion::getId).toList();
        List<ActividadEvaluativa> actividades = actividadEvaluativaRepository
                .findAllByIdInstitucionAndIdPeriodoEvaluacionIn(idInstitucion, idsPeriodo).stream()
                .filter(a -> materias.contains(a.getIdMateria()))
                .filter(a -> "PUBLICADA".equals(a.getEstado()) || "CERRADA".equals(a.getEstado()))
                .toList();
        List<UUID> idsActividad = actividades.stream().map(ActividadEvaluativa::getId).toList();
        Map<UUID, CalificacionActividad> notaPorActividad = idsActividad.isEmpty() ? Map.of()
                : calificacionActividadRepository.findAllByIdEstudianteAndIdActividadIn(idEstudiante, idsActividad)
                        .stream().filter(nota -> nota.getNotaObtenida() != null)
                        .collect(Collectors.toMap(CalificacionActividad::getIdActividad, nota -> nota));
        List<CalificacionSer> notasSer = calificacionSerRepository
                .findAllByIdInstitucionAndIdEstudianteAndIdPeriodoEvaluacionIn(
                        idInstitucion, idEstudiante, idsPeriodo).stream()
                .filter(nota -> materias.contains(nota.getIdMateria())).toList();
        List<AutoevaluacionTrimestral> autoevaluaciones = autoevaluacionRepository
                .findAllByIdInstitucionAndIdEstudianteAndIdPeriodoEvaluacionIn(
                        idInstitucion, idEstudiante, idsPeriodo).stream()
                .filter(nota -> materias.contains(nota.getIdMateria())).toList();

        Map<MateriaPeriodo, List<ActividadEvaluativa>> actividadesPorClave = actividades.stream()
                .collect(Collectors.groupingBy(a -> new MateriaPeriodo(
                        a.getIdMateria(), periodoPorId.get(a.getIdPeriodoEvaluacion()).getNumeroPeriodo())));
        Map<MateriaPeriodo, CalificacionSer> serPorClave = notasSer.stream().collect(Collectors.toMap(
                nota -> new MateriaPeriodo(nota.getIdMateria(),
                        periodoPorId.get(nota.getIdPeriodoEvaluacion()).getNumeroPeriodo()), nota -> nota));
        Map<MateriaPeriodo, AutoevaluacionTrimestral> autoPorClave = autoevaluaciones.stream()
                .collect(Collectors.toMap(nota -> new MateriaPeriodo(nota.getIdMateria(),
                        periodoPorId.get(nota.getIdPeriodoEvaluacion()).getNumeroPeriodo()), nota -> nota));

        Map<String, BigDecimal> pesos = new HashMap<>();
        periodoDimensionRepository.findAllByIdPeriodoEvaluacionIn(idsPeriodo).forEach(pd ->
                pesos.put(pd.getIdPeriodoEvaluacion() + ":" + pd.getDimension().getNombre(),
                        BigDecimal.valueOf(pd.getPonderacion())));

        Set<MateriaPeriodo> claves = new HashSet<>(actividadesPorClave.keySet());
        claves.addAll(serPorClave.keySet());
        claves.addAll(autoPorClave.keySet());
        Map<MateriaPeriodo, BigDecimal> consolidados = new HashMap<>();
        Map<MateriaPeriodo, DatosDimension> datosPorClave = new HashMap<>();
        int pendientes = 0;
        for (MateriaPeriodo clave : claves) {
            PeriodoEvaluacion periodo = periodos.stream()
                    .filter(p -> p.getNumeroPeriodo().equals(clave.periodo())).findFirst().orElseThrow();
            List<ActividadEvaluativa> grupo = actividadesPorClave.getOrDefault(clave, List.of());
            boolean evidenciaActividad = grupo.stream().anyMatch(a -> notaPorActividad.containsKey(a.getId()));
            CalificacionSer ser = serPorClave.get(clave);
            AutoevaluacionTrimestral auto = autoPorClave.get(clave);
            if (!evidenciaActividad && ser == null && auto == null) continue;

            BigDecimal pesoSaber = peso(pesos, periodo, "SABER", periodo.getPesoSaber());
            BigDecimal pesoHacer = peso(pesos, periodo, "HACER", periodo.getPesoHacer());
            BigDecimal pesoSer = peso(pesos, periodo, "SER", periodo.getPesoSer());
            BigDecimal pesoAuto = peso(pesos, periodo, "AUTOEVALUACION", periodo.getPesoAuto());
            BigDecimal saber = calcularDimension(grupo, "SABER", notaPorActividad, pesoSaber);
            BigDecimal hacer = calcularDimension(grupo, "HACER", notaPorActividad, pesoHacer);
            BigDecimal notaSer = ser == null ? BigDecimal.ZERO : valorOZero(ser.getNotaSer());
            BigDecimal notaAuto = auto == null ? BigDecimal.ZERO : valorOZero(auto.getNotaAutoevaluacion());
            datosPorClave.put(clave, new DatosDimension(
                    saber, hacer, notaSer, notaAuto, pesoSaber, pesoHacer, pesoSer, pesoAuto));
            consolidados.put(clave, saber.add(hacer).add(notaSer).add(notaAuto)
                    .setScale(2, RoundingMode.HALF_UP));
            if (periodo.getFechaFin().isBefore(hoy)) {
                pendientes += (int) grupo.stream().filter(a -> !notaPorActividad.containsKey(a.getId())).count();
            }
        }
        if (consolidados.isEmpty()) return MetricasAcademicas.sinDatos();

        BigDecimal promedio = consolidados.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(consolidados.size()), 2, RoundingMode.HALF_UP);
        String tendencia = tendenciaConsolidada(consolidados);
        int bajoRendimiento = materiasBajoRendimiento(consolidados, idInstitucion);
        BigDecimal notaMinima = BigDecimal.valueOf(
                configuracionService.getInt(idInstitucion, "NOTA_MINIMA_APROBACION"));
        Map<UUID, String> nombresMateria = materiaRepository
                .findAllByIdInAndIdInstitucionAndEstado(materias, idInstitucion, "ACTIVO").stream()
                .collect(Collectors.toMap(materia -> materia.getId(), materia -> materia.getNombre()));
        List<DesgloseMateriaResponse> desgloseMaterias = datosPorClave.entrySet().stream()
                .collect(Collectors.groupingBy(entry -> entry.getKey().idMateria()))
                .entrySet().stream()
                .map(entry -> {
                    List<DatosDimension> datos = entry.getValue().stream().map(Map.Entry::getValue).toList();
                    BigDecimal cantidad = BigDecimal.valueOf(datos.size());
                    BigDecimal saber = promedioDimension(datos, DatosDimension::saber, cantidad);
                    BigDecimal hacer = promedioDimension(datos, DatosDimension::hacer, cantidad);
                    BigDecimal ser = promedioDimension(datos, DatosDimension::ser, cantidad);
                    BigDecimal auto = promedioDimension(datos, DatosDimension::auto, cantidad);
                    BigDecimal total = saber.add(hacer).add(ser).add(auto).setScale(2, RoundingMode.HALF_UP);
                    List<NotaPeriodoResponse> notasPorPeriodo = entry.getValue().stream()
                            .map(valor -> new NotaPeriodoResponse(
                                    valor.getKey().periodo(), consolidados.get(valor.getKey())))
                            .sorted(Comparator.comparingInt(NotaPeriodoResponse::periodo))
                            .toList();
                    return new DesgloseMateriaResponse(
                            entry.getKey(),
                            nombresMateria.getOrDefault(entry.getKey(), "Materia sin nombre"),
                            total,
                            total.compareTo(notaMinima) >= 0,
                            List.of(
                                    new DesgloseDimensionResponse("SABER", saber,
                                            promedioDimension(datos, DatosDimension::pesoSaber, cantidad)),
                                    new DesgloseDimensionResponse("HACER", hacer,
                                            promedioDimension(datos, DatosDimension::pesoHacer, cantidad)),
                                    new DesgloseDimensionResponse("SER", ser,
                                            promedioDimension(datos, DatosDimension::pesoSer, cantidad)),
                                    new DesgloseDimensionResponse("AUTOEVALUACION", auto,
                                            promedioDimension(datos, DatosDimension::pesoAuto, cantidad))),
                            notasPorPeriodo);
                })
                .sorted(Comparator.comparing(DesgloseMateriaResponse::notaTotal)
                        .thenComparing(DesgloseMateriaResponse::nombreMateria))
                .toList();
        List<EvolucionNotaResponse> evolucion = consolidados.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(MateriaPeriodo::periodo)
                        .thenComparing(MateriaPeriodo::idMateria)))
                .map(entry -> {
                    PeriodoEvaluacion periodo = periodos.stream()
                            .filter(p -> p.getNumeroPeriodo().equals(entry.getKey().periodo())).findFirst().orElseThrow();
                    return new EvolucionNotaResponse("Consolidado P" + entry.getKey().periodo(),
                            entry.getKey().periodo(), entry.getValue(),
                            periodo.getFechaFin().atStartOfDay().toInstant(ZoneOffset.UTC));
                }).toList();
        return new MetricasAcademicas(
                promedio, tendencia, pendientes, bajoRendimiento, evolucion, desgloseMaterias, true);
    }

    private BigDecimal promedioDimension(List<DatosDimension> datos,
                                         java.util.function.Function<DatosDimension, BigDecimal> selector,
                                         BigDecimal cantidad) {
        return datos.stream().map(selector).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(cantidad, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularDimension(List<ActividadEvaluativa> actividades, String dimension,
                                         Map<UUID, CalificacionActividad> notas, BigDecimal peso) {
        List<BigDecimal> registradas = actividades.stream()
                .filter(a -> dimension.equals(a.getDimension()))
                .map(a -> notas.get(a.getId()))
                .filter(Objects::nonNull)
                .map(CalificacionActividad::getNotaObtenida).toList();
        if (registradas.isEmpty()) return BigDecimal.ZERO;
        BigDecimal promedio = registradas.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(registradas.size()), 4, RoundingMode.HALF_UP);
        return promedio.multiply(peso).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal peso(Map<String, BigDecimal> pesos, PeriodoEvaluacion periodo,
                            String dimension, Integer fallback) {
        return pesos.getOrDefault(periodo.getId() + ":" + dimension,
                BigDecimal.valueOf(fallback == null ? 0 : fallback));
    }

    private String tendenciaConsolidada(Map<MateriaPeriodo, BigDecimal> consolidados) {
        List<BigDecimal> diferencias = consolidados.entrySet().stream()
                .collect(Collectors.groupingBy(entry -> entry.getKey().idMateria()))
                .values().stream()
                .map(valores -> valores.stream().sorted(Comparator.comparing(e -> e.getKey().periodo())).toList())
                .filter(valores -> valores.size() >= 2)
                .map(valores -> valores.getLast().getValue().subtract(valores.getFirst().getValue()))
                .toList();
        if (diferencias.isEmpty()) return "SIN_DATOS";
        BigDecimal diferencia = diferencias.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(diferencias.size()), 2, RoundingMode.HALF_UP);
        if (diferencia.compareTo(BigDecimal.valueOf(5)) >= 0) return "SUBIENDO";
        if (diferencia.compareTo(BigDecimal.valueOf(-5)) <= 0) return "BAJANDO";
        return "ESTABLE";
    }

    private Map<MateriaPeriodo, BigDecimal> consolidarPorMateriaPeriodo(
            List<Evaluacion> evaluaciones, List<Calificacion> calificaciones,
            Map<Integer, PeriodoEvaluacion> periodos, LocalDate hoy) {
        Map<UUID, Calificacion> notas = calificaciones.stream()
                .collect(Collectors.toMap(Calificacion::getIdEvaluacion, nota -> nota, (a, b) -> a));
        Map<MateriaPeriodo, List<Evaluacion>> porMateriaPeriodo = evaluaciones.stream()
                .filter(e -> "NUMERICA".equals(e.getEscala()))
                .collect(Collectors.groupingBy(e -> new MateriaPeriodo(e.getIdMateria(), e.getPeriodo())));
        Map<MateriaPeriodo, BigDecimal> consolidados = new HashMap<>();
        porMateriaPeriodo.forEach((clave, grupo) -> {
            List<Evaluacion> calificadas = grupo.stream().filter(e -> notas.containsKey(e.getId())).toList();
            boolean vencido = periodos.get(clave.periodo()).getFechaFin().isBefore(hoy);
            List<Evaluacion> basePonderacion = vencido ? grupo : calificadas;
            BigDecimal ponderacionBase = basePonderacion.stream().map(Evaluacion::getPonderacion)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (ponderacionBase.signum() == 0) return;
            BigDecimal ponderado = calificadas.stream()
                    .map(e -> notas.get(e.getId()).getNotaNumerica().multiply(e.getPonderacion()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            consolidados.put(clave, ponderado.divide(ponderacionBase, 2, RoundingMode.HALF_UP));
        });
        return consolidados;
    }

    private int materiasBajoRendimiento(Map<MateriaPeriodo, BigDecimal> consolidados, UUID idInstitucion) {
        BigDecimal minimo = BigDecimal.valueOf(configuracionService.getInt(idInstitucion, "NOTA_MINIMA_APROBACION"));
        return (int) consolidados.entrySet().stream()
                .filter(entry -> entry.getValue().compareTo(minimo) < 0)
                .map(entry -> entry.getKey().idMateria()).distinct().count();
    }

    private String tendenciaPorMateria(List<Calificacion> calificaciones, Map<UUID, Evaluacion> evaluaciones,
                                        Comparator<Evaluacion> orden) {
        List<BigDecimal> diferencias = calificaciones.stream()
                .collect(Collectors.groupingBy(c -> evaluaciones.get(c.getIdEvaluacion()).getIdMateria()))
                .values().stream().map(notas -> notas.stream()
                        .sorted(Comparator.comparing(c -> evaluaciones.get(c.getIdEvaluacion()), orden)).toList())
                .filter(notas -> notas.size() >= 2)
                .map(notas -> {
                    List<Calificacion> ultimas = notas.subList(Math.max(0, notas.size() - 3), notas.size());
                    return ultimas.getLast().getNotaNumerica().subtract(ultimas.getFirst().getNotaNumerica());
                }).toList();
        if (diferencias.isEmpty()) return "SIN_DATOS";
        BigDecimal diferencia = diferencias.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(diferencias.size()), 2, RoundingMode.HALF_UP);
        if (diferencia.compareTo(BigDecimal.valueOf(5)) >= 0) return "SUBIENDO";
        if (diferencia.compareTo(BigDecimal.valueOf(-5)) <= 0) return "BAJANDO";
        return "ESTABLE";
    }

    private List<FactorContribuyente> construirFactores(UUID idInstitucion, Metricas m) {
        int minimoAsistencia = configuracionService.getInt(idInstitucion, "PORCENTAJE_ASISTENCIA_MINIMO");
        int notaMinima = configuracionService.getInt(idInstitucion, "NOTA_MINIMA_APROBACION");
        double asistenciaFactor = minimoAsistencia <= 0 ? 0
                : Math.max(0, (minimoAsistencia - m.porcentajeAsistencia.doubleValue()) / minimoAsistencia);
        double notasFactor = notaMinima <= 0 ? 0
                : Math.max(0, (notaMinima - m.promedio.doubleValue()) / notaMinima);
        double tendenciaFactor = switch (m.tendencia) {
            case "BAJANDO" -> 1;
            case "ESTABLE" -> .35;
            case "SIN_DATOS" -> .15;
            default -> 0;
        };
        double pendientesFactor = Math.min(1, m.evaluacionesPendientes / 3.0);
        double historialFactor = Math.min(1, m.materiasBajoRendimiento / 3.0);
        return List.of(
                factor("Asistencia", 30, m.porcentajeAsistencia.doubleValue(), asistenciaFactor,
                        "Asistencia " + m.porcentajeAsistencia + "% (minimo " + minimoAsistencia + "%)"),
                factor("Calificaciones", 40, m.promedio.doubleValue(), notasFactor,
                        "Promedio " + m.promedio + " (minimo " + notaMinima + ")"),
                factor("Tendencia", 15, tendenciaFactor * 100, tendenciaFactor, "Tendencia de notas: " + m.tendencia),
                factor("Evaluaciones pendientes", 5, m.evaluacionesPendientes, pendientesFactor,
                        m.evaluacionesPendientes + " evaluacion(es) vencidas sin nota"),
                factor("Materias con bajo rendimiento", 10, m.materiasBajoRendimiento, historialFactor,
                        m.materiasBajoRendimiento + " materia(s) bajo el minimo"));
    }

    private FactorContribuyente factor(String nombre, double peso, double valor, double factor, String descripcion) {
        return new FactorContribuyente(nombre, peso, valor, round(peso * factor), descripcion);
    }

    private List<String> recomendaciones(UUID idInstitucion, Metricas m, String nivel) {
        List<String> result = new ArrayList<>();
        if (m.porcentajeAsistencia.compareTo(BigDecimal.valueOf(
                configuracionService.getInt(idInstitucion, "PORCENTAJE_ASISTENCIA_MINIMO"))) < 0)
            result.add("Contactar al tutor para revisar las inasistencias.");
        if (m.promedio.compareTo(BigDecimal.valueOf(
                configuracionService.getInt(idInstitucion, "NOTA_MINIMA_APROBACION"))) < 0)
            result.add("Asignar refuerzo academico y revisar las actividades con menor nota.");
        if ("BAJANDO".equals(m.tendencia))
            result.add("Programar una entrevista breve para identificar la causa de la tendencia descendente.");
        if (m.evaluacionesPendientes > 0)
            result.add("Completar o verificar el registro de evaluaciones vencidas.");
        if ("CRITICO".equals(nivel))
            result.add("Derivar el caso a coordinacion academica para seguimiento prioritario.");
        return result;
    }

    private AnalisisRiesgoResponse response(List<RiesgoEstudianteDetalleResponse> estudiantes,
                                             List<ResumenRiesgoParalelo> comparativa) {
        Map<String, Long> distribucion = Arrays.stream(new String[]{"BAJO", "MEDIO", "ALTO", "CRITICO", "SIN_DATOS"})
                .collect(Collectors.toMap(n -> n, n -> estudiantes.stream().filter(e ->
                                "SIN_DATOS".equals(n) ? "DATOS_INSUFICIENTES".equals(e.estadoAnalisis())
                                        : "CALCULADO".equals(e.estadoAnalisis()) && n.equals(e.nivelRiesgo())).count(),
                        (a, b) -> a, LinkedHashMap::new));
        return new AnalisisRiesgoResponse(estudiantes.size(), distribucion, comparativa,
                estudiantes.stream().sorted(Comparator.comparing(RiesgoEstudianteDetalleResponse::score).reversed()).toList(),
                Instant.now());
    }

    private ResumenRiesgoParalelo resumenParalelo(Paralelo paralelo,
                                                   List<RiesgoEstudianteDetalleResponse> estudiantes) {
        List<RiesgoEstudianteDetalleResponse> calculados = estudiantes.stream()
                .filter(e -> "CALCULADO".equals(e.estadoAnalisis())).toList();
        int enRiesgo = (int) calculados.stream()
                .filter(e -> "ALTO".equals(e.nivelRiesgo()) || "CRITICO".equals(e.nivelRiesgo())).count();
        BigDecimal promedio = calculados.isEmpty() ? BigDecimal.ZERO : calculados.stream()
                .map(RiesgoEstudianteDetalleResponse::score).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(calculados.size()), 2, RoundingMode.HALF_UP);
        return new ResumenRiesgoParalelo(paralelo.getId(), paralelo.getNombre(), estudiantes.size(),
                calculados.size(), estudiantes.size() - calculados.size(), enRiesgo,
                calculados.isEmpty() ? BigDecimal.ZERO : BigDecimal.valueOf(enRiesgo * 100.0 / calculados.size())
                        .setScale(2, RoundingMode.HALF_UP), promedio);
    }

    private String nivel(BigDecimal score) {
        if (score.compareTo(BigDecimal.valueOf(76)) >= 0) return "CRITICO";
        if (score.compareTo(BigDecimal.valueOf(51)) >= 0) return "ALTO";
        if (score.compareTo(BigDecimal.valueOf(26)) >= 0) return "MEDIO";
        return "BAJO";
    }

    private void validarAccesoDocente(UUID idParalelo, UUID idGestion) {
        if (tieneAccesoInstitucional() || !SecurityUtils.currentUserHasRole("DOCENTE")) return;
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        Docente docente = docenteRepository.findByIdUsuarioAndIdInstitucion(SecurityUtils.currentUserId(), idInstitucion)
                .orElseThrow(() -> new AccessDeniedException("El usuario autenticado no tiene docente asociado"));
        boolean asignado = asignacionRepository.existsByIdInstitucionAndIdDocenteAndIdParaleloAndIdGestionAndEstado(
                idInstitucion, docente.getId(), idParalelo, idGestion, "ACTIVA");
        if (!asignado) throw new AccessDeniedException("No tienes una asignacion activa para el paralelo solicitado");
    }

    private boolean tieneAccesoInstitucional() {
        return SecurityUtils.currentUserHasRole("ADMIN_INSTITUCION")
                || SecurityUtils.currentUserHasRole("DIRECTOR")
                || SecurityUtils.currentUserHasRole("SECRETARIO")
                || SecurityUtils.currentUserHasRole("SUPER_ADMIN");
    }

    private UUID idDocenteVisible(UUID idInstitucion) {
        if (tieneAccesoInstitucional() || !SecurityUtils.currentUserHasRole("DOCENTE")) return null;
        return docenteRepository
                .findByIdUsuarioAndIdInstitucion(SecurityUtils.currentUserId(), idInstitucion)
                .orElseThrow(() -> new AccessDeniedException("El usuario autenticado no tiene docente asociado"))
                .getId();
    }

    private String serializarFactores(List<FactorContribuyente> factores) {
        try {
            return objectMapper.writeValueAsString(factores);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudieron serializar los factores de riesgo", exception);
        }
    }

    private List<FactorContribuyente> deserializarFactores(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    private BigDecimal valorOZero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record Metricas(BigDecimal porcentajeAsistencia, BigDecimal promedio, String tendencia,
                            int evaluacionesPendientes, int materiasBajoRendimiento,
                            List<EvolucionNotaResponse> evolucion,
                            List<DesgloseMateriaResponse> desgloseMaterias, boolean tieneAsistencia,
                            boolean tieneNotaNumerica) {
    }

    private record MetricasAcademicas(BigDecimal promedio, String tendencia, int pendientes,
                                       int materiasBajoRendimiento, List<EvolucionNotaResponse> evolucion,
                                       List<DesgloseMateriaResponse> desgloseMaterias, boolean tieneDatos) {
        private static MetricasAcademicas sinDatos() {
            return new MetricasAcademicas(
                    BigDecimal.ZERO, "SIN_DATOS", 0, 0, List.of(), List.of(), false);
        }
    }

    private record DatosDimension(BigDecimal saber, BigDecimal hacer, BigDecimal ser, BigDecimal auto,
                                  BigDecimal pesoSaber, BigDecimal pesoHacer,
                                  BigDecimal pesoSer, BigDecimal pesoAuto) {
    }

    private record MateriaPeriodo(UUID idMateria, Integer periodo) {
    }
}
