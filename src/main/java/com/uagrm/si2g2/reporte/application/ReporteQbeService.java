package com.uagrm.si2g2.reporte.application;

import com.uagrm.si2g2.auditoria.application.AuditoriaService;
import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.institucion.domain.InstitucionRepository;
import com.uagrm.si2g2.reporte.dto.*;
import com.uagrm.si2g2.reporte.export.ReporteExporter;
import com.uagrm.si2g2.reporte.export.ReporteExporterRegistry;
import com.uagrm.si2g2.reporte.query.ReporteQuerySupport;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReporteQbeService {

    private static final int MAX_PREVIEW_SIZE = 100;
    private static final int MAX_EXPORT_ROWS = 10_000;

    private final NamedParameterJdbcTemplate jdbc;
    private final ReporteExporterRegistry exporterRegistry;
    private final InstitucionRepository institucionRepository;
    private final AuditoriaService auditoriaService;

    @Transactional(readOnly = true)
    public List<QbeEntityDefinitionResponse> catalogo() {
        return definitions().values().stream()
                .map(def -> new QbeEntityDefinitionResponse(def.entity(), def.label(), def.fields().values().stream()
                        .map(field -> new QbeFieldDefinitionResponse(field.field(), field.label(), field.type(), field.operators()))
                        .toList()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ReportePreviewResponse preview(QbePreviewRequest request) {
        ReportePreviewResponse response = execute(request, sanitizePage(request.page()), sanitizeSize(request.size()));
        auditoriaService.registrar(SecurityUtils.requireCurrentInstitutionId(), SecurityUtils.currentUserId(),
                "REPORTES", "QBE_PREVIEW", "reporte_qbe", request.entidad(), true, "Vista previa de reporte QBE");
        return response;
    }

    @Transactional(readOnly = true)
    public ReporteService.ExportedReport exportar(QbePreviewRequest request, String formato) {
        ReportePreviewResponse response = execute(request, 0, MAX_EXPORT_ROWS);
        if (response.totalRegistros() > MAX_EXPORT_ROWS) {
            throw new IllegalArgumentException("El reporte QBE supera " + MAX_EXPORT_ROWS + " registros. Aplique filtros más específicos antes de exportar.");
        }
        ReporteExporter exporter = exporterRegistry.get(formato);
        byte[] bytes = exporter.exportar(response);
        auditoriaService.registrar(SecurityUtils.requireCurrentInstitutionId(), SecurityUtils.currentUserId(),
                "REPORTES", "QBE_EXPORTAR_" + exporter.formato(), "reporte_qbe", request.entidad(), true, "Exportación de reporte QBE");
        String fileName = normalizeFileName("qbe-" + request.entidad()) + "." + exporter.extension();
        return new ReporteService.ExportedReport(bytes, exporter.contentType(), fileName);
    }

    private ReportePreviewResponse execute(QbePreviewRequest request, int page, int size) {
        QbeEntityDefinition definition = definitions().get(request.entidad().toUpperCase(Locale.ROOT));
        if (definition == null) {
            throw new EntityNotFoundException("Entidad QBE no soportada: " + request.entidad());
        }

        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        String institucionNombre = institucionRepository.findById(idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Institución no encontrada: " + idInstitucion))
                .getNombre();

        List<QbeFieldDefinition> selectedFields = selectFields(definition, request.columnas());
        String select = selectedFields.stream().map(field -> field.column() + " AS " + field.alias()).collect(Collectors.joining(", "));
        StringBuilder sql = new StringBuilder("SELECT ").append(select).append(" ").append(definition.from()).append(" WHERE ")
                .append(definition.tenantColumn()).append(" = :idInstitucion");
        MapSqlParameterSource params = new MapSqlParameterSource("idInstitucion", idInstitucion);
        List<String> qbeConditions = new ArrayList<>();

        int index = 0;
        for (QbeConditionRequest condition : request.condiciones() == null ? List.<QbeConditionRequest>of() : request.condiciones()) {
            QbeFieldDefinition field = definition.fields().get(condition.campo());
            if (field == null) throw new IllegalArgumentException("Campo QBE no soportado: " + condition.campo());
            applyCondition(sql, params, qbeConditions, field, condition, index++);
        }

        sql.append(" ORDER BY ").append(selectedFields.getFirst().column());

        ReporteExecutionContext context = new ReporteExecutionContext(
                idInstitucion,
                institucionNombre,
                SecurityUtils.currentUserId(),
                Optional.ofNullable(SecurityUtils.currentUser()).map(user -> (user.getNombres() + " " + user.getApellidos()).trim()).orElse("Usuario"),
                Map.of(),
                List.of("Modo QBE: entidad=" + definition.label(), "Condiciones QBE: " + (qbeConditions.isEmpty() ? "sin condiciones explícitas" : String.join(" AND ", qbeConditions))),
                request.presentacion(),
                page,
                size
        );

        return ReporteQuerySupport.execute(
                jdbc,
                "Reporte dinámico QBE - " + definition.label(),
                context.usuarioNombre(),
                ReporteQuerySupport.headerFilters(context),
                selectedFields.stream().map(field -> new ReporteColumnResponse(field.alias(), field.label(), mapColumnType(field.type()))).toList(),
                sql.toString(),
                params,
                page,
                size,
                null
        );
    }

    private void applyCondition(StringBuilder sql, MapSqlParameterSource params, List<String> qbeConditions, QbeFieldDefinition field, QbeConditionRequest condition, int index) {
        String operator = Optional.ofNullable(condition.operador()).orElse("").toUpperCase(Locale.ROOT);
        if (!field.operators().contains(operator)) {
            throw new IllegalArgumentException("Operador no permitido para el campo " + field.label() + ": " + operator);
        }
        String param = field.field() + index;
        String column = field.column();

        switch (operator) {
            case "CONTAINS" -> {
                sql.append(" AND UPPER(").append(column).append(") LIKE :").append(param);
                params.addValue(param, "%" + text(condition.valor()).toUpperCase(Locale.ROOT) + "%");
                qbeConditions.add(field.label() + " contiene '" + condition.valor() + "'");
            }
            case "STARTS_WITH" -> {
                sql.append(" AND UPPER(").append(column).append(") LIKE :").append(param);
                params.addValue(param, text(condition.valor()).toUpperCase(Locale.ROOT) + "%");
                qbeConditions.add(field.label() + " comienza con '" + condition.valor() + "'");
            }
            case "EQUALS" -> {
                sql.append(" AND ").append(column).append(" = :").append(param);
                params.addValue(param, castValue(field.type(), condition.valor()));
                qbeConditions.add(field.label() + " = '" + condition.valor() + "'");
            }
            case "NOT_EMPTY" -> {
                sql.append(" AND ").append(column).append(" IS NOT NULL AND TRIM(CAST(").append(column).append(" AS text)) <> ''");
                qbeConditions.add(field.label() + " no vacío");
            }
            case "GT" -> {
                sql.append(" AND ").append(column).append(" > :").append(param);
                params.addValue(param, castValue(field.type(), condition.valor()));
                qbeConditions.add(field.label() + " > " + condition.valor());
            }
            case "LT" -> {
                sql.append(" AND ").append(column).append(" < :").append(param);
                params.addValue(param, castValue(field.type(), condition.valor()));
                qbeConditions.add(field.label() + " < " + condition.valor());
            }
            case "BETWEEN" -> {
                sql.append(" AND ").append(column).append(" BETWEEN :").append(param).append("Desde AND :").append(param).append("Hasta");
                params.addValue(param + "Desde", castValue(field.type(), condition.valor()));
                params.addValue(param + "Hasta", castValue(field.type(), condition.valorHasta()));
                qbeConditions.add(field.label() + " entre " + condition.valor() + " y " + condition.valorHasta());
            }
            case "IN_LIST" -> {
                List<String> values = Arrays.stream(text(condition.valor()).split(",")).map(String::trim).filter(v -> !v.isBlank()).toList();
                sql.append(" AND ").append(column).append(" IN (:").append(param).append(")");
                params.addValue(param, values.stream().map(value -> castValue(field.type(), value)).toList());
                qbeConditions.add(field.label() + " en lista " + values);
            }
            case "DATE_RANGE" -> {
                sql.append(" AND ").append(column).append(" BETWEEN :").append(param).append("Desde AND :").append(param).append("Hasta");
                params.addValue(param + "Desde", LocalDate.parse(condition.valor()));
                params.addValue(param + "Hasta", LocalDate.parse(condition.valorHasta()));
                qbeConditions.add(field.label() + " entre " + condition.valor() + " y " + condition.valorHasta());
            }
            case "BEFORE" -> {
                sql.append(" AND ").append(column).append(" < :").append(param);
                params.addValue(param, LocalDate.parse(condition.valor()));
                qbeConditions.add(field.label() + " antes de " + condition.valor());
            }
            case "AFTER" -> {
                sql.append(" AND ").append(column).append(" > :").append(param);
                params.addValue(param, LocalDate.parse(condition.valor()));
                qbeConditions.add(field.label() + " después de " + condition.valor());
            }
            case "MONTH_YEAR" -> {
                sql.append(" AND TO_CHAR(").append(column).append(", 'YYYY-MM') = :").append(param);
                params.addValue(param, condition.valor());
                qbeConditions.add(field.label() + " en mes/año " + condition.valor());
            }
            case "TRUE" -> {
                sql.append(" AND ").append(column).append(" = TRUE");
                qbeConditions.add(field.label() + " = verdadero");
            }
            case "FALSE" -> {
                sql.append(" AND ").append(column).append(" = FALSE");
                qbeConditions.add(field.label() + " = falso");
            }
            default -> throw new IllegalArgumentException("Operador no soportado: " + operator);
        }
    }

    private List<QbeFieldDefinition> selectFields(QbeEntityDefinition definition, List<String> columnas) {
        if (columnas == null || columnas.isEmpty()) {
            return new ArrayList<>(definition.fields().values());
        }
        List<QbeFieldDefinition> selected = columnas.stream()
                .map(definition.fields()::get)
                .filter(Objects::nonNull)
                .toList();
        if (selected.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos una columna válida para QBE");
        }
        return selected;
    }

    private int sanitizePage(Integer page) { return Math.max(page == null ? 0 : page, 0); }
    private int sanitizeSize(Integer size) { int value = size == null ? 25 : size; return Math.max(1, Math.min(value, MAX_PREVIEW_SIZE)); }
    private String text(String value) { if (value == null || value.isBlank()) throw new IllegalArgumentException("Falta valor para la condición QBE"); return value.trim(); }

    private Object castValue(String type, String value) {
        return switch (type) {
            case "number" -> new java.math.BigDecimal(text(value));
            case "date" -> LocalDate.parse(text(value));
            case "boolean" -> Boolean.parseBoolean(text(value));
            default -> text(value);
        };
    }

    private String mapColumnType(String type) {
        return switch (type) {
            case "number" -> "number";
            case "date" -> "date";
            default -> "text";
        };
    }

    private String normalizeFileName(String value) {
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private Map<String, QbeEntityDefinition> definitions() {
        Map<String, QbeEntityDefinition> defs = new LinkedHashMap<>();
        defs.put("ESTUDIANTE", new QbeEntityDefinition(
                "ESTUDIANTE", "Estudiantes", "FROM estudiante e", "e.id_institucion",
                fields(
                        field("codigoEstudiante", "Código estudiante", "text", "e.codigo_estudiante"),
                        field("documentoIdentidad", "Documento", "text", "e.documento_identidad"),
                        field("nombres", "Nombres", "text", "e.nombres"),
                        field("apellidos", "Apellidos", "text", "e.apellidos"),
                        field("fechaNacimiento", "Fecha nacimiento", "date", "e.fecha_nacimiento", List.of("EQUALS", "DATE_RANGE", "BEFORE", "AFTER", "MONTH_YEAR")),
                        field("sexo", "Sexo", "text", "e.sexo"),
                        field("estado", "Estado", "text", "e.estado")
                )));

        defs.put("DOCENTE", new QbeEntityDefinition(
                "DOCENTE", "Docentes", "FROM docente d", "d.id_institucion",
                fields(
                        field("codigo", "Código docente", "text", "d.codigo"),
                        field("documentoIdentidad", "Documento", "text", "d.documento_identidad"),
                        field("nombres", "Nombres", "text", "d.nombres"),
                        field("apellidos", "Apellidos", "text", "d.apellidos"),
                        field("especialidad", "Especialidad", "text", "d.especialidad"),
                        field("estado", "Estado", "text", "d.estado")
                )));

        defs.put("INSCRIPCION", new QbeEntityDefinition(
                "INSCRIPCION", "Inscripciones",
                "FROM inscripcion i JOIN estudiante e ON e.id = i.id_estudiante AND e.id_institucion = i.id_institucion " +
                        "JOIN paralelo p ON p.id = i.id_paralelo AND p.id_institucion = i.id_institucion " +
                        "JOIN curso c ON c.id = p.id_curso AND c.id_institucion = p.id_institucion " +
                        "JOIN gestion_academica g ON g.id = i.id_gestion_academica AND g.id_institucion = i.id_institucion",
                "i.id_institucion",
                fields(
                        field("fechaInscripcion", "Fecha inscripción", "date", "i.fecha_inscripcion", List.of("EQUALS", "DATE_RANGE", "BEFORE", "AFTER", "MONTH_YEAR")),
                        field("estado", "Estado inscripción", "text", "i.estado"),
                        field("estudiante", "Estudiante", "text", "CONCAT(e.apellidos, ' ', e.nombres)"),
                        field("curso", "Curso", "text", "c.nombre"),
                        field("paralelo", "Paralelo", "text", "p.nombre"),
                        field("gestion", "Gestión", "text", "g.nombre")
                )));

        defs.put("ASISTENCIA", new QbeEntityDefinition(
                "ASISTENCIA", "Asistencias",
                "FROM asistencia_detalle adt JOIN asistencia_registro ar ON ar.id = adt.id_asistencia_registro " +
                        "JOIN inscripcion i ON i.id = adt.id_inscripcion AND i.id_institucion = adt.id_institucion " +
                        "JOIN estudiante e ON e.id = i.id_estudiante AND e.id_institucion = i.id_institucion " +
                        "JOIN asignacion_docente ad ON ad.id = ar.id_asignacion_docente AND ad.id_institucion = ar.id_institucion " +
                        "JOIN materia m ON m.id = ad.id_materia AND m.id_institucion = ad.id_institucion " +
                        "JOIN paralelo p ON p.id = ad.id_paralelo AND p.id_institucion = ad.id_institucion " +
                        "JOIN curso c ON c.id = p.id_curso AND c.id_institucion = p.id_institucion",
                "adt.id_institucion",
                fields(
                        field("fecha", "Fecha", "date", "ar.fecha", List.of("EQUALS", "DATE_RANGE", "BEFORE", "AFTER", "MONTH_YEAR")),
                        field("estadoAsistencia", "Estado asistencia", "text", "adt.estado_asistencia"),
                        field("estudiante", "Estudiante", "text", "CONCAT(e.apellidos, ' ', e.nombres)"),
                        field("curso", "Curso", "text", "c.nombre"),
                        field("paralelo", "Paralelo", "text", "p.nombre"),
                        field("materia", "Materia", "text", "m.nombre")
                )));

        defs.put("CALIFICACION", new QbeEntityDefinition(
                "CALIFICACION", "Calificaciones",
                "FROM calificacion cal JOIN evaluacion ev ON ev.id = cal.id_evaluacion AND ev.id_institucion = cal.id_institucion " +
                        "JOIN inscripcion i ON i.id = cal.id_inscripcion AND i.id_institucion = cal.id_institucion " +
                        "JOIN estudiante e ON e.id = i.id_estudiante AND e.id_institucion = i.id_institucion " +
                        "JOIN asignacion_docente ad ON ad.id = ev.id_asignacion_docente AND ad.id_institucion = ev.id_institucion " +
                        "JOIN materia m ON m.id = ad.id_materia AND m.id_institucion = ad.id_institucion " +
                        "JOIN paralelo p ON p.id = ad.id_paralelo AND p.id_institucion = ad.id_institucion " +
                        "JOIN curso c ON c.id = p.id_curso AND c.id_institucion = p.id_institucion",
                "cal.id_institucion",
                fields(
                        field("periodo", "Periodo", "number", "ev.periodo", List.of("EQUALS", "GT", "LT", "BETWEEN", "IN_LIST")),
                        field("notaNumerica", "Nota numérica", "number", "cal.nota_numerica", List.of("EQUALS", "GT", "LT", "BETWEEN", "IN_LIST")),
                        field("estadoEvaluacion", "Estado evaluación", "text", "ev.estado"),
                        field("estudiante", "Estudiante", "text", "CONCAT(e.apellidos, ' ', e.nombres)"),
                        field("curso", "Curso", "text", "c.nombre"),
                        field("paralelo", "Paralelo", "text", "p.nombre"),
                        field("materia", "Materia", "text", "m.nombre")
                )));
        return defs;
    }

    private Map<String, QbeFieldDefinition> fields(QbeFieldDefinition... definitions) {
        return Arrays.stream(definitions).collect(Collectors.toMap(QbeFieldDefinition::field, field -> field, (a, b) -> a, LinkedHashMap::new));
    }

    private QbeFieldDefinition field(String field, String label, String type, String column) {
        return new QbeFieldDefinition(field, label, type, column, field, defaultOperators(type));
    }

    private QbeFieldDefinition field(String field, String label, String type, String column, List<String> operators) {
        return new QbeFieldDefinition(field, label, type, column, field, operators);
    }

    private List<String> defaultOperators(String type) {
        return switch (type) {
            case "number" -> List.of("EQUALS", "GT", "LT", "BETWEEN", "IN_LIST");
            case "date" -> List.of("EQUALS", "DATE_RANGE", "BEFORE", "AFTER", "MONTH_YEAR");
            case "boolean" -> List.of("TRUE", "FALSE");
            default -> List.of("CONTAINS", "STARTS_WITH", "EQUALS", "NOT_EMPTY");
        };
    }

    private record QbeEntityDefinition(String entity, String label, String from, String tenantColumn, Map<String, QbeFieldDefinition> fields) {
    }

    private record QbeFieldDefinition(String field, String label, String type, String column, String alias, List<String> operators) {
    }
}
