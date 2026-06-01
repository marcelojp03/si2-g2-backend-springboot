package com.uagrm.si2g2.reporte.application;

import com.uagrm.si2g2.reporte.dto.ReporteMetadataResponse;
import com.uagrm.si2g2.reporte.dto.ReportePreviewResponse;
import com.uagrm.si2g2.reporte.query.ReporteQuerySupport;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MateriasDisponiblesReporteHandler extends AbstractReporteHandler {

    public static final String CODIGO = "MATERIAS_DISPONIBLES";

    public MateriasDisponiblesReporteHandler(NamedParameterJdbcTemplate jdbc) {
        super(jdbc);
    }

    @Override
    public String codigo() {
        return CODIGO;
    }

    @Override
    public ReporteMetadataResponse metadata() {
        return metadata(CODIGO, "Materias disponibles", "Listado de materias registradas en la institución", "PREDEFINIDO", false,
                List.of(filter("estado", "Estado", "text", false), filter("sortBy", "Ordenar por", "text", false), filter("sortDirection", "Dirección orden", "text", false)));
    }

    @Override
    public ReportePreviewResponse ejecutar(ReporteExecutionContext context) {
        StringBuilder where = new StringBuilder(" WHERE m.id_institucion = :idInstitucion");
        MapSqlParameterSource params = new MapSqlParameterSource("idInstitucion", context.idInstitucion());
        ReporteQuerySupport.addTextFilter(where, params, context.filtros(), "estado", "m.estado");
        String orderBy = resolveOrderBy(context);

        String sql = """
                SELECT m.codigo, m.nombre, m.area, m.carga_horaria, m.estado
                FROM materia m
                """ + where + " ORDER BY " + orderBy;

        return ReporteQuerySupport.execute(
                jdbc,
                metadata().nombre(),
                context.usuarioNombre(),
                ReporteQuerySupport.headerFilters(context, ReporteQuerySupport.filtro("Estado", context.filtros().get("estado")), ReporteQuerySupport.filtro("Orden", orderBy)),
                List.of(
                        col("codigo", "Código", "text"),
                        col("nombre", "Materia", "text"),
                        col("area", "Área", "text"),
                        col("cargaHoraria", "Carga horaria", "number"),
                        col("estado", "Estado", "text")
                ),
                sql,
                params,
                context.page(),
                context.size(),
                null
        );
    }

    private String resolveOrderBy(ReporteExecutionContext context) {
        String sortBy = String.valueOf(context.filtros().getOrDefault("sortBy", "nombre")).toLowerCase();
        String direction = "DESC".equalsIgnoreCase(String.valueOf(context.filtros().getOrDefault("sortDirection", "ASC"))) ? "DESC" : "ASC";
        return switch (sortBy) {
            case "codigo" -> "m.codigo " + direction + ", m.nombre ASC";
            case "area" -> "m.area " + direction + ", m.nombre ASC";
            case "cargahoraria" -> "m.carga_horaria " + direction + ", m.nombre ASC";
            default -> "m.nombre " + direction;
        };
    }
}
