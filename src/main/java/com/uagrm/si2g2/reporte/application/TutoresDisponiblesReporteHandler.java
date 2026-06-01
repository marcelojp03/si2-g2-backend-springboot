package com.uagrm.si2g2.reporte.application;

import com.uagrm.si2g2.reporte.dto.ReporteMetadataResponse;
import com.uagrm.si2g2.reporte.dto.ReportePreviewResponse;
import com.uagrm.si2g2.reporte.query.ReporteQuerySupport;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TutoresDisponiblesReporteHandler extends AbstractReporteHandler {

    public static final String CODIGO = "TUTORES_DISPONIBLES";

    public TutoresDisponiblesReporteHandler(NamedParameterJdbcTemplate jdbc) {
        super(jdbc);
    }

    @Override
    public String codigo() {
        return CODIGO;
    }

    @Override
    public ReporteMetadataResponse metadata() {
        return metadata(CODIGO, "Tutores disponibles", "Listado de tutores registrados en la institución", "PREDEFINIDO", false,
                List.of(filter("estado", "Estado", "text", false), filter("sortBy", "Ordenar por", "text", false), filter("sortDirection", "Dirección orden", "text", false)));
    }

    @Override
    public ReportePreviewResponse ejecutar(ReporteExecutionContext context) {
        StringBuilder where = new StringBuilder(" WHERE t.id_institucion = :idInstitucion");
        MapSqlParameterSource params = new MapSqlParameterSource("idInstitucion", context.idInstitucion());
        ReporteQuerySupport.addTextFilter(where, params, context.filtros(), "estado", "t.estado");
        String orderBy = resolveOrderBy(context);

        String sql = """
                SELECT t.documento_identidad, CONCAT(t.apellidos, ' ', t.nombres) AS tutor,
                       t.telefono, t.correo, t.direccion, t.estado
                FROM tutor t
                """ + where + " ORDER BY " + orderBy;

        return ReporteQuerySupport.execute(
                jdbc,
                metadata().nombre(),
                context.usuarioNombre(),
                ReporteQuerySupport.headerFilters(context, ReporteQuerySupport.filtro("Estado", context.filtros().get("estado"))),
                List.of(
                        col("documentoIdentidad", "Documento", "text"),
                        col("tutor", "Tutor", "text"),
                        col("telefono", "Teléfono", "text"),
                        col("correo", "Correo", "text"),
                        col("direccion", "Dirección", "text"),
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
        String sortBy = String.valueOf(context.filtros().getOrDefault("sortBy", "tutor")).toLowerCase();
        String direction = "DESC".equalsIgnoreCase(String.valueOf(context.filtros().getOrDefault("sortDirection", "ASC"))) ? "DESC" : "ASC";
        return switch (sortBy) {
            case "documento" -> "t.documento_identidad " + direction + ", tutor ASC";
            case "estado" -> "t.estado " + direction + ", tutor ASC";
            default -> "t.apellidos " + direction + ", t.nombres " + direction;
        };
    }
}
