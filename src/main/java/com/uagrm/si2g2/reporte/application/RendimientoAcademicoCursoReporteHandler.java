package com.uagrm.si2g2.reporte.application;

import com.uagrm.si2g2.reporte.dto.*;
import com.uagrm.si2g2.reporte.query.ReporteQuerySupport;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RendimientoAcademicoCursoReporteHandler extends AbstractReporteHandler {

    public static final String CODIGO = "RENDIMIENTO_ACADEMICO_CURSO";

    public RendimientoAcademicoCursoReporteHandler(NamedParameterJdbcTemplate jdbc) { super(jdbc); }

    @Override public String codigo() { return CODIGO; }

    @Override
    public ReporteMetadataResponse metadata() {
        return metadata(CODIGO, "Rendimiento académico por curso", "Promedio de calificaciones por estudiante, curso y materia", "PREDEFINIDO", true,
                List.of(filter("idGestion", "Gestión académica", "uuid", false), filter("periodo", "Periodo", "number", false), filter("idCurso", "Curso", "uuid", false), filter("idMateria", "Materia", "uuid", false), filter("sortBy", "Ordenar por", "text", false), filter("sortDirection", "Dirección orden", "text", false)));
    }

    @Override
    public ReportePreviewResponse ejecutar(ReporteExecutionContext context) {
        StringBuilder where = new StringBuilder(" WHERE cal.id_institucion = :idInstitucion");
        MapSqlParameterSource params = new MapSqlParameterSource("idInstitucion", context.idInstitucion());
        ReporteQuerySupport.addUuidFilter(where, params, context.filtros(), "idGestion", "i.id_gestion_academica");
        ReporteQuerySupport.addUuidFilter(where, params, context.filtros(), "idCurso", "c.id");
        ReporteQuerySupport.addUuidFilter(where, params, context.filtros(), "idMateria", "m.id");
        Integer periodo = ReporteQuerySupport.integer(context.filtros().get("periodo"));
        if (periodo != null) {
            where.append(" AND ev.periodo = :periodo");
            params.addValue("periodo", periodo);
        }

        String orderBy = resolveOrderBy(context);

        String sql = """
                SELECT e.codigo_estudiante, CONCAT(e.apellidos, ' ', e.nombres) AS estudiante,
                       c.nombre AS curso, p.nombre AS paralelo, m.nombre AS materia,
                       ROUND(AVG(cal.nota_numerica), 2) AS promedio
                FROM calificacion cal
                JOIN evaluacion ev ON ev.id = cal.id_evaluacion AND ev.id_institucion = cal.id_institucion
                JOIN inscripcion i ON i.id = cal.id_inscripcion AND i.id_institucion = cal.id_institucion
                JOIN estudiante e ON e.id = i.id_estudiante AND e.id_institucion = i.id_institucion
                JOIN asignacion_docente ad ON ad.id = ev.id_asignacion_docente AND ad.id_institucion = ev.id_institucion
                JOIN materia m ON m.id = ad.id_materia AND m.id_institucion = ad.id_institucion
                JOIN paralelo p ON p.id = ad.id_paralelo AND p.id_institucion = ad.id_institucion
                JOIN curso c ON c.id = p.id_curso AND c.id_institucion = p.id_institucion
                """ + where + " AND cal.nota_numerica IS NOT NULL GROUP BY e.codigo_estudiante, e.apellidos, e.nombres, c.nombre, p.nombre, m.nombre ORDER BY " + orderBy;

        return ReporteQuerySupport.execute(jdbc, metadata().nombre(), context.usuarioNombre(),
                ReporteQuerySupport.headerFilters(context, ReporteQuerySupport.filtro("Periodo", periodo), ReporteQuerySupport.filtro("Orden", orderBy)),
                List.of(col("codigoEstudiante", "Código", "text"), col("estudiante", "Estudiante", "text"), col("curso", "Curso", "text"), col("paralelo", "Paralelo", "text"), col("materia", "Materia", "text"), col("promedio", "Promedio", "number")),
                sql, params, context.page(), context.size(), null);
    }

    private String resolveOrderBy(ReporteExecutionContext context) {
        String sortBy = String.valueOf(context.filtros().getOrDefault("sortBy", "curso")).toLowerCase();
        String direction = "DESC".equalsIgnoreCase(String.valueOf(context.filtros().getOrDefault("sortDirection", "ASC"))) ? "DESC" : "ASC";
        return switch (sortBy) {
            case "promedio" -> "promedio " + direction + ", estudiante ASC";
            case "materia" -> "m.nombre " + direction + ", estudiante ASC";
            case "estudiante" -> "estudiante " + direction;
            default -> "c.nombre " + direction + ", p.nombre ASC, estudiante ASC";
        };
    }
}
