package com.uagrm.si2g2.reporte.application;

import com.uagrm.si2g2.reporte.dto.ReporteMetadataResponse;
import com.uagrm.si2g2.reporte.dto.ReportePreviewResponse;
import com.uagrm.si2g2.reporte.query.ReporteQuerySupport;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AgrupacionGenericaReporteHandler extends AbstractReporteHandler {

    public static final String CODIGO = "AGRUPACION_GENERICA";

    public AgrupacionGenericaReporteHandler(NamedParameterJdbcTemplate jdbc) {
        super(jdbc);
    }

    @Override
    public String codigo() {
        return CODIGO;
    }

    @Override
    public ReporteMetadataResponse metadata() {
        return metadata(CODIGO, "Agrupación genérica", "Agrupaciones seguras por entidad y dimensión permitidas", "ANALITICO", false,
                List.of(filter("groupEntity", "Entidad", "text", true), filter("groupBy", "Agrupar por", "text", true), filter("sortDirection", "Dirección orden", "text", false)));
    }

    @Override
    public ReportePreviewResponse ejecutar(ReporteExecutionContext context) {
        String entity = String.valueOf(context.filtros().getOrDefault("groupEntity", "")).toUpperCase();
        String groupBy = String.valueOf(context.filtros().getOrDefault("groupBy", "")).toUpperCase();
        String direction = "ASC".equalsIgnoreCase(String.valueOf(context.filtros().getOrDefault("sortDirection", "DESC"))) ? "ASC" : "DESC";

        GroupDefinition definition = definition(entity, groupBy);
        MapSqlParameterSource params = new MapSqlParameterSource("idInstitucion", context.idInstitucion());
        String sql = "SELECT " + definition.labelSql() + " AS grupo, COUNT(*) AS total FROM " + definition.fromSql() +
                " WHERE " + definition.tenantColumn() + " = :idInstitucion GROUP BY " + definition.groupSql() + " ORDER BY total " + direction + ", grupo ASC";

        return ReporteQuerySupport.execute(
                jdbc,
                metadata().nombre() + " - " + entity + " por " + groupBy,
                context.usuarioNombre(),
                ReporteQuerySupport.headerFilters(context, ReporteQuerySupport.filtro("Entidad agrupada", entity), ReporteQuerySupport.filtro("Agrupar por", groupBy)),
                List.of(col("grupo", "Grupo", "text"), col("total", "Total", "number")),
                sql,
                params,
                context.page(),
                context.size(),
                null
        );
    }

    private GroupDefinition definition(String entity, String groupBy) {
        return switch (entity + ":" + groupBy) {
            case "DOCENTE:MATERIA" -> new GroupDefinition(
                    "materia m LEFT JOIN docente_materia dm ON dm.id_materia = m.id LEFT JOIN docente d ON d.id = dm.id_docente AND d.id_institucion = m.id_institucion",
                    "m.id_institucion", "m.nombre", "m.nombre");
            case "ESTUDIANTE:CURSO" -> new GroupDefinition(
                    "inscripcion i JOIN paralelo p ON p.id = i.id_paralelo AND p.id_institucion = i.id_institucion JOIN curso c ON c.id = p.id_curso AND c.id_institucion = p.id_institucion",
                    "i.id_institucion", "c.nombre", "c.nombre");
            case "ESTUDIANTE:PARALELO" -> new GroupDefinition(
                    "inscripcion i JOIN paralelo p ON p.id = i.id_paralelo AND p.id_institucion = i.id_institucion JOIN curso c ON c.id = p.id_curso AND c.id_institucion = p.id_institucion",
                    "i.id_institucion", "CONCAT(c.nombre, ' ', p.nombre)", "CONCAT(c.nombre, ' ', p.nombre)");
            case "MATERIA:AREA" -> new GroupDefinition(
                    "materia m", "m.id_institucion", "COALESCE(m.area, 'Sin área')", "COALESCE(m.area, 'Sin área')");
            case "TUTOR:ESTADO" -> new GroupDefinition(
                    "tutor t", "t.id_institucion", "t.estado", "t.estado");
            default -> throw new IllegalArgumentException("Agrupación no soportada: " + entity + " por " + groupBy);
        };
    }

    private record GroupDefinition(String fromSql, String tenantColumn, String groupSql, String labelSql) {
    }
}
