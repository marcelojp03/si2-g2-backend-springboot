package com.uagrm.si2g2.reporte.application;

import com.uagrm.si2g2.reporte.dto.*;
import com.uagrm.si2g2.reporte.query.ReporteQuerySupport;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AsistenciaMensualReporteHandler extends AbstractReporteHandler {

    public static final String CODIGO = "ASISTENCIA_MENSUAL";

    public AsistenciaMensualReporteHandler(NamedParameterJdbcTemplate jdbc) { super(jdbc); }

    @Override public String codigo() { return CODIGO; }

    @Override
    public ReporteMetadataResponse metadata() {
        return metadata(CODIGO, "Asistencia mensual", "Resumen de asistencia por curso, paralelo y materia", "PREDEFINIDO", true,
                List.of(filter("idGestion", "Gestión académica", "uuid", false), filter("idCurso", "Curso", "uuid", false), filter("idMateria", "Materia", "uuid", false), filter("mes", "Mes", "number", false), filter("sortBy", "Ordenar por", "text", false), filter("sortDirection", "Dirección orden", "text", false)));
    }

    @Override
    public ReportePreviewResponse ejecutar(ReporteExecutionContext context) {
        StringBuilder where = new StringBuilder(" WHERE ar.id_institucion = :idInstitucion");
        MapSqlParameterSource params = new MapSqlParameterSource("idInstitucion", context.idInstitucion());
        ReporteQuerySupport.addUuidFilter(where, params, context.filtros(), "idGestion", "ad.id_gestion_academica");
        ReporteQuerySupport.addUuidFilter(where, params, context.filtros(), "idCurso", "c.id");
        ReporteQuerySupport.addUuidFilter(where, params, context.filtros(), "idMateria", "m.id");
        Integer mes = ReporteQuerySupport.integer(context.filtros().get("mes"));
        if (mes != null) {
            where.append(" AND EXTRACT(MONTH FROM ar.fecha) = :mes");
            params.addValue("mes", mes);
        }

        String orderBy = resolveOrderBy(context);

        String sql = """
                SELECT c.nombre AS curso, p.nombre AS paralelo, m.nombre AS materia,
                       COUNT(*) FILTER (WHERE adt.estado_asistencia = 'PRESENTE') AS presentes,
                       COUNT(*) FILTER (WHERE adt.estado_asistencia = 'AUSENTE') AS ausentes,
                       COUNT(*) FILTER (WHERE adt.estado_asistencia = 'TARDANZA') AS tardanzas,
                       COUNT(*) FILTER (WHERE adt.estado_asistencia = 'JUSTIFICADO') AS justificados,
                       ROUND((COUNT(*) FILTER (WHERE adt.estado_asistencia IN ('PRESENTE','TARDANZA','JUSTIFICADO')) * 100.0) / NULLIF(COUNT(*), 0), 2) AS porcentaje_asistencia
                FROM asistencia_registro ar
                JOIN asistencia_detalle adt ON adt.id_asistencia_registro = ar.id
                JOIN asignacion_docente ad ON ad.id = ar.id_asignacion_docente AND ad.id_institucion = ar.id_institucion
                JOIN materia m ON m.id = ad.id_materia AND m.id_institucion = ad.id_institucion
                JOIN paralelo p ON p.id = ad.id_paralelo AND p.id_institucion = ad.id_institucion
                JOIN curso c ON c.id = p.id_curso AND c.id_institucion = p.id_institucion
                """ + where + " GROUP BY c.nombre, p.nombre, m.nombre ORDER BY " + orderBy;

        return ReporteQuerySupport.execute(jdbc, metadata().nombre(), context.usuarioNombre(),
                ReporteQuerySupport.headerFilters(context, ReporteQuerySupport.filtro("Mes", mes), ReporteQuerySupport.filtro("Orden", orderBy)),
                List.of(col("curso", "Curso", "text"), col("paralelo", "Paralelo", "text"), col("materia", "Materia", "text"), col("presentes", "Presentes", "number"), col("ausentes", "Ausentes", "number"), col("tardanzas", "Tardanzas", "number"), col("justificados", "Justificados", "number"), col("porcentajeAsistencia", "% asistencia", "number")),
                sql, params, context.page(), context.size(), null);
    }

    private String resolveOrderBy(ReporteExecutionContext context) {
        String sortBy = String.valueOf(context.filtros().getOrDefault("sortBy", "curso")).toLowerCase();
        String direction = "DESC".equalsIgnoreCase(String.valueOf(context.filtros().getOrDefault("sortDirection", "ASC"))) ? "DESC" : "ASC";
        return switch (sortBy) {
            case "porcentajeasistencia", "asistencia" -> "porcentaje_asistencia " + direction + ", c.nombre ASC, p.nombre ASC";
            case "presentes" -> "presentes " + direction + ", c.nombre ASC";
            case "ausentes", "faltas" -> "ausentes " + direction + ", c.nombre ASC";
            case "tardanzas" -> "tardanzas " + direction + ", c.nombre ASC";
            case "justificados" -> "justificados " + direction + ", c.nombre ASC";
            case "materia" -> "m.nombre " + direction + ", c.nombre ASC";
            default -> "c.nombre " + direction + ", p.nombre ASC, m.nombre ASC";
        };
    }
}
