package com.uagrm.si2g2.reporte.application;

import com.uagrm.si2g2.reporte.dto.ReporteMetadataResponse;
import com.uagrm.si2g2.reporte.dto.ReportePreviewResponse;
import com.uagrm.si2g2.reporte.query.ReporteQuerySupport;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EstudiantesPorCursoParaleloReporteHandler extends AbstractReporteHandler {

    public static final String CODIGO = "ESTUDIANTES_POR_CURSO_PARALELO";

    public EstudiantesPorCursoParaleloReporteHandler(NamedParameterJdbcTemplate jdbc) {
        super(jdbc);
    }

    @Override
    public String codigo() {
        return CODIGO;
    }

    @Override
    public ReporteMetadataResponse metadata() {
        return metadata(CODIGO, "Estudiantes por curso y paralelo", "Listado de alumnos filtrados por curso/paralelo", "PREDEFINIDO", false,
                List.of(filter("idGestion", "Gestión académica", "uuid", false), filter("idCurso", "Curso", "uuid", false), filter("idParalelo", "Paralelo", "uuid", false), filter("estado", "Estado inscripción", "text", false)));
    }

    @Override
    public ReportePreviewResponse ejecutar(ReporteExecutionContext context) {
        StringBuilder where = new StringBuilder(" WHERE i.id_institucion = :idInstitucion");
        MapSqlParameterSource params = new MapSqlParameterSource("idInstitucion", context.idInstitucion());
        ReporteQuerySupport.addUuidFilter(where, params, context.filtros(), "idGestion", "i.id_gestion_academica");
        ReporteQuerySupport.addUuidFilter(where, params, context.filtros(), "idCurso", "c.id");
        ReporteQuerySupport.addUuidFilter(where, params, context.filtros(), "idParalelo", "p.id");
        ReporteQuerySupport.addTextFilter(where, params, context.filtros(), "estado", "i.estado");

        String sql = """
                SELECT e.codigo_estudiante, CONCAT(e.apellidos, ' ', e.nombres) AS estudiante,
                       c.nombre AS curso, p.nombre AS paralelo, i.estado, e.sexo, e.telefono
                FROM inscripcion i
                JOIN estudiante e ON e.id = i.id_estudiante AND e.id_institucion = i.id_institucion
                JOIN paralelo p ON p.id = i.id_paralelo AND p.id_institucion = i.id_institucion
                JOIN curso c ON c.id = p.id_curso AND c.id_institucion = p.id_institucion
                """ + where + " ORDER BY c.nombre ASC, p.nombre ASC, e.apellidos ASC, e.nombres ASC";

        return ReporteQuerySupport.execute(
                jdbc,
                metadata().nombre(),
                context.usuarioNombre(),
                ReporteQuerySupport.headerFilters(context, ReporteQuerySupport.filtro("Estado inscripción", context.filtros().get("estado"))),
                List.of(
                        col("codigoEstudiante", "Código", "text"),
                        col("estudiante", "Estudiante", "text"),
                        col("curso", "Curso", "text"),
                        col("paralelo", "Paralelo", "text"),
                        col("estado", "Estado", "text"),
                        col("sexo", "Sexo", "text"),
                        col("telefono", "Teléfono", "text")
                ),
                sql,
                params,
                context.page(),
                context.size(),
                null
        );
    }
}
