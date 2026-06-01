package com.uagrm.si2g2.reporte.application;

import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.curso.domain.Curso;
import com.uagrm.si2g2.curso.domain.Paralelo;
import com.uagrm.si2g2.curso.domain.ParaleloRepository;
import com.uagrm.si2g2.curso.domain.CursoRepository;
import com.uagrm.si2g2.docente.domain.Docente;
import com.uagrm.si2g2.docente.domain.DocenteRepository;
import com.uagrm.si2g2.materia.domain.Materia;
import com.uagrm.si2g2.materia.domain.MateriaRepository;
import com.uagrm.si2g2.tutor.domain.TutorRepository;
import com.uagrm.si2g2.reporte.dto.ReporteNaturalLanguageRequest;
import com.uagrm.si2g2.reporte.dto.ReportePreviewRequest;
import com.uagrm.si2g2.reporte.dto.ReportePreviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ReporteNaturalLanguageService {

    private final ReporteService reporteService;
    private final MateriaRepository materiaRepository;
    private final CursoRepository cursoRepository;
    private final ParaleloRepository paraleloRepository;
    private final DocenteRepository docenteRepository;
    private final TutorRepository tutorRepository;

    @Transactional(readOnly = true)
    public ReportePreviewResponse preview(ReporteNaturalLanguageRequest request) {
        Translation translation = translate(request);
        return reporteService.previewConNotas(translation.request(), translation.headerNotes(), "NL_PREVIEW", "Vista previa de reporte por lenguaje natural");
    }

    @Transactional(readOnly = true)
    public ReporteService.ExportedReport exportar(ReporteNaturalLanguageRequest request, String formato) {
        Translation translation = translate(request);
        return reporteService.exportarConNotas(translation.request(), formato, translation.headerNotes(), "NL_EXPORTAR", "Exportación de reporte por lenguaje natural");
    }

    private Translation translate(ReporteNaturalLanguageRequest request) {
        String raw = request.consulta();
        String normalized = normalize(raw);
        validateForbiddenRequests(normalized);

        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        QueryContext ctx = new QueryContext(raw, normalized, request, idInstitucion);

        detectGeneralOptions(ctx);
        detectDomainFilters(ctx);
        detectIntent(ctx);
        validateAmbiguity(ctx);

        ReportePreviewRequest translatedRequest = new ReportePreviewRequest(
                ctx.reportCode,
                ctx.filters,
                request.presentacion(),
                request.page(),
                ctx.pageSize
        );

        return new Translation(translatedRequest, List.of(
                "Consulta original: " + raw,
                "Interpretación técnica: " + ctx.technicalSummary(),
                "Modo de interpretación: lenguaje natural seguro con filtro tenant inyectado en backend"
        ));
    }

    private void validateForbiddenRequests(String normalized) {
        if (containsAny(normalized, "comparar con colegio", "comparar con institucion", "otra institucion", "otro colegio", "otra unidad educativa", "todos los colegios")) {
            throw new IllegalArgumentException("Solo puedes consultar datos de tu propia institución");
        }
        if (containsAny(normalized, "uuid de otra institucion", "institution uuid de otra")) {
            throw new IllegalArgumentException("No puedes consultar ni forzar el UUID de otra institución");
        }
        if (containsAny(normalized, "asistencia docente", "profesores que faltaron", "docentes que faltaron")) {
            throw new IllegalArgumentException("No existe un modelo de asistencia docente en este módulo para responder esa consulta");
        }
    }

    private void detectGeneralOptions(QueryContext ctx) {
        ctx.chartType = detectChartType(ctx.normalizedQuery);
        detectLimit(ctx.normalizedQuery).ifPresent(limit -> ctx.pageSize = Math.min(limit, 100));

        if (containsAny(ctx.normalizedQuery, "mejor", "mejores", "mas alto", "mayor", "top", "ranking alto")) {
            ctx.sortDirection = "DESC";
        }
        if (containsAny(ctx.normalizedQuery, "peor", "peores", "mas bajo", "menor", "ultimos", "ranking bajo")) {
            ctx.sortDirection = "ASC";
        }
    }

    private void detectDomainFilters(QueryContext ctx) {
        detectMonth(ctx.normalizedQuery).ifPresent(month -> ctx.filters.put("mes", month));
        detectQuarter(ctx.normalizedQuery).ifPresent(quarter -> ctx.filters.put("periodo", quarter));
        detectPromedioThreshold(ctx.normalizedQuery).ifPresent(value -> ctx.filters.put("promedioMaximo", value));
        detectEstadoInscripcion(ctx.normalizedQuery).ifPresent(value -> ctx.filters.put("estado", value));

        matchMateria(ctx.idInstitucion, ctx.normalizedQuery).ifPresent(m -> ctx.filters.put("idMateria", m.getId().toString()));
        matchCurso(ctx.idInstitucion, ctx.normalizedQuery).ifPresent(c -> ctx.filters.put("idCurso", c.getId().toString()));
        matchDocente(ctx.idInstitucion, ctx.normalizedQuery).ifPresent(d -> ctx.filters.put("idDocente", d.getId().toString()));
    }

    private void detectIntent(QueryContext ctx) {
        GroupIntent groupIntent = detectGenericGrouping(ctx);
        if (groupIntent != null) {
            ctx.reportCode = AgrupacionGenericaReporteHandler.CODIGO;
            ctx.filters.put("groupEntity", groupIntent.entity());
            ctx.filters.put("groupBy", groupIntent.groupBy());
            ctx.filters.put("sortDirection", ctx.sortDirection);
            return;
        }

        if (isSubjectListIntent(ctx.normalizedQuery)) {
            ctx.reportCode = MateriasDisponiblesReporteHandler.CODIGO;
            ctx.sortBy = detectSubjectSort(ctx.normalizedQuery);
            if (!containsAny(ctx.normalizedQuery, "mejor", "mejores", "peor", "peores", "top", "ultimos", "primeros", "mas alto", "mas bajo")) {
                ctx.sortDirection = "ASC";
            }
            ctx.filters.put("sortBy", ctx.sortBy);
            ctx.filters.put("sortDirection", ctx.sortDirection);
            return;
        }

        if (isTeachersBySubjectIntent(ctx.normalizedQuery)) {
            ctx.reportCode = DocentesPorMateriaReporteHandler.CODIGO;
            ctx.filters.put("sortDirection", ctx.sortDirection);
            matchMateria(ctx.idInstitucion, ctx.normalizedQuery).ifPresent(m -> ctx.filters.put("idMateria", m.getId().toString()));
            return;
        }

        if (isTutorListIntent(ctx.normalizedQuery)) {
            ctx.reportCode = TutoresDisponiblesReporteHandler.CODIGO;
            if (!containsAny(ctx.normalizedQuery, "mejor", "mejores", "peor", "peores", "top", "ultimos", "primeros")) {
                ctx.sortDirection = "ASC";
            }
            ctx.filters.put("sortBy", "tutor");
            ctx.filters.put("sortDirection", ctx.sortDirection);
            return;
        }

        if (isStudentListIntent(ctx.normalizedQuery)) {
            ctx.reportCode = EstudiantesPorCursoParaleloReporteHandler.CODIGO;
            detectCourseSection(ctx).ifPresent(section -> {
                ctx.filters.put("idCurso", section.idCurso().toString());
                ctx.filters.put("idParalelo", section.idParalelo().toString());
            });
            return;
        }

        if (isEnrollmentIntent(ctx.normalizedQuery)) {
            ctx.reportCode = MatriculaPorCursoReporteHandler.CODIGO;
            ctx.sortBy = containsAny(ctx.normalizedQuery, "mas matricula", "mayor matricula", "mas inscritos", "mas estudiantes") ? "matricula" : "curso";
            ctx.filters.put("sortBy", ctx.sortBy);
            ctx.filters.put("sortDirection", ctx.sortDirection);
            return;
        }

        if (isAttendanceIntent(ctx.normalizedQuery)) {
            ctx.reportCode = AsistenciaMensualReporteHandler.CODIGO;
            ctx.sortBy = detectAttendanceSort(ctx.normalizedQuery);
            ctx.filters.put("sortBy", ctx.sortBy);
            ctx.filters.put("sortDirection", ctx.sortDirection);
            return;
        }

        if (isRiskIntent(ctx.normalizedQuery)) {
            ctx.reportCode = AlumnosRiesgoAcademicoReporteHandler.CODIGO;
            if (!ctx.filters.containsKey("promedioMaximo")) {
                ctx.filters.put("promedioMaximo", defaultRiskThreshold(ctx.normalizedQuery));
            }
            ctx.filters.put("sortDirection", containsAny(ctx.normalizedQuery, "mejor", "mejores") ? "DESC" : "ASC");
            return;
        }

        if (isPerformanceIntent(ctx.normalizedQuery)) {
            ctx.reportCode = RendimientoAcademicoCursoReporteHandler.CODIGO;
            ctx.sortBy = detectPerformanceSort(ctx.normalizedQuery);
            ctx.filters.put("sortBy", ctx.sortBy);
            ctx.filters.put("sortDirection", ctx.sortDirection);
            return;
        }

        if (isAssignmentIntent(ctx.normalizedQuery)) {
            ctx.reportCode = DocentesAsignacionesReporteHandler.CODIGO;
            return;
        }

        throw new IllegalArgumentException("No pude interpretar la consulta. Pruebe con ejemplos como 'top 10 alumnos con mejor promedio en matemáticas', 'asistencia por curso este mes' o 'matrícula por curso'.");
    }

    private void validateAmbiguity(QueryContext ctx) {
        if (ctx.reportCode == null) {
            throw new IllegalArgumentException("No pude determinar el tipo de reporte solicitado");
        }

        if (containsAny(ctx.normalizedQuery, "comparar", "comparacion", "versus", "vs") && !ctx.filters.containsKey("periodo") && !ctx.filters.containsKey("mes")) {
            throw new IllegalArgumentException("La consulta es ambigua. Especifique el periodo a comparar, por ejemplo 'marzo vs abril' o 'primer trimestre vs segundo trimestre'.");
        }

        if (containsAny(ctx.normalizedQuery, "promedio") && containsAny(ctx.normalizedQuery, "curso o estudiante", "por curso o estudiante")) {
            throw new IllegalArgumentException("La consulta es ambigua. Indique si desea promedio por estudiante o por curso.");
        }

        if (ctx.pageSize >= 100 && containsAny(ctx.normalizedQuery, "todos los", "todas las", "todo", "completo")) {
            throw new IllegalArgumentException("La consulta es demasiado amplia. Agregue filtros como curso, materia, trimestre o mes antes de generar el reporte.");
        }
    }

    private boolean isEnrollmentIntent(String normalized) {
        return containsAny(normalized, "matricula", "inscritos", "inscripciones", "cantidad de alumnos por curso", "estudiantes por curso");
    }

    private boolean isTutorListIntent(String normalized) {
        return containsAny(normalized, "muestrame los tutores", "mostrar tutores", "lista de tutores", "listado de tutores", "que tutores hay", "tutores disponibles");
    }

    private boolean isStudentListIntent(String normalized) {
        return containsAny(normalized, "muestrame los alumnos", "muestrame los estudiantes", "mostrar alumnos", "mostrar estudiantes", "alumnos del", "estudiantes del", "lista de alumnos", "lista de estudiantes");
    }

    private boolean isSubjectListIntent(String normalized) {
        return containsAny(normalized,
                "materias que hay",
                "muestrame las materias",
                "mostrar materias",
                "lista de materias",
                "listado de materias",
                "que materias hay",
                "materias disponibles");
    }

    private boolean isTeachersBySubjectIntent(String normalized) {
        return containsAny(normalized,
                "docentes agrupadas por materia",
                "docentes agrupados por materia",
                "profesores agrupados por materia",
                "docentes por materia",
                "profesores por materia",
                "docentes agrupados por asignatura",
                "profesores agrupados por asignatura");
    }

    private boolean isAttendanceIntent(String normalized) {
        return containsAny(normalized, "asistencia", "faltas", "ausencias", "tardanzas", "justificados");
    }

    private boolean isRiskIntent(String normalized) {
        return containsAny(normalized, "riesgo academico", "riesgo", "peor promedio", "bajo promedio", "promedio menor", "reprobados", "desaprobados");
    }

    private boolean isPerformanceIntent(String normalized) {
        return containsAny(normalized, "rendimiento", "promedio", "mejor promedio", "mejores notas", "notas por", "calificaciones");
    }

    private boolean isAssignmentIntent(String normalized) {
        return containsAny(normalized, "asignaciones", "dictan", "docentes", "profesores", "carga horaria");
    }

    private String detectAttendanceSort(String normalized) {
        if (containsAny(normalized, "faltas", "ausencias", "ausentes")) return "ausentes";
        if (containsAny(normalized, "tardanzas")) return "tardanzas";
        if (containsAny(normalized, "justificados")) return "justificados";
        if (containsAny(normalized, "porcentaje de asistencia", "mejor asistencia", "peor asistencia", "asistencia por curso")) return "asistencia";
        return "curso";
    }

    private String detectSubjectSort(String normalized) {
        if (containsAny(normalized, "codigo")) return "codigo";
        if (containsAny(normalized, "area")) return "area";
        if (containsAny(normalized, "carga horaria", "horas")) return "cargahoraria";
        return "nombre";
    }

    private GroupIntent detectGenericGrouping(QueryContext ctx) {
        if (!containsAny(ctx.normalizedQuery, "agrupado por", "agrupados por", "agrupadas por", "agrupar por", "por materia", "por curso", "por paralelo", "por area", "por estado")) {
            return null;
        }

        if (containsAny(ctx.normalizedQuery, "docentes", "profesores") && containsAny(ctx.normalizedQuery, "por materia", "agrupado por materia", "agrupados por materia")) {
            return new GroupIntent("DOCENTE", "MATERIA");
        }
        if (containsAny(ctx.normalizedQuery, "alumnos", "estudiantes") && containsAny(ctx.normalizedQuery, "por curso", "agrupado por curso", "agrupados por curso")) {
            return new GroupIntent("ESTUDIANTE", "CURSO");
        }
        if (containsAny(ctx.normalizedQuery, "alumnos", "estudiantes") && containsAny(ctx.normalizedQuery, "por paralelo", "agrupado por paralelo", "agrupados por paralelo", "por aula")) {
            return new GroupIntent("ESTUDIANTE", "PARALELO");
        }
        if (containsAny(ctx.normalizedQuery, "materias") && containsAny(ctx.normalizedQuery, "por area", "agrupado por area", "agrupadas por area")) {
            return new GroupIntent("MATERIA", "AREA");
        }
        if (containsAny(ctx.normalizedQuery, "tutores") && containsAny(ctx.normalizedQuery, "por estado", "agrupados por estado")) {
            return new GroupIntent("TUTOR", "ESTADO");
        }
        return null;
    }

    private String detectPerformanceSort(String normalized) {
        if (containsAny(normalized, "promedio", "nota", "notas")) return "promedio";
        if (containsAny(normalized, "materia")) return "materia";
        if (containsAny(normalized, "estudiante", "alumno")) return "estudiante";
        return "curso";
    }

    private BigDecimal defaultRiskThreshold(String normalized) {
        if (containsAny(normalized, "reprobados", "desaprobados")) {
            return new BigDecimal("50");
        }
        return new BigDecimal("51");
    }

    private Optional<BigDecimal> detectPromedioThreshold(String normalized) {
        Matcher matcher = Pattern.compile("promedio\\s*(?:menor(?:\\s*o\\s*igual)?\\s*a|<=|<)\\s*(\\d+(?:[.,]\\d+)?)").matcher(normalized);
        if (matcher.find()) return Optional.of(parseDecimal(matcher.group(1)));
        matcher = Pattern.compile("promedio\\s*(?:mayor(?:\\s*o\\s*igual)?\\s*a|>=|>)\\s*(\\d+(?:[.,]\\d+)?)").matcher(normalized);
        if (matcher.find()) return Optional.of(parseDecimal(matcher.group(1)));
        return Optional.empty();
    }

    private Optional<String> detectEstadoInscripcion(String normalized) {
        if (containsAny(normalized, "retirados", "retirado")) return Optional.of("RETIRADA");
        if (containsAny(normalized, "concluidos", "concluido")) return Optional.of("CONCLUIDA");
        if (containsAny(normalized, "activos", "activa", "vigentes")) return Optional.of("ACTIVA");
        return Optional.empty();
    }

    private Optional<Integer> detectQuarter(String normalized) {
        if (containsAny(normalized, "primer trimestre", "1er trimestre", "trimestre 1", "trimestre uno")) return Optional.of(1);
        if (containsAny(normalized, "segundo trimestre", "2do trimestre", "trimestre 2", "trimestre dos")) return Optional.of(2);
        if (containsAny(normalized, "tercer trimestre", "3er trimestre", "trimestre 3", "trimestre tres")) return Optional.of(3);
        return Optional.empty();
    }

    private Optional<Integer> detectMonth(String normalized) {
        LocalDate now = LocalDate.now();
        if (containsAny(normalized, "este mes", "mes actual")) return Optional.of(now.getMonthValue());
        if (containsAny(normalized, "mes pasado", "ultimo mes")) return Optional.of(now.minusMonths(1).getMonthValue());

        Map<String, Integer> months = Map.ofEntries(
                Map.entry("enero", Month.JANUARY.getValue()), Map.entry("febrero", Month.FEBRUARY.getValue()),
                Map.entry("marzo", Month.MARCH.getValue()), Map.entry("abril", Month.APRIL.getValue()),
                Map.entry("mayo", Month.MAY.getValue()), Map.entry("junio", Month.JUNE.getValue()),
                Map.entry("julio", Month.JULY.getValue()), Map.entry("agosto", Month.AUGUST.getValue()),
                Map.entry("septiembre", Month.SEPTEMBER.getValue()), Map.entry("setiembre", Month.SEPTEMBER.getValue()),
                Map.entry("octubre", Month.OCTOBER.getValue()), Map.entry("noviembre", Month.NOVEMBER.getValue()),
                Map.entry("diciembre", Month.DECEMBER.getValue())
        );

        return months.entrySet().stream()
                .filter(entry -> normalized.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    private Optional<Integer> detectLimit(String normalized) {
        Matcher matcher = Pattern.compile("(?:top|los|las|primeros|primeras|ultimos|ultimas)\\s+(\\d{1,3})").matcher(normalized);
        if (matcher.find()) return Optional.of(Integer.parseInt(matcher.group(1)));
        matcher = Pattern.compile("(\\d{1,3})\\s+(alumnos|estudiantes|cursos|docentes|profesores|materias)").matcher(normalized);
        if (matcher.find()) return Optional.of(Integer.parseInt(matcher.group(1)));
        return Optional.empty();
    }

    private Optional<Materia> matchMateria(UUID idInstitucion, String normalized) {
        return materiaRepository.findAllByIdInstitucion(idInstitucion).stream()
                .filter(materia -> normalized.contains(normalize(materia.getNombre())) || normalized.contains(normalize(materia.getCodigo())))
                .findFirst();
    }

    private Optional<Curso> matchCurso(UUID idInstitucion, String normalized) {
        return cursoRepository.findAllByIdInstitucion(idInstitucion).stream()
                .filter(curso -> normalized.contains(normalize(curso.getNombre())) || (curso.getCodigo() != null && normalized.contains(normalize(curso.getCodigo()))))
                .findFirst();
    }

    private Optional<Docente> matchDocente(UUID idInstitucion, String normalized) {
        return docenteRepository.findAllByIdInstitucion(idInstitucion).stream()
                .filter(docente -> normalized.contains(normalize(docente.getNombres()))
                        || normalized.contains(normalize(docente.getApellidos()))
                        || normalized.contains(normalize(docente.getApellidos() + " " + docente.getNombres())))
                .findFirst();
    }

    private Optional<CourseSection> detectCourseSection(QueryContext ctx) {
        List<Curso> cursos = cursoRepository.findAllByIdInstitucion(ctx.idInstitucion);
        List<Paralelo> paralelos = paraleloRepository.findAllByIdInstitucion(ctx.idInstitucion);
        for (Paralelo paralelo : paralelos) {
            Curso curso = cursos.stream().filter(item -> item.getId().equals(paralelo.getIdCurso())).findFirst().orElse(null);
            if (curso == null) continue;
            String composed = normalize(curso.getNombre() + " " + paralelo.getNombre());
            String compact = composed.replace(" ", "");
            String queryCompact = ctx.normalizedQuery.replace(" ", "");
            if (ctx.normalizedQuery.contains(composed) || queryCompact.contains(compact)) {
                return Optional.of(new CourseSection(curso.getId(), paralelo.getId()));
            }
        }
        return Optional.empty();
    }

    private String detectChartType(String normalized) {
        if (containsAny(normalized, "grafico circular", "pastel", "pie", "torta")) return "pie";
        if (containsAny(normalized, "grafico de lineas", "lineas", "linea")) return "line";
        if (containsAny(normalized, "radar")) return "radar";
        if (containsAny(normalized, "area")) return "area";
        if (containsAny(normalized, "barras", "barra", "columnas")) return "bar";
        return null;
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) return true;
        }
        return false;
    }

    private BigDecimal parseDecimal(String value) {
        return new BigDecimal(value.replace(',', '.'));
    }

    private String normalize(String value) {
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9\\s<>.=]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static final class QueryContext {
        private final String rawQuery;
        private final String normalizedQuery;
        private final UUID idInstitucion;
        private final Map<String, Object> filters = new LinkedHashMap<>();
        private String reportCode;
        private String sortBy;
        private String sortDirection = "DESC";
        private String chartType;
        private int pageSize;

        private QueryContext(String rawQuery, String normalizedQuery, ReporteNaturalLanguageRequest request, UUID idInstitucion) {
            this.rawQuery = rawQuery;
            this.normalizedQuery = normalizedQuery;
            this.idInstitucion = idInstitucion;
            this.pageSize = request.size() == null ? 25 : request.size();
        }

        private String technicalSummary() {
            List<String> parts = new ArrayList<>();
            parts.add("reporte=" + reportCode);
            if (chartType != null) parts.add("chartType=" + chartType);
            parts.add("limit=" + pageSize);
            if (!filters.isEmpty()) {
                filters.forEach((key, value) -> parts.add(key + "=" + value));
            }
            return String.join(", ", parts);
        }
    }

    private record Translation(ReportePreviewRequest request, List<String> headerNotes) {
    }

    private record GroupIntent(String entity, String groupBy) {
    }

    private record CourseSection(UUID idCurso, UUID idParalelo) {
    }
}
