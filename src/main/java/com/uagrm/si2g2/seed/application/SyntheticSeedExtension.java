package com.uagrm.si2g2.seed.application;

import com.uagrm.si2g2.academico.domain.GestionAcademica;
import com.uagrm.si2g2.academico.domain.GestionAcademicaRepository;
import com.uagrm.si2g2.asignacion.domain.AsignacionDocente;
import com.uagrm.si2g2.asignacion.domain.AsignacionDocenteRepository;
import com.uagrm.si2g2.asistencia.domain.AsistenciaDetalle;
import com.uagrm.si2g2.asistencia.domain.AsistenciaDetalleRepository;
import com.uagrm.si2g2.asistencia.domain.AsistenciaRegistro;
import com.uagrm.si2g2.asistencia.domain.AsistenciaRegistroRepository;
import com.uagrm.si2g2.aula.domain.Aula;
import com.uagrm.si2g2.aula.domain.AulaRepository;
import com.uagrm.si2g2.calificacion.domain.ActividadEvaluativa;
import com.uagrm.si2g2.calificacion.domain.ActividadEvaluativaRepository;
import com.uagrm.si2g2.calificacion.domain.AutoevaluacionTrimestral;
import com.uagrm.si2g2.calificacion.domain.AutoevaluacionTrimestralRepository;
import com.uagrm.si2g2.calificacion.domain.CalificacionActividad;
import com.uagrm.si2g2.calificacion.domain.CalificacionActividadRepository;
import com.uagrm.si2g2.calificacion.domain.CalificacionSer;
import com.uagrm.si2g2.calificacion.domain.CalificacionSerRepository;
import com.uagrm.si2g2.calificacion.domain.EvaluacionMateria;
import com.uagrm.si2g2.calificacion.domain.EvaluacionMateriaRepository;
import com.uagrm.si2g2.calificacion.domain.PeriodoEvaluacion;
import com.uagrm.si2g2.calificacion.domain.PeriodoEvaluacionRepository;
import com.uagrm.si2g2.curso.domain.Curso;
import com.uagrm.si2g2.curso.domain.CursoRepository;
import com.uagrm.si2g2.docente.domain.Docente;
import com.uagrm.si2g2.docente.domain.DocenteRepository;
import com.uagrm.si2g2.estudiante.domain.Estudiante;
import com.uagrm.si2g2.estudiante.domain.EstudianteRepository;
import com.uagrm.si2g2.horario.domain.HorarioClase;
import com.uagrm.si2g2.horario.domain.HorarioClaseRepository;
import com.uagrm.si2g2.inscripcion.domain.Inscripcion;
import com.uagrm.si2g2.inscripcion.domain.InscripcionRepository;
import com.uagrm.si2g2.materia.domain.Materia;
import com.uagrm.si2g2.materia.domain.MateriaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyntheticSeedExtension {

    private static final String[] DIAS_SEMANA = {"LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES"};
    private static final String[] ESTADOS_ASISTENCIA = {"PRESENTE", "AUSENTE", "TARDANZA", "JUSTIFICADO"};
    private static final double[] PESOS_ASISTENCIA = {0.80, 0.08, 0.07, 0.05};
    private static final String[] TIPOS_EVALUACION = {"PARCIAL", "TRABAJO_PRACTICO"};
    private static final BigDecimal PONDERACION_PARCIAL = new BigDecimal("40");
    private static final BigDecimal PONDERACION_TP = new BigDecimal("60");
    private static final int SEMANAS_ASISTENCIA = 8;
    private static final int HORARIOS_POR_ASIGNACION = 2;

    private final GestionAcademicaRepository gestionAcademicaRepository;
    private final CursoRepository cursoRepository;
    private final MateriaRepository materiaRepository;
    private final AulaRepository aulaRepository;
    private final DocenteRepository docenteRepository;
    private final EstudianteRepository estudianteRepository;
    private final InscripcionRepository inscripcionRepository;
    private final AsignacionDocenteRepository asignacionDocenteRepository;
    private final PeriodoEvaluacionRepository periodoEvaluacionRepository;
    private final EvaluacionMateriaRepository evaluacionMateriaRepository;
    private final ActividadEvaluativaRepository actividadEvaluativaRepository;
    private final CalificacionActividadRepository calificacionActividadRepository;
    private final CalificacionSerRepository calificacionSerRepository;
    private final AutoevaluacionTrimestralRepository autoevaluacionTrimestralRepository;
    private final HorarioClaseRepository horarioClaseRepository;
    private final AsistenciaRegistroRepository asistenciaRegistroRepository;
    private final AsistenciaDetalleRepository asistenciaDetalleRepository;

    @Transactional
    public void seedPeriodosYEvaluaciones(UUID idInstitucion) {
        GestionAcademica gestion = gestionAcademicaRepository.findByIdInstitucionAndActivaTrue(idInstitucion).orElse(null);
        if (gestion == null) {
            log.warn("No hay gestion activa para {}, saltando seed de evaluaciones", idInstitucion);
            return;
        }

        List<PeriodoEvaluacion> periodos = seedPeriodosEvaluacion(idInstitucion, gestion);
        log.info("Periodos creados: {}", periodos.size());

        List<Materia> materias = materiaRepository.findAllByIdInstitucion(idInstitucion);
        seedEvaluacionesMateria(idInstitucion, materias, periodos);
        log.info("Evaluaciones por materia creadas");
    }

    @Transactional
    public void seedActividadesYCalificaciones(UUID idInstitucion) {
        List<Materia> materias = materiaRepository.findAllByIdInstitucion(idInstitucion);
        List<Docente> docentes = docenteRepository.findAllByIdInstitucion(idInstitucion);
        List<Estudiante> estudiantes = estudianteRepository.findAllByIdInstitucion(idInstitucion);
        List<PeriodoEvaluacion> periodos = periodoEvaluacionRepository
                .findAllByIdInstitucionAndIdGestionAcademica(idInstitucion,
                        gestionAcademicaRepository.findByIdInstitucionAndActivaTrue(idInstitucion).orElseThrow().getId());
        Random rng = new Random(2026);

        int totalActividades = 0;
        int totalCalificaciones = 0;

        for (Materia materia : materias) {
            Docente docente = docentes.get(Math.abs(materia.getCodigo().hashCode()) % docentes.size());
            for (PeriodoEvaluacion pe : periodos) {
                for (String dim : new String[]{"SABER", "HACER"}) {
                    String nombreAct = dim + " - " + materia.getNombre() + " T" + pe.getNumeroPeriodo();
                    if (actividadEvaluativaRepository
                            .existsByIdInstitucionAndIdPeriodoEvaluacionAndNombreActividadIgnoreCase(
                                    idInstitucion, pe.getId(), nombreAct)) continue;

                    LocalDate fechaAct = pe.getFechaInicio().plusDays(dim.equals("SABER") ? 5 : 20);
                    ActividadEvaluativa act = actividadEvaluativaRepository.save(
                            ActividadEvaluativa.builder()
                                    .idInstitucion(idInstitucion)
                                    .idPeriodoEvaluacion(pe.getId())
                                    .idMateria(materia.getId())
                                    .idDocente(docente.getId())
                                    .nombreActividad(nombreAct)
                                    .dimension(dim)
                                    .fechaActividad(fechaAct)
                                    .puntajeMaximo(dim.equals("SABER") ? 40 : 45)
                                    .estado("PUBLICADA")
                                    .build());
                    totalActividades++;

                    int pmax = dim.equals("SABER") ? 40 : 45;
                    for (Estudiante est : estudiantes) {
                        if (calificacionActividadRepository
                                .findByIdActividadAndIdEstudiante(act.getId(), est.getId()).isPresent()) continue;
                        double base = pmax * 0.3;
                        double nota = base + rng.nextDouble() * (pmax - base);
                        calificacionActividadRepository.save(CalificacionActividad.builder()
                                .idInstitucion(idInstitucion)
                                .idActividad(act.getId())
                                .idEstudiante(est.getId())
                                .notaObtenida(BigDecimal.valueOf(nota).setScale(2, BigDecimal.ROUND_HALF_UP))
                                .estado("REGISTRADA")
                                .build());
                        totalCalificaciones++;
                    }
                }
            }
        }
        log.info("Actividades creadas: {}, calificaciones: {}", totalActividades, totalCalificaciones);
    }

    @Transactional
    public void seedCalificacionSerYAuto(UUID idInstitucion) {
        List<Materia> materias = materiaRepository.findAllByIdInstitucion(idInstitucion);
        List<Estudiante> estudiantes = estudianteRepository.findAllByIdInstitucion(idInstitucion);
        List<PeriodoEvaluacion> periodos = periodoEvaluacionRepository
                .findAllByIdInstitucionAndIdGestionAcademica(idInstitucion,
                        gestionAcademicaRepository.findByIdInstitucionAndActivaTrue(idInstitucion).orElseThrow().getId());
        Random rng = new Random(2026);
        int totalSer = 0, totalAuto = 0;

        for (Estudiante est : estudiantes) {
            for (Materia materia : materias) {
                for (PeriodoEvaluacion pe : periodos) {
                    if (calificacionSerRepository
                            .findByIdPeriodoEvaluacionAndIdEstudianteAndIdMateria(pe.getId(), est.getId(), materia.getId())
                            .isEmpty()) {
                        double notaSer = 5 + rng.nextDouble() * 5;
                        calificacionSerRepository.save(CalificacionSer.builder()
                                .idInstitucion(idInstitucion)
                                .idPeriodoEvaluacion(pe.getId())
                                .idEstudiante(est.getId())
                                .idMateria(materia.getId())
                                .notaSer(BigDecimal.valueOf(notaSer).setScale(2, BigDecimal.ROUND_HALF_UP))
                                .estado("REGISTRADA")
                                .build());
                        totalSer++;
                    }
                    if (autoevaluacionTrimestralRepository
                            .findByIdPeriodoEvaluacionAndIdEstudianteAndIdMateria(pe.getId(), est.getId(), materia.getId())
                            .isEmpty()) {
                        double notaAuto = 2 + rng.nextDouble() * 3;
                        autoevaluacionTrimestralRepository.save(AutoevaluacionTrimestral.builder()
                                .idInstitucion(idInstitucion)
                                .idPeriodoEvaluacion(pe.getId())
                                .idEstudiante(est.getId())
                                .idMateria(materia.getId())
                                .notaAutoevaluacion(BigDecimal.valueOf(notaAuto).setScale(2, BigDecimal.ROUND_HALF_UP))
                                .estado("REGISTRADA")
                                .build());
                        totalAuto++;
                    }
                }
            }
        }
        log.info("Calificaciones SER: {}, Autoevaluaciones: {}", totalSer, totalAuto);
    }

    @Transactional
    public void seedHorarios(UUID idInstitucion) {
        List<AsignacionDocente> asignaciones = asignacionDocenteRepository.findAllByIdInstitucion(idInstitucion);
        List<Aula> aulas = aulaRepository.findAllByIdInstitucionOrderByEstadoAscNombreAsc(idInstitucion);
        LocalTime[] horasInicio = {LocalTime.of(8, 0), LocalTime.of(9, 30),
                LocalTime.of(11, 0), LocalTime.of(14, 0), LocalTime.of(15, 30)};
        LocalTime[] horasFin = {LocalTime.of(9, 15), LocalTime.of(10, 45),
                LocalTime.of(12, 15), LocalTime.of(15, 15), LocalTime.of(16, 45)};
        int aulaIdx = 0, total = 0;

        for (AsignacionDocente asig : asignaciones) {
            if (!horarioClaseRepository.findByIdAsignacionDocenteAndEstado(asig.getId(), "ACTIVO").isEmpty()) continue;
            for (int h = 0; h < HORARIOS_POR_ASIGNACION; h++) {
                Aula aula = aulas.get(aulaIdx % aulas.size());
                aulaIdx++;
                int slot = Math.abs(asig.getIdMateria().hashCode() + h) % horasInicio.length;
                String dia = DIAS_SEMANA[(Math.abs(asig.getId().hashCode()) + h) % DIAS_SEMANA.length];
                horarioClaseRepository.save(HorarioClase.builder()
                        .idInstitucion(idInstitucion)
                        .idAsignacionDocente(asig.getId())
                        .idAula(aula.getId())
                        .diaSemana(dia)
                        .horaInicio(horasInicio[slot])
                        .horaFin(horasFin[slot])
                        .build());
                total++;
            }
        }
        log.info("Horarios creados: {}", total);
    }

    @Transactional
    public void seedAsistencias(UUID idInstitucion) {
        GestionAcademica gestion = gestionAcademicaRepository.findByIdInstitucionAndActivaTrue(idInstitucion).orElse(null);
        if (gestion == null) return;
        List<AsignacionDocente> asignaciones = asignacionDocenteRepository.findAllByIdInstitucion(idInstitucion);
        Map<UUID, List<Inscripcion>> insPorParalelo = inscripcionRepository
                .findAllByIdInstitucion(idInstitucion).stream()
                .collect(Collectors.groupingBy(Inscripcion::getIdParalelo));
        Random rng = new Random(2026);
        LocalDate inicio = gestion.getFechaInicio().plusWeeks(2);
        int totalReg = 0, totalDet = 0;

        for (AsignacionDocente asig : asignaciones) {
            List<Inscripcion> inscritos = insPorParalelo.getOrDefault(asig.getIdParalelo(), List.of());
            if (inscritos.isEmpty()) continue;
            for (int s = 0; s < SEMANAS_ASISTENCIA; s++) {
                LocalDate fecha = inicio.plusWeeks(s);
                if (asistenciaRegistroRepository.existsByIdInstitucionAndIdAsignacionDocenteAndFecha(
                        idInstitucion, asig.getId(), fecha)) continue;
                AsistenciaRegistro reg = asistenciaRegistroRepository.save(AsistenciaRegistro.builder()
                        .idInstitucion(idInstitucion)
                        .idAsignacionDocente(asig.getId())
                        .fecha(fecha)
                        .build());
                totalReg++;
                for (Inscripcion ins : inscritos) {
                    if (asistenciaDetalleRepository.existsByIdAsistenciaRegistroAndIdInscripcion(reg.getId(), ins.getId()))
                        continue;
                    double roll = rng.nextDouble();
                    String estado = ESTADOS_ASISTENCIA[0];
                    double acc = 0;
                    for (int i = 0; i < ESTADOS_ASISTENCIA.length; i++) {
                        acc += PESOS_ASISTENCIA[i];
                        if (roll <= acc) { estado = ESTADOS_ASISTENCIA[i]; break; }
                    }
                    asistenciaDetalleRepository.save(AsistenciaDetalle.builder()
                            .idAsistenciaRegistro(reg.getId())
                            .idInscripcion(ins.getId())
                            .estadoAsistencia(estado)
                            .build());
                    totalDet++;
                }
            }
        }
        log.info("Registros asistencia: {}, Detalles: {}", totalReg, totalDet);
    }

    private List<PeriodoEvaluacion> seedPeriodosEvaluacion(UUID idInstitucion, GestionAcademica gestion) {
        List<PeriodoEvaluacion> periodos = new ArrayList<>();
        LocalDate inicio = gestion.getFechaInicio();
        long diasGestion = gestion.getFechaFin().toEpochDay() - inicio.toEpochDay();
        int tercio = (int) (diasGestion / 3);
        for (int p = 1; p <= 3; p++) {
            if (periodoEvaluacionRepository.findByIdInstitucionAndIdGestionAcademicaAndNumeroPeriodo(
                    idInstitucion, gestion.getId(), p).isPresent()) continue;
            LocalDate pInicio = inicio.plusDays((p - 1) * tercio);
            LocalDate pFin = (p < 3) ? inicio.plusDays(p * tercio - 1) : gestion.getFechaFin();
            periodos.add(periodoEvaluacionRepository.save(PeriodoEvaluacion.builder()
                    .idInstitucion(idInstitucion)
                    .idGestionAcademica(gestion.getId())
                    .numeroPeriodo(p)
                    .tipoPeriodo("TRIMESTRAL")
                    .fechaInicio(pInicio)
                    .fechaFin(pFin)
                    .estado("ABIERTO")
                    .build()));
        }
        return periodos;
    }

    private void seedEvaluacionesMateria(UUID idInstitucion, List<Materia> materias, List<PeriodoEvaluacion> periodos) {
        for (Materia materia : materias) {
            for (PeriodoEvaluacion pe : periodos) {
                for (int t = 0; t < TIPOS_EVALUACION.length; t++) {
                    String nombre = TIPOS_EVALUACION[t] + " " + pe.getNumeroPeriodo() + "er Trimestre";
                    if (evaluacionMateriaRepository.existsByIdInstitucionAndIdMateriaAndPeriodoAndNombreIgnoreCase(
                            idInstitucion, materia.getId(), pe.getNumeroPeriodo(), nombre)) continue;
                    evaluacionMateriaRepository.save(EvaluacionMateria.builder()
                            .idInstitucion(idInstitucion)
                            .idMateria(materia.getId())
                            .periodo(pe.getNumeroPeriodo())
                            .tipo(TIPOS_EVALUACION[t])
                            .nombre(nombre)
                            .ponderacion(t == 0 ? PONDERACION_PARCIAL : PONDERACION_TP)
                            .build());
                }
            }
        }
    }
}
