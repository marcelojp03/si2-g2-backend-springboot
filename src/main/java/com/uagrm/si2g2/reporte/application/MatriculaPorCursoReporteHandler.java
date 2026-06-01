package com.uagrm.si2g2.reporte.application;

import com.uagrm.si2g2.reporte.dto.*;
import com.uagrm.si2g2.reporte.query.ReporteQuerySupport;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MatriculaPorCursoReporteHandler extends AbstractReporteHandler {

    public static final String CODIGO = "MATRICULA_POR_CURSO";

    public MatriculaPorCursoReporteHandler(NamedParameterJdbcTemplate jdbc) {
        super(jdbc);
    }

    @Override
    public String codigo() {
        return CODIGO;
    }

    @Override
    public ReporteMetadataResponse metadata() {
        return metadata(CODIGO, "Matrícula por curso", "Total de estudiantes inscritos por curso y paralelo", "PREDEFINIDO", true,
                List.of(filter("idGestion", "Gestión académica", "uuid", false), filter("idCurso", "Curso", "uuid", false), filter("estado", "Estado inscripción", "text", false), filter("sortBy", "Ordenar por", "text", false), filter("sortDirection", "Dirección orden", "text", false)));
    }

    @Override
    public ReportePreviewResponse ejecutar(ReporteExecutionContext context) {
        StringBuilder where = new StringBuilder(" WHERE i.id_institucion = :idInstitucion");
        MapSqlParameterSource params = new MapSqlParameterSource("idInstitucion", context.idInstitucion());
        ReporteQuerySupport.addUuidFilter(where, params, context.filtros(), "idGestion", "i.id_gestion_academica");
        ReporteQuerySupport.addUuidFilter(where, params, context.filtros(), "idCurso", "c.id");
        ReporteQuerySupport.addTextFilter(where, params, context.filtros(), "estado", "i.estado");

        String orderBy = resolveOrderBy(context);

        String sql = """
                SELECT c.nombre AS curso, p.nombre AS paralelo, i.estado AS estado, COUNT(*) AS total_estudiantes
                FROM inscripcion i
                JOIN paralelo p ON p.id = i.id_paralelo AND p.id_institucion = i.id_institucion
                JOIN curso c ON c.id = p.id_curso AND c.id_institucion = p.id_institucion
                """ + where + " GROUP BY c.nombre, p.nombre, i.estado ORDER BY " + orderBy;

        return ReporteQuerySupport.execute(jdbc, metadata().nombre(), context.usuarioNombre(),
                ReporteQuerySupport.headerFilters(context, ReporteQuerySupport.filtro("Estado", context.filtros().get("estado")), ReporteQuerySupport.filtro("Orden", orderBy)),
                List.of(col("curso", "Curso", "text"), col("paralelo", "Paralelo", "text"), col("estado", "Estado", "text"), col("totalEstudiantes", "Total estudiantes", "number")),
                sql, params, context.page(), context.size(), null);
    }

    private String resolveOrderBy(ReporteExecutionContext context) {
        String sortBy = String.valueOf(context.filtros().getOrDefault("sortBy", "curso")).toLowerCase();
        String direction = "DESC".equalsIgnoreCase(String.valueOf(context.filtros().getOrDefault("sortDirection", "ASC"))) ? "DESC" : "ASC";
        return switch (sortBy) {
            case "total", "totalestudiantes", "matricula" -> "COUNT(*) " + direction + ", c.nombre ASC, p.nombre ASC";
            case "paralelo" -> "p.nombre " + direction + ", c.nombre ASC";
            default -> "c.nombre " + direction + ", p.nombre ASC, i.estado ASC";
        };
    }
}
