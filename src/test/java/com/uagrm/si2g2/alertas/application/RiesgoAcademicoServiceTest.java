package com.uagrm.si2g2.alertas.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uagrm.si2g2.academico.domain.GestionAcademica;
import com.uagrm.si2g2.academico.domain.GestionAcademicaRepository;
import com.uagrm.si2g2.alertas.domain.*;
import com.uagrm.si2g2.alertas.dto.FactorContribuyente;
import com.uagrm.si2g2.asignacion.domain.AsignacionDocente;
import com.uagrm.si2g2.asignacion.domain.AsignacionDocenteRepository;
import com.uagrm.si2g2.asistencia.domain.AsistenciaDetalle;
import com.uagrm.si2g2.asistencia.domain.AsistenciaDetalleRepository;
import com.uagrm.si2g2.asistencia.domain.AsistenciaRegistro;
import com.uagrm.si2g2.asistencia.domain.AsistenciaRegistroRepository;
import com.uagrm.si2g2.auth.domain.Rol;
import com.uagrm.si2g2.auth.domain.Usuario;
import com.uagrm.si2g2.calificacion.domain.CalificacionRepository;
import com.uagrm.si2g2.calificacion.domain.Calificacion;
import com.uagrm.si2g2.calificacion.domain.Evaluacion;
import com.uagrm.si2g2.calificacion.domain.EvaluacionRepository;
import com.uagrm.si2g2.calificacion.domain.PeriodoEvaluacion;
import com.uagrm.si2g2.calificacion.domain.PeriodoEvaluacionRepository;
import com.uagrm.si2g2.calificacion.domain.ActividadEvaluativa;
import com.uagrm.si2g2.calificacion.domain.ActividadEvaluativaRepository;
import com.uagrm.si2g2.calificacion.domain.CalificacionActividad;
import com.uagrm.si2g2.calificacion.domain.CalificacionActividadRepository;
import com.uagrm.si2g2.calificacion.domain.CalificacionSer;
import com.uagrm.si2g2.calificacion.domain.CalificacionSerRepository;
import com.uagrm.si2g2.calificacion.domain.AutoevaluacionTrimestral;
import com.uagrm.si2g2.calificacion.domain.AutoevaluacionTrimestralRepository;
import com.uagrm.si2g2.curso.domain.CursoRepository;
import com.uagrm.si2g2.curso.domain.Paralelo;
import com.uagrm.si2g2.curso.domain.ParaleloRepository;
import com.uagrm.si2g2.docente.domain.Docente;
import com.uagrm.si2g2.docente.domain.DocenteRepository;
import com.uagrm.si2g2.dimension.domain.PeriodoDimensionRepository;
import com.uagrm.si2g2.estudiante.domain.EstudianteRepository;
import com.uagrm.si2g2.estudiante.domain.Estudiante;
import com.uagrm.si2g2.inscripcion.domain.Inscripcion;
import com.uagrm.si2g2.inscripcion.domain.InscripcionRepository;
import com.uagrm.si2g2.institucion.application.ConfiguracionService;
import com.uagrm.si2g2.materia.domain.Materia;
import com.uagrm.si2g2.materia.domain.MateriaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiesgoAcademicoServiceTest {
    @Mock private InscripcionRepository inscripcionRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private ParaleloRepository paraleloRepository;
    @Mock private CursoRepository cursoRepository;
    @Mock private MateriaRepository materiaRepository;
    @Mock private GestionAcademicaRepository gestionRepository;
    @Mock private AsignacionDocenteRepository asignacionRepository;
    @Mock private DocenteRepository docenteRepository;
    @Mock private AsistenciaRegistroRepository asistenciaRegistroRepository;
    @Mock private AsistenciaDetalleRepository asistenciaDetalleRepository;
    @Mock private EvaluacionRepository evaluacionRepository;
    @Mock private PeriodoEvaluacionRepository periodoEvaluacionRepository;
    @Mock private CalificacionRepository calificacionRepository;
    @Mock private ActividadEvaluativaRepository actividadEvaluativaRepository;
    @Mock private CalificacionActividadRepository calificacionActividadRepository;
    @Mock private CalificacionSerRepository calificacionSerRepository;
    @Mock private AutoevaluacionTrimestralRepository autoevaluacionRepository;
    @Mock private PeriodoDimensionRepository periodoDimensionRepository;
    @Mock private AlertaRiesgoRepository alertaRepository;
    @Mock private RecomendacionIaRepository recomendacionRepository;
    @Mock private AlertaRiesgoSeguimientoRepository seguimientoRepository;
    @Mock private ConfiguracionService configuracionService;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks private RiesgoAcademicoService service;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validatesParallelBelongsToManagementBeforeWriting() {
        UUID institution = UUID.randomUUID();
        UUID management = UUID.randomUUID();
        UUID otherManagement = UUID.randomUUID();
        UUID parallel = UUID.randomUUID();
        authenticate(institution, "ADMIN_INSTITUCION");
        when(gestionRepository.findByIdAndIdInstitucion(management, institution))
                .thenReturn(Optional.of(GestionAcademica.builder().id(management).idInstitucion(institution).build()));
        when(paraleloRepository.findByIdAndIdInstitucion(parallel, institution))
                .thenReturn(Optional.of(Paralelo.builder().id(parallel).idInstitucion(institution)
                        .idGestionAcademica(otherManagement).build()));

        assertThrows(EntityNotFoundException.class, () -> service.analizarParalelo(parallel, management));

        verify(alertaRepository, never()).save(any());
        verify(recomendacionRepository, never()).saveAll(any());
    }

    @Test
    void institutionalSummaryDoesNotPersistAlerts() {
        UUID institution = UUID.randomUUID();
        UUID management = UUID.randomUUID();
        authenticate(institution, "DIRECTOR");
        when(gestionRepository.findByIdAndIdInstitucion(management, institution))
                .thenReturn(Optional.of(GestionAcademica.builder().id(management).idInstitucion(institution).build()));
        when(paraleloRepository.findAllByIdInstitucion(institution)).thenReturn(List.of());

        var result = service.resumenInstitucion(management);

        assertEquals(0, result.totalEstudiantes());
        verify(alertaRepository, never()).save(any());
        verify(recomendacionRepository, never()).saveAll(any());
        verify(recomendacionRepository, never()).deleteAllByIdAlertaRiesgo(any());
    }

    @Test
    void insufficientDataPreservesExistingAlertState() {
        UUID institution = UUID.randomUUID();
        UUID management = UUID.randomUUID();
        UUID parallel = UUID.randomUUID();
        UUID course = UUID.randomUUID();
        UUID student = UUID.randomUUID();
        UUID enrollmentId = UUID.randomUUID();
        authenticate(institution, "DIRECTOR");
        Paralelo paralelo = Paralelo.builder().id(parallel).idInstitucion(institution)
                .idGestionAcademica(management).idCurso(course).nombre("A").estado("ACTIVO").build();
        Inscripcion enrollment = Inscripcion.builder().id(enrollmentId).idInstitucion(institution)
                .idEstudiante(student).idGestion(management).idParalelo(parallel)
                .fechaInscripcion(LocalDate.now()).estado("ACTIVA").build();
        Estudiante estudiante = Estudiante.builder().id(student).idInstitucion(institution)
                .codigoEstudiante("E-2").nombres("Luis").apellidos("Perez").build();
        AlertaRiesgo existente = AlertaRiesgo.builder().id(UUID.randomUUID()).idInstitucion(institution)
                .idEstudiante(student).idGestionAcademica(management).nivelRiesgo("ALTO")
                .scoreIa(new BigDecimal("0.6200")).porcentajeAsistencia(new BigDecimal("70.00"))
                .promedioCalificaciones(new BigDecimal("45.00"))
                .estadoAlerta("EN_SEGUIMIENTO").activa(true).build();
        when(gestionRepository.findByIdAndIdInstitucion(management, institution))
                .thenReturn(Optional.of(GestionAcademica.builder().id(management).idInstitucion(institution).build()));
        when(paraleloRepository.findByIdAndIdInstitucion(parallel, institution)).thenReturn(Optional.of(paralelo));
        when(inscripcionRepository.findAllByIdInstitucionAndIdParalelo(institution, parallel))
                .thenReturn(List.of(enrollment));
        when(estudianteRepository.findByIdAndIdInstitucionForUpdate(student, institution))
                .thenReturn(Optional.of(estudiante));
        when(cursoRepository.findByIdAndIdInstitucion(course, institution)).thenReturn(Optional.empty());
        when(asignacionRepository.findAllByIdInstitucionAndIdParalelo(institution, parallel)).thenReturn(List.of());
        when(periodoEvaluacionRepository.findAllByIdInstitucionAndIdGestionAcademica(institution, management))
                .thenReturn(List.of());
        when(calificacionRepository.findAllByIdInstitucionAndIdInscripcion(institution, enrollmentId))
                .thenReturn(List.of());
        when(alertaRepository.findActivaForUpdate(
                institution, student, management)).thenReturn(Optional.of(existente));

        var result = service.analizarParalelo(parallel, management);

        assertEquals("DATOS_INSUFICIENTES", result.estudiantes().getFirst().estadoAnalisis());
        assertEquals("EN_SEGUIMIENTO", result.estudiantes().getFirst().estadoAlerta());
        assertEquals(new BigDecimal("62.00"), result.estudiantes().getFirst().score());
        assertEquals(new BigDecimal("70.00"), result.estudiantes().getFirst().porcentajeAsistencia());
        assertTrue(existente.getActiva());
        assertFalse(existente.getDatosVigentes());
        assertEquals(0, result.comparativaParalelos().getFirst().totalConDatos());
        assertEquals(1, result.comparativaParalelos().getFirst().totalSinDatos());
        assertEquals(BigDecimal.ZERO, result.comparativaParalelos().getFirst().scorePromedio());
        verify(alertaRepository).save(existente);
    }

    @Test
    void calculatesWeightedGradesAndOnlyOverdueUngradedEvaluationsAsPending() {
        UUID institution = UUID.randomUUID();
        UUID management = UUID.randomUUID();
        UUID parallel = UUID.randomUUID();
        UUID student = UUID.randomUUID();
        UUID enrollmentId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        UUID subject = UUID.randomUUID();
        LocalDate enrollmentDate = LocalDate.now().minusMonths(1);
        authenticate(institution, "DIRECTOR");
        Inscripcion enrollment = Inscripcion.builder().id(enrollmentId).idInstitucion(institution)
                .idEstudiante(student).idGestion(management).idParalelo(parallel)
                .fechaInscripcion(enrollmentDate).estado("ACTIVA").build();
        when(gestionRepository.findByIdAndIdInstitucion(management, institution))
                .thenReturn(Optional.of(GestionAcademica.builder().id(management).idInstitucion(institution).build()));
        when(inscripcionRepository.findAllByIdInstitucionAndIdEstudiante(institution, student))
                .thenReturn(List.of(enrollment));
        when(estudianteRepository.findByIdAndIdInstitucion(student, institution)).thenReturn(Optional.of(
                Estudiante.builder().id(student).idInstitucion(institution).codigoEstudiante("E-1")
                        .nombres("Ana").apellidos("Lopez").build()));
        when(paraleloRepository.findByIdAndIdInstitucion(parallel, institution)).thenReturn(Optional.of(
                Paralelo.builder().id(parallel).idInstitucion(institution).idGestionAcademica(management)
                        .idCurso(UUID.randomUUID()).nombre("A").build()));
        AsignacionDocente assignment = AsignacionDocente.builder().id(assignmentId).idInstitucion(institution)
                .idMateria(subject).idParalelo(parallel).idGestion(management).estado("ACTIVA").build();
        when(asignacionRepository.findAllByIdInstitucionAndIdParalelo(institution, parallel))
                .thenReturn(List.of(assignment));
        UUID attendanceId = UUID.randomUUID();
        when(asistenciaRegistroRepository.findAllByIdInstitucionAndIdAsignacionDocente(institution, assignmentId))
                .thenReturn(List.of(AsistenciaRegistro.builder().id(attendanceId).idInstitucion(institution)
                        .fecha(LocalDate.now()).estado("REGISTRADA").build()));
        when(asistenciaDetalleRepository.findAllByIdAsistenciaRegistroIn(List.of(attendanceId)))
                .thenReturn(List.of(AsistenciaDetalle.builder().idAsistenciaRegistro(attendanceId)
                        .idInscripcion(enrollmentId).estadoAsistencia("JUSTIFICADO").build()));

        Instant base = Instant.now().minusSeconds(1000);
        when(periodoEvaluacionRepository.findAllByIdInstitucionAndIdGestionAcademica(institution, management))
                .thenReturn(List.of(PeriodoEvaluacion.builder().numeroPeriodo(1)
                        .fechaInicio(LocalDate.now().minusMonths(1))
                        .fechaFin(LocalDate.now().minusDays(1)).build()));
        Evaluacion lowWeight = evaluation(subject, 1, "E1", "10", base);
        Evaluacion highWeight = evaluation(subject, 1, "E2", "70", base.plusSeconds(1));
        Evaluacion literal = evaluation(subject, 1, "Literal", "10", base.plusSeconds(2));
        literal.setEscala("LITERAL");
        Evaluacion overdue = evaluation(subject, 1, "Vencida", "10", base.plusSeconds(3));
        when(evaluacionRepository.findAllByIdInstitucionAndIdMateria(institution, subject))
                .thenReturn(List.of(lowWeight, highWeight, literal, overdue));
        when(calificacionRepository.findAllByIdInstitucionAndIdInscripcion(institution, enrollmentId))
                .thenReturn(List.of(
                        Calificacion.builder().idEvaluacion(lowWeight.getId()).idInscripcion(enrollmentId)
                                .notaNumerica(BigDecimal.ZERO).build(),
                        Calificacion.builder().idEvaluacion(highWeight.getId()).idInscripcion(enrollmentId)
                                .notaNumerica(BigDecimal.valueOf(100)).build(),
                        Calificacion.builder().idEvaluacion(literal.getId()).idInscripcion(enrollmentId)
                                .notaLiteral("A").build()));
        when(alertaRepository.findByIdInstitucionAndIdEstudianteAndIdGestionAcademicaAndActivaTrue(
                institution, student, management)).thenReturn(Optional.empty());

        var result = service.detalleEstudiante(student, management);

        assertEquals(new BigDecimal("77.78"), result.promedioCalificaciones());
        assertEquals(new BigDecimal("100.00"), result.porcentajeAsistencia());
        assertEquals(1, result.evaluacionesPendientes());
        assertEquals("CALCULADO", result.estadoAnalisis());
        verify(alertaRepository, never()).save(any());
    }

    @Test
    void usesDimensionalConsolidatedGradeAsPrimaryRiskEvidence() {
        UUID institution = UUID.randomUUID();
        UUID management = UUID.randomUUID();
        UUID parallel = UUID.randomUUID();
        UUID student = UUID.randomUUID();
        UUID enrollmentId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        UUID subject = UUID.randomUUID();
        UUID periodId = UUID.randomUUID();
        UUID saberActivity = UUID.randomUUID();
        UUID hacerActivity = UUID.randomUUID();
        authenticate(institution, "DIRECTOR");

        Inscripcion enrollment = Inscripcion.builder().id(enrollmentId).idInstitucion(institution)
                .idEstudiante(student).idGestion(management).idParalelo(parallel)
                .fechaInscripcion(LocalDate.now().minusMonths(1)).estado("ACTIVA").build();
        when(gestionRepository.findByIdAndIdInstitucion(management, institution))
                .thenReturn(Optional.of(GestionAcademica.builder().id(management).idInstitucion(institution).build()));
        when(inscripcionRepository.findAllByIdInstitucionAndIdEstudiante(institution, student))
                .thenReturn(List.of(enrollment));
        when(estudianteRepository.findByIdAndIdInstitucion(student, institution)).thenReturn(Optional.of(
                Estudiante.builder().id(student).idInstitucion(institution).codigoEstudiante("E-DIM")
                        .nombres("Ana").apellidos("Dimensional").build()));
        when(paraleloRepository.findByIdAndIdInstitucion(parallel, institution)).thenReturn(Optional.of(
                Paralelo.builder().id(parallel).idInstitucion(institution).idGestionAcademica(management)
                        .idCurso(UUID.randomUUID()).nombre("A").build()));
        AsignacionDocente assignment = AsignacionDocente.builder().id(assignmentId).idInstitucion(institution)
                .idMateria(subject).idParalelo(parallel).idGestion(management).estado("ACTIVA").build();
        when(asignacionRepository.findAllByIdInstitucionAndIdParalelo(institution, parallel))
                .thenReturn(List.of(assignment));
        UUID attendanceId = UUID.randomUUID();
        when(asistenciaRegistroRepository.findAllByIdInstitucionAndIdAsignacionDocente(institution, assignmentId))
                .thenReturn(List.of(AsistenciaRegistro.builder().id(attendanceId).idInstitucion(institution)
                        .fecha(LocalDate.now()).estado("REGISTRADA").build()));
        when(asistenciaDetalleRepository.findAllByIdAsistenciaRegistroIn(List.of(attendanceId)))
                .thenReturn(List.of(AsistenciaDetalle.builder().idAsistenciaRegistro(attendanceId)
                        .idInscripcion(enrollmentId).estadoAsistencia("PRESENTE").build()));

        PeriodoEvaluacion period = PeriodoEvaluacion.builder().id(periodId).numeroPeriodo(1)
                .fechaInicio(LocalDate.now().minusMonths(1)).fechaFin(LocalDate.now().plusDays(1))
                .pesoSer(10).pesoSaber(45).pesoHacer(40).pesoAuto(5).build();
        when(periodoEvaluacionRepository.findAllByIdInstitucionAndIdGestionAcademica(institution, management))
                .thenReturn(List.of(period));
        when(actividadEvaluativaRepository.findAllByIdInstitucionAndIdPeriodoEvaluacionIn(
                institution, List.of(periodId))).thenReturn(List.of(
                        ActividadEvaluativa.builder().id(saberActivity).idInstitucion(institution)
                                .idPeriodoEvaluacion(periodId).idMateria(subject).dimension("SABER")
                                .estado("PUBLICADA").build(),
                        ActividadEvaluativa.builder().id(hacerActivity).idInstitucion(institution)
                                .idPeriodoEvaluacion(periodId).idMateria(subject).dimension("HACER")
                                .estado("PUBLICADA").build()));
        when(calificacionActividadRepository.findAllByIdEstudianteAndIdActividadIn(
                student, List.of(saberActivity, hacerActivity))).thenReturn(List.of(
                        CalificacionActividad.builder().idActividad(saberActivity).idEstudiante(student)
                                .notaObtenida(new BigDecimal("80")).build(),
                        CalificacionActividad.builder().idActividad(hacerActivity).idEstudiante(student)
                                .notaObtenida(new BigDecimal("70")).build()));
        when(calificacionSerRepository.findAllByIdInstitucionAndIdEstudianteAndIdPeriodoEvaluacionIn(
                institution, student, List.of(periodId))).thenReturn(List.of(
                        CalificacionSer.builder().idPeriodoEvaluacion(periodId).idMateria(subject)
                                .idEstudiante(student).notaSer(new BigDecimal("8")).build()));
        when(autoevaluacionRepository.findAllByIdInstitucionAndIdEstudianteAndIdPeriodoEvaluacionIn(
                institution, student, List.of(periodId))).thenReturn(List.of(
                        AutoevaluacionTrimestral.builder().idPeriodoEvaluacion(periodId).idMateria(subject)
                                .idEstudiante(student).notaAutoevaluacion(new BigDecimal("4")).build()));
        when(periodoDimensionRepository.findAllByIdPeriodoEvaluacionIn(List.of(periodId))).thenReturn(List.of());
        when(materiaRepository.findAllByIdInAndIdInstitucionAndEstado(
                Set.of(subject), institution, "ACTIVO")).thenReturn(List.of(
                Materia.builder().id(subject).idInstitucion(institution).nombre("Matemática").build()));
        when(configuracionService.getInt(institution, "NOTA_MINIMA_APROBACION")).thenReturn(51);
        when(alertaRepository.findByIdInstitucionAndIdEstudianteAndIdGestionAcademicaAndActivaTrue(
                institution, student, management)).thenReturn(Optional.empty());

        var result = service.detalleEstudiante(student, management);

        assertEquals(new BigDecimal("76.00"), result.promedioCalificaciones());
        assertEquals("CALCULADO", result.estadoAnalisis());
        assertEquals(1, result.evolucionNotas().size());
        assertEquals(1, result.desgloseMaterias().size());
        var desglose = result.desgloseMaterias().getFirst();
        assertEquals(subject, desglose.idMateria());
        assertEquals("Matemática", desglose.nombreMateria());
        assertEquals(new BigDecimal("76.00"), desglose.notaTotal());
        assertTrue(desglose.aprobada());
        assertEquals(List.of("SABER", "HACER", "SER", "AUTOEVALUACION"),
                desglose.dimensiones().stream().map(dimension -> dimension.dimension()).toList());
        assertEquals(List.of(
                        new BigDecimal("36.00"), new BigDecimal("28.00"),
                        new BigDecimal("8.00"), new BigDecimal("4.00")),
                desglose.dimensiones().stream().map(dimension -> dimension.puntaje()).toList());
        assertEquals(List.of(
                        new BigDecimal("45.00"), new BigDecimal("40.00"),
                        new BigDecimal("10.00"), new BigDecimal("5.00")),
                desglose.dimensiones().stream().map(dimension -> dimension.peso()).toList());
        assertEquals(1, desglose.notasPorPeriodo().size());
        assertEquals(1, desglose.notasPorPeriodo().getFirst().periodo());
        assertEquals(new BigDecimal("76.00"), desglose.notasPorPeriodo().getFirst().nota());
        verify(calificacionRepository, never()).findAllByIdInstitucionAndIdInscripcion(any(), any());
    }

    @Test
    void serializesCompleteRiskFactors() throws Exception {
        FactorContribuyente factor = new FactorContribuyente("Asistencia", 30, 72.5, 3.0, "Detalle");

        String json = ReflectionTestUtils.invokeMethod(service, "serializarFactores", List.of(factor));
        var node = objectMapper.readTree(json).get(0);

        assertEquals("Asistencia", node.get("nombre").asText());
        assertEquals(30, node.get("peso").asDouble());
        assertEquals(72.5, node.get("valor").asDouble());
        assertEquals(3.0, node.get("impacto").asDouble());
        assertEquals("Detalle", node.get("descripcion").asText());
    }

    @Test
    void normalizesOpenPeriodOnlyByGradedWeightAndExpiresMissingWeightAsZero() {
        UUID subject = UUID.randomUUID();
        Evaluacion graded = evaluation(subject, 1, "Calificada", "20", Instant.now());
        Evaluacion missing = evaluation(subject, 1, "Sin nota", "80", Instant.now());
        Calificacion grade = Calificacion.builder().idEvaluacion(graded.getId())
                .notaNumerica(new BigDecimal("80")).build();
        LocalDate today = LocalDate.now();
        PeriodoEvaluacion open = PeriodoEvaluacion.builder().numeroPeriodo(1)
                .fechaInicio(today.minusDays(1)).fechaFin(today.plusDays(1)).build();
        PeriodoEvaluacion expired = PeriodoEvaluacion.builder().numeroPeriodo(1)
                .fechaInicio(today.minusDays(2)).fechaFin(today.minusDays(1)).build();

        Map<?, BigDecimal> openResult = ReflectionTestUtils.invokeMethod(service,
                "consolidarPorMateriaPeriodo", List.of(graded, missing), List.of(grade), Map.of(1, open), today);
        Map<?, BigDecimal> expiredResult = ReflectionTestUtils.invokeMethod(service,
                "consolidarPorMateriaPeriodo", List.of(graded, missing), List.of(grade), Map.of(1, expired), today);

        assertNotNull(openResult);
        assertNotNull(expiredResult);
        assertEquals(new BigDecimal("80.00"), openResult.values().iterator().next());
        assertEquals(new BigDecimal("16.00"), expiredResult.values().iterator().next());
    }

    private Evaluacion evaluation(UUID subject, int period, String name, String weight, Instant createdAt) {
        return Evaluacion.builder().id(UUID.randomUUID()).idMateria(subject).periodo(period).nombre(name)
                .ponderacion(new BigDecimal(weight)).estado("ABIERTA").creadoEn(createdAt).build();
    }

    private UUID authenticate(UUID institution, String role) {
        UUID userId = UUID.randomUUID();
        Usuario user = Usuario.builder().id(userId).idInstitucion(institution).correo("test@example.com")
                .hashContrasena("secret").nombres("Test").apellidos("User")
                .roles(Set.of(Rol.builder().codigo(role).nombre(role).esGlobal(true).build())).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        return userId;
    }
}
