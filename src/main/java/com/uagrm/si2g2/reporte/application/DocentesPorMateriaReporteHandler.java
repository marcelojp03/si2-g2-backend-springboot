package com.uagrm.si2g2.reporte.application;

import com.uagrm.si2g2.reporte.dto.ReporteMetadataResponse;
import com.uagrm.si2g2.reporte.dto.ReportePreviewResponse;
import com.uagrm.si2g2.reporte.query.ReporteQuerySupport;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DocentesPorMateriaReporteHandler extends AbstractReporteHandler {

    public static final String CODIGO = "DOCENTES_POR_MATERIA";

    public DocentesPorMateriaReporteHandler(NamedParameterJdbcTemplate jdbc) {
        super(jdbc);
    }

    @Override
    public String codigo() {
        return CODIGO;
    }

    @Override
    public ReporteMetadataResponse metadata() {
        return metadata(CODIGO, "Docentes agrupados por materia", "Agrupa docentes por materia con totales y listado resumido", "ANALITICO", false,
                List.of(filter("idMateria", "Materia", "uuid", false), filter("sortDirection", "Dirección orden", "text", false)));
    }

    @Override
    public ReportePreviewResponse ejecutar(ReporteExecutionContext context) {
        StringBuilder where = new StringBuilder(" WHERE m.id_institucion = :idInstitucion");
        MapSqlParameterSource params = new MapSqlParameterSource("idInstitucion", context.idInstitucion());
        ReporteQuerySupport.addUuidFilter(where, params, context.filtros(), "idMateria", "m.id");
        String direction = "ASC".equalsIgnoreCase(String.valueOf(context.filtros().getOrDefault("sortDirection", "DESC"))) ? "ASC" : "DESC";

        String sql = """
                SELECT m.codigo, m.nombre AS materia,
                       COUNT(DISTINCT d.id) AS total_docentes,
                       STRING_AGG(DISTINCT CONCAT(d.apellidos, ' ', d.nombres), ', ' ORDER BY CONCAT(d.apellidos, ' ', d.nombres)) AS docentes
                FROM materia m
                LEFT JOIN asignacion_docente ad ON ad.id_materia = m.id AND ad.id_institucion = m.id_institucion
                LEFT JOIN docente d ON d.id = ad.id_docente AND d.id_institucion = m.id_institucion
                """ + where + " GROUP BY m.id, m.codigo, m.nombre ORDER BY total_docentes " + direction + ", m.nombre ASC";

        return ReporteQuerySupport.execute(
                jdbc,
                metadata().nombre(),
                context.usuarioNombre(),
                ReporteQuerySupport.headerFilters(context, ReporteQuerySupport.filtro("Orden total docentes", direction)),
                List.of(
                        col("codigo", "Código", "text"),
                        col("materia", "Materia", "text"),
                        col("totalDocentes", "Total docentes", "number"),
                        col("docentes", "Docentes agrupados", "text")
                ),
                sql,
                params,
                context.page(),
                context.size(),
                null
        );
    }
}
