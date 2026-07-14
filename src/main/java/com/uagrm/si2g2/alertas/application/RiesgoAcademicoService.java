package com.uagrm.si2g2.alertas.application;

import com.uagrm.si2g2.alertas.domain.AlertaRiesgo;
import com.uagrm.si2g2.alertas.domain.AlertaRiesgoRepository;
import com.uagrm.si2g2.alertas.domain.RecomendacionIa;
import com.uagrm.si2g2.alertas.domain.RecomendacionIaRepository;
import com.uagrm.si2g2.alertas.dto.*;
import com.uagrm.si2g2.asignacion.domain.AsignacionDocente;
import com.uagrm.si2g2.asignacion.domain.AsignacionDocenteRepository;
import com.uagrm.si2g2.asistencia.domain.AsistenciaDetalle;
import com.uagrm.si2g2.asistencia.domain.AsistenciaDetalleRepository;
import com.uagrm.si2g2.asistencia.domain.AsistenciaRegistro;
import com.uagrm.si2g2.asistencia.domain.AsistenciaRegistroRepository;
import com.uagrm.si2g2.calificacion.domain.Calificacion;
import com.uagrm.si2g2.calificacion.domain.CalificacionRepository;
import com.uagrm.si2g2.calificacion.domain.Evaluacion;
import com.uagrm.si2g2.calificacion.domain.EvaluacionRepository;
import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.curso.domain.Curso;
import com.uagrm.si2g2.curso.domain.CursoRepository;
import com.uagrm.si2g2.curso.domain.Paralelo;
import com.uagrm.si2g2.curso.domain.ParaleloRepository;
import com.uagrm.si2g2.estudiante.domain.Estudiante;
import com.uagrm.si2g2.estudiante.domain.EstudianteRepository;
import com.uagrm.si2g2.inscripcion.domain.Inscripcion;
import com.uagrm.si2g2.inscripcion.domain.InscripcionRepository;
import com.uagrm.si2g2.institucion.application.ConfiguracionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Calcula un score explicable con datos reales. No depende de FastAPI para que
 * la deteccion siga disponible aun cuando el servicio de IA este apagado.
 */
@Service
@RequiredArgsConstructor
public class RiesgoAcademicoService {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final Set<String> ESTADOS_ALERTA = Set.of("ABIERTA", "EN_SEGUIMIENTO", "ATENDIDA", "CERRADA");

    private final InscripcionRepository inscripcionRepository;
    private final EstudianteRepository estudianteRepository;
    private final ParaleloRepository paraleloRepository;
    private final CursoRepository cursoRepository;
    private final AsignacionDocenteRepository asignacionRepository;
    private final AsistenciaRegistroRepository asistenciaRegistroRepository;
    private final AsistenciaDetalleRepository asistenciaDetalleRepository;
    private final EvaluacionRepository evaluacionRepository;
    private final CalificacionRepository calificacionRepository;
    private final AlertaRiesgoRepository alertaRepository;
    private final RecomendacionIaRepository recomendacionRepository;
    private final ConfiguracionService configuracionService;

    @Transactional
    public AnalisisRiesgoResponse analizarParalelo(UUID idParalelo, UUID idGestion) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        alertaRepository.desactivarPorGestion(idInstitucion, idGestion);
        List<RiesgoEstudianteDetalleResponse> estudiantes = analizarParaleloInterno(idInstitucion, idParalelo, idGestion, true);
        return response(estudiantes, List.of(resumenParalelo(idParalelo, estudiantes)));
    }

    @Transactional
    public AnalisisRiesgoResponse analizarInstitucion(UUID idGestion) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        alertaRepository.desactivarPorGestion(idInstitucion, idGestion);

        List<RiesgoEstudianteDetalleResponse> estudiantes = new ArrayList<>();
        List<ResumenRiesgoParalelo> comparativa = new ArrayList<>();
        paraleloRepository.findAllByIdInstitucion(idInstitucion).stream()
                .filter(p -> idGestion.equals(p.getIdGestionAcademica()))
                .filter(p -> "ACTIVO".equals(p.getEstado()))
                .forEach(paralelo -> {
                    List<RiesgoEstudianteDetalleResponse> resultado = analizarParaleloInterno(
                            idInstitucion, paralelo.getId(), idGestion, true);
                    estudiantes.addAll(resultado);
                    comparativa.add(resumenParalelo(paralelo.getId(), resultado));
                });
        return response(estudiantes, comparativa);
    }

    @Transactional(readOnly = true)
    public RiesgoEstudianteDetalleResponse detalleEstudiante(UUID idEstudiante, UUID idGestion) {
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        Inscripcion inscripcion = inscripcionRepository.findAllByIdInstitucionAndIdEstudiante(idInstitucion, idEstudiante)
                .stream()
                .filter(i -> idGestion.equals(i.getIdGestion()))
                .filter(i -> "ACTIVA".equals(i.getEstado()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("El estudiante no tiene una inscripción activa en la gestión"));
        return analizarInscripcion(idInstitucion, inscripcion, false);
    }

    @Transactional
    public AlertaRiesgo actualizarEstado(UUID idAlerta, String estado) {
        String normalizado = estado.trim().toUpperCase(Locale.ROOT);
        if (!ESTADOS_ALERTA.contains(normalizado)) {
            throw new IllegalArgumentException("Estado de alerta inválido");
        }
        AlertaRiesgo alerta = alertaRepository.findByIdAndIdInstitucion(idAlerta, SecurityUtils.requireCurrentInstitutionId())
                .orElseThrow(() -> new EntityNotFoundException("Alerta no encontrada"));
        alerta.setEstadoAlerta(normalizado);
        return alertaRepository.save(alerta);
    }

    private List<RiesgoEstudianteDetalleResponse> analizarParaleloInterno(
            UUID idInstitucion, UUID idParalelo, UUID idGestion, boolean persistir) {
        paraleloRepository.findByIdAndIdInstitucion(idParalelo, idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Paralelo no encontrado"));
        return inscripcionRepository.findAllByIdInstitucionAndIdParalelo(idInstitucion, idParalelo).stream()
                .filter(i -> idGestion.equals(i.getIdGestion()))
                .filter(i -> "ACTIVA".equals(i.getEstado()))
                .map(i -> analizarInscripcion(idInstitucion, i, persistir))
                .sorted(Comparator.comparing(RiesgoEstudianteDetalleResponse::score).reversed())
                .toList();
    }

    private RiesgoEstudianteDetalleResponse analizarInscripcion(UUID idInstitucion, Inscripcion inscripcion, boolean persistir) {
        Estudiante estudiante = estudianteRepository.findByIdAndIdInstitucion(inscripcion.getIdEstudiante(), idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Estudiante no encontrado"));
        Paralelo paralelo = paraleloRepository.findByIdAndIdInstitucion(inscripcion.getIdParalelo(), idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Paralelo no encontrado"));
        Curso curso = cursoRepository.findByIdAndIdInstitucion(paralelo.getIdCurso(), idInstitucion).orElse(null);

        List<AsignacionDocente> asignaciones = asignacionRepository
                .findAllByIdInstitucionAndIdParalelo(idInstitucion, paralelo.getId()).stream()
                .filter(a -> inscripcion.getIdGestion().equals(a.getIdGestion()))
                .filter(a -> "ACTIVA".equals(a.getEstado()))
                .toList();

        Metricas metricas = calcularMetricas(idInstitucion, inscripcion, asignaciones);
        List<FactorContribuyente> factores = construirFactores(idInstitucion, metricas);
        BigDecimal score = factores.stream()
                .map(f -> BigDecimal.valueOf(f.impacto()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        String nivel = nivel(score);
        List<String> recomendaciones = recomendaciones(idInstitucion, metricas, nivel);

        UUID idAlerta = null;
        String estadoAlerta = "SIN_ALERTA";
        if (persistir && score.compareTo(BigDecimal.valueOf(25)) > 0) {
            AlertaRiesgo alerta = AlertaRiesgo.builder()
                    .idInstitucion(idInstitucion)
                    .idEstudiante(estudiante.getId())
                    .idGestionAcademica(inscripcion.getIdGestion())
                    .nivelRiesgo(nivel)
                    .motivo(factores.stream().filter(f -> f.impacto() > 0).map(FactorContribuyente::descripcion).collect(Collectors.joining(". ")))
                    .scoreIa(score.divide(HUNDRED, 4, RoundingMode.HALF_UP))
                    .porcentajeAsistencia(metricas.porcentajeAsistencia)
                    .promedioCalificaciones(metricas.promedio)
                    .tendenciaNotas(metricas.tendencia)
                    .evaluacionesPendientes(metricas.evaluacionesPendientes)
                    .materiasReprobadasHistorial(metricas.materiasBajoRendimiento)
                    .factoresJson(serializarFactores(factores))
                    .estadoAlerta("ABIERTA")
                    .activa(true)
                    .build();
            alerta = alertaRepository.save(alerta);
            idAlerta = alerta.getId();
            estadoAlerta = alerta.getEstadoAlerta();
            if (!recomendaciones.isEmpty()) {
                UUID alertaId = alerta.getId();
                recomendacionRepository.saveAll(recomendaciones.stream()
                        .map(texto -> RecomendacionIa.builder().idAlertaRiesgo(alertaId)
                                .descripcion(texto).tipoAccion("SEGUIMIENTO").build())
                        .toList());
            }
        } else if (!persistir) {
            Optional<AlertaRiesgo> alertaActual = alertaRepository
                    .findTopByIdEstudianteAndIdGestionAcademicaAndActivaTrueOrderByProcesadoEnDesc(estudiante.getId(), inscripcion.getIdGestion());
            idAlerta = alertaActual.map(AlertaRiesgo::getId).orElse(null);
            estadoAlerta = alertaActual.map(AlertaRiesgo::getEstadoAlerta).orElse("SIN_ALERTA");
        }

        return new RiesgoEstudianteDetalleResponse(
                idAlerta, estudiante.getId(), estudiante.getCodigoEstudiante(), estudiante.getNombres(), estudiante.getApellidos(),
                curso != null ? curso.getNombre() : "Sin curso", paralelo.getNombre(), score, nivel,
                metricas.porcentajeAsistencia, metricas.promedio, metricas.tendencia, metricas.evaluacionesPendientes,
                metricas.materiasBajoRendimiento, estadoAlerta, factores, recomendaciones, metricas.evolucion);
    }

    private Metricas calcularMetricas(UUID idInstitucion, Inscripcion inscripcion, List<AsignacionDocente> asignaciones) {
        List<UUID> idsAsignacion = asignaciones.stream().map(AsignacionDocente::getId).toList();
        List<AsistenciaRegistro> registros = idsAsignacion.stream()
                .flatMap(id -> asistenciaRegistroRepository.findAllByIdInstitucionAndIdAsignacionDocente(idInstitucion, id).stream())
                .toList();
        List<AsistenciaDetalle> detalles = registros.isEmpty() ? List.of() : asistenciaDetalleRepository
                .findAllByIdAsistenciaRegistroIn(registros.stream().map(AsistenciaRegistro::getId).toList()).stream()
                .filter(d -> inscripcion.getId().equals(d.getIdInscripcion())).toList();
        BigDecimal asistencia = registros.isEmpty() ? BigDecimal.valueOf(100) : BigDecimal.valueOf(
                detalles.stream().filter(d -> "PRESENTE".equals(d.getEstadoAsistencia()) || "TARDANZA".equals(d.getEstadoAsistencia())).count()
                        * 100.0 / registros.size()).setScale(2, RoundingMode.HALF_UP);

        List<Evaluacion> evaluaciones = asignaciones.stream().map(AsignacionDocente::getIdMateria).distinct()
                .flatMap(idMateria -> evaluacionRepository.findAllByIdInstitucionAndIdMateria(idInstitucion, idMateria).stream())
                .filter(e -> !"ANULADA".equals(e.getEstado()))
                .sorted(Comparator.comparing(Evaluacion::getCreadoEn))
                .toList();
        Map<UUID, Evaluacion> evaluacionPorId = evaluaciones.stream().collect(Collectors.toMap(Evaluacion::getId, e -> e));
        List<Calificacion> calificaciones = calificacionRepository.findAllByIdInscripcion(inscripcion.getId()).stream()
                .filter(c -> evaluacionPorId.containsKey(c.getIdEvaluacion()))
                .filter(c -> c.getNotaNumerica() != null)
                .sorted(Comparator.comparing(c -> evaluacionPorId.get(c.getIdEvaluacion()).getCreadoEn()))
                .toList();
        BigDecimal promedio = calificaciones.isEmpty() ? BigDecimal.valueOf(100) : calificaciones.stream()
                .map(Calificacion::getNotaNumerica).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(calificaciones.size()), 2, RoundingMode.HALF_UP);
        int pendientes = Math.max(0, evaluaciones.size() - calificaciones.size());
        String tendencia = tendencia(calificaciones);
        int materiasBajoRendimiento = materiasBajoRendimiento(calificaciones, evaluacionPorId, idInstitucion);
        List<EvolucionNotaResponse> evolucion = calificaciones.stream().map(c -> {
            Evaluacion e = evaluacionPorId.get(c.getIdEvaluacion());
            return new EvolucionNotaResponse(e.getNombre(), e.getPeriodo(), c.getNotaNumerica(), e.getCreadoEn());
        }).toList();
        return new Metricas(asistencia, promedio, tendencia, pendientes, materiasBajoRendimiento, evolucion);
    }

    private int materiasBajoRendimiento(List<Calificacion> calificaciones, Map<UUID, Evaluacion> evaluaciones, UUID idInstitucion) {
        BigDecimal minimo = BigDecimal.valueOf(configuracionService.getInt(idInstitucion, "NOTA_MINIMA_APROBACION"));
        Map<UUID, List<BigDecimal>> porMateria = calificaciones.stream().collect(Collectors.groupingBy(
                c -> evaluaciones.get(c.getIdEvaluacion()).getIdMateria(),
                Collectors.mapping(Calificacion::getNotaNumerica, Collectors.toList())));
        return (int) porMateria.values().stream().filter(notas -> notas.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(notas.size()), 2, RoundingMode.HALF_UP).compareTo(minimo) < 0).count();
    }

    private List<FactorContribuyente> construirFactores(UUID idInstitucion, Metricas m) {
        int minimoAsistencia = configuracionService.getInt(idInstitucion, "PORCENTAJE_ASISTENCIA_MINIMO");
        int notaMinima = configuracionService.getInt(idInstitucion, "NOTA_MINIMA_APROBACION");
        double asistenciaFactor = Math.max(0, (minimoAsistencia - m.porcentajeAsistencia.doubleValue()) / minimoAsistencia);
        double notasFactor = Math.max(0, (notaMinima - m.promedio.doubleValue()) / notaMinima);
        double tendenciaFactor = switch (m.tendencia) { case "BAJANDO" -> 1; case "ESTABLE" -> .35; case "SIN_DATOS" -> .15; default -> 0; };
        double pendientesFactor = Math.min(1, m.evaluacionesPendientes / 3.0);
        double historialFactor = Math.min(1, m.materiasBajoRendimiento / 3.0);
        return List.of(
                factor("Asistencia", 30, m.porcentajeAsistencia.doubleValue(), asistenciaFactor, "Asistencia " + m.porcentajeAsistencia + "% (mínimo " + minimoAsistencia + "% )"),
                factor("Calificaciones", 40, m.promedio.doubleValue(), notasFactor, "Promedio " + m.promedio + " (mínimo " + notaMinima + ")"),
                factor("Tendencia", 15, tendenciaFactor * 100, tendenciaFactor, "Tendencia de notas: " + m.tendencia),
                factor("Evaluaciones pendientes", 5, m.evaluacionesPendientes, pendientesFactor, m.evaluacionesPendientes + " evaluación(es) sin nota"),
                factor("Materias con bajo rendimiento", 10, m.materiasBajoRendimiento, historialFactor, m.materiasBajoRendimiento + " materia(s) bajo el mínimo")
        );
    }

    private FactorContribuyente factor(String nombre, double peso, double valor, double factor, String descripcion) {
        return new FactorContribuyente(nombre, peso, valor, round(peso * factor), descripcion);
    }

    private String tendencia(List<Calificacion> calificaciones) {
        if (calificaciones.size() < 2) return "SIN_DATOS";
        List<Calificacion> ultimas = calificaciones.subList(Math.max(0, calificaciones.size() - 3), calificaciones.size());
        BigDecimal diferencia = ultimas.get(ultimas.size() - 1).getNotaNumerica().subtract(ultimas.get(0).getNotaNumerica());
        if (diferencia.compareTo(BigDecimal.valueOf(5)) >= 0) return "SUBIENDO";
        if (diferencia.compareTo(BigDecimal.valueOf(-5)) <= 0) return "BAJANDO";
        return "ESTABLE";
    }

    private List<String> recomendaciones(UUID idInstitucion, Metricas m, String nivel) {
        List<String> result = new ArrayList<>();
        if (m.porcentajeAsistencia.compareTo(BigDecimal.valueOf(configuracionService.getInt(idInstitucion, "PORCENTAJE_ASISTENCIA_MINIMO"))) < 0) result.add("Contactar al tutor para revisar las inasistencias.");
        if (m.promedio.compareTo(BigDecimal.valueOf(configuracionService.getInt(idInstitucion, "NOTA_MINIMA_APROBACION"))) < 0) result.add("Asignar refuerzo académico y revisar las actividades con menor nota.");
        if ("BAJANDO".equals(m.tendencia)) result.add("Programar una entrevista breve para identificar la causa de la tendencia descendente.");
        if (m.evaluacionesPendientes > 0) result.add("Completar o verificar el registro de evaluaciones pendientes.");
        if ("CRITICO".equals(nivel)) result.add("Derivar el caso a coordinación académica para seguimiento prioritario.");
        return result;
    }

    private AnalisisRiesgoResponse response(List<RiesgoEstudianteDetalleResponse> estudiantes, List<ResumenRiesgoParalelo> comparativa) {
        Map<String, Long> distribucion = Arrays.stream(new String[]{"BAJO", "MEDIO", "ALTO", "CRITICO"})
                .collect(Collectors.toMap(n -> n, n -> estudiantes.stream().filter(e -> n.equals(e.nivelRiesgo())).count(), (a, b) -> a, LinkedHashMap::new));
        return new AnalisisRiesgoResponse(estudiantes.size(), distribucion, comparativa,
                estudiantes.stream().sorted(Comparator.comparing(RiesgoEstudianteDetalleResponse::score).reversed()).toList(), Instant.now());
    }

    private ResumenRiesgoParalelo resumenParalelo(UUID idParalelo, List<RiesgoEstudianteDetalleResponse> estudiantes) {
        int enRiesgo = (int) estudiantes.stream().filter(e -> "ALTO".equals(e.nivelRiesgo()) || "CRITICO".equals(e.nivelRiesgo())).count();
        String nombre = estudiantes.isEmpty() ? paraleloRepository.findById(idParalelo).map(Paralelo::getNombre).orElse("Paralelo") : estudiantes.getFirst().nombreParalelo();
        BigDecimal promedio = estudiantes.isEmpty() ? BigDecimal.ZERO : estudiantes.stream().map(RiesgoEstudianteDetalleResponse::score)
                .reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(estudiantes.size()), 2, RoundingMode.HALF_UP);
        return new ResumenRiesgoParalelo(idParalelo, nombre, estudiantes.size(), enRiesgo,
                estudiantes.isEmpty() ? BigDecimal.ZERO : BigDecimal.valueOf(enRiesgo * 100.0 / estudiantes.size()).setScale(2, RoundingMode.HALF_UP), promedio);
    }

    private String nivel(BigDecimal score) {
        if (score.compareTo(BigDecimal.valueOf(76)) >= 0) return "CRITICO";
        if (score.compareTo(BigDecimal.valueOf(51)) >= 0) return "ALTO";
        if (score.compareTo(BigDecimal.valueOf(26)) >= 0) return "MEDIO";
        return "BAJO";
    }

    private String serializarFactores(List<FactorContribuyente> factores) {
        return factores.stream().map(f -> "{\"nombre\":\"" + f.nombre() + "\",\"impacto\":" + f.impacto() + "}")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private double round(double value) { return Math.round(value * 100.0) / 100.0; }

    private record Metricas(BigDecimal porcentajeAsistencia, BigDecimal promedio, String tendencia,
                            int evaluacionesPendientes, int materiasBajoRendimiento, List<EvolucionNotaResponse> evolucion) {}
}
