package com.uagrm.si2g2.reporte.application;

import com.uagrm.si2g2.reporte.dto.*;
import com.uagrm.si2g2.reporte.query.ReporteQuerySupport;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class AlumnosRiesgoAcademicoReporteHandler extends AbstractReporteHandler {

    public static final String CODIGO = "ALUMNOS_RIESGO_ACADEMICO";

    public AlumnosRiesgoAcademicoReporteHandler(NamedParameterJdbcTemplate jdbc) { super(jdbc); }

    @Override public String codigo() { return CODIGO; }

    @Override
    public ReporteMetadataResponse metadata() {
        return metadata(CODIGO, "Alumnos con riesgo académico", "Estudiantes cuyo promedio está por debajo del umbral indicado", "ANALITICO", true,
                List.of(filter("idGestion", "Gestión académica", "uuid", false), filter("periodo", "Periodo", "number", false), filter("idCurso", "Curso", "uuid", false), filter("idMateria", "Materia", "uuid", false), filter("promedioMaximo", "Promedio menor o igual", "number", false), filter("sortDirection", "Dirección orden", "text", false)));
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
        BigDecimal promedioMaximo = ReporteQuerySupport.decimal(context.filtros().getOrDefault("promedioMaximo", "51"));
        params.addValue("promedioMaximo", promedioMaximo);

        String direction = "DESC".equalsIgnoreCase(String.valueOf(context.filtros().getOrDefault("sortDirection", "ASC"))) ? "DESC" : "ASC";

        String sql = """
                SELECT e.codigo_estudiante, CONCAT(e.apellidos, ' ', e.nombres) AS estudiante,
                       c.nombre AS curso, p.nombre AS paralelo, m.nombre AS materia,
                       CONCAT(d.apellidos, ' ', d.nombres) AS docente,
                       ROUND(AVG(cal.nota_numerica), 2) AS promedio
                FROM calificacion cal
                JOIN evaluacion ev ON ev.id = cal.id_evaluacion AND ev.id_institucion = cal.id_institucion
                JOIN inscripcion i ON i.id = cal.id_inscripcion AND i.id_institucion = cal.id_institucion
                JOIN estudiante e ON e.id = i.id_estudiante AND e.id_institucion = i.id_institucion
                JOIN asignacion_docente ad ON ad.id = ev.id_asignacion_docente AND ad.id_institucion = ev.id_institucion
                JOIN docente d ON d.id = ad.id_docente AND d.id_institucion = ad.id_institucion
                JOIN materia m ON m.id = ad.id_materia AND m.id_institucion = ad.id_institucion
                JOIN paralelo p ON p.id = ad.id_paralelo AND p.id_institucion = ad.id_institucion
                JOIN curso c ON c.id = p.id_curso AND c.id_institucion = p.id_institucion
                """ + where + " AND cal.nota_numerica IS NOT NULL GROUP BY e.codigo_estudiante, e.apellidos, e.nombres, c.nombre, p.nombre, m.nombre, d.apellidos, d.nombres HAVING AVG(cal.nota_numerica) <= :promedioMaximo ORDER BY promedio " + direction + ", e.apellidos, e.nombres";

        return ReporteQuerySupport.execute(jdbc, metadata().nombre(), context.usuarioNombre(),
                ReporteQuerySupport.headerFilters(context, ReporteQuerySupport.filtro("Periodo", periodo), ReporteQuerySupport.filtro("Promedio máximo", promedioMaximo), ReporteQuerySupport.filtro("Orden promedio", direction)),
                List.of(col("codigoEstudiante", "Código", "text"), col("estudiante", "Estudiante", "text"), col("curso", "Curso", "text"), col("paralelo", "Paralelo", "text"), col("materia", "Materia", "text"), col("docente", "Docente", "text"), col("promedio", "Promedio", "number")),
                sql, params, context.page(), context.size(), null);
    }
}
