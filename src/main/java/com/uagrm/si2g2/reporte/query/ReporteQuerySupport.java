package com.uagrm.si2g2.reporte.query;

import com.uagrm.si2g2.reporte.dto.*;
import com.uagrm.si2g2.reporte.application.ReporteExecutionContext;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public final class ReporteQuerySupport {

    private ReporteQuerySupport() {}

    public static final List<String> EXPORT_FORMATS = List.of("PDF", "XLSX", "CSV");

    public static ReportePreviewResponse execute(
            NamedParameterJdbcTemplate jdbc,
            String nombreReporte,
            String usuarioNombre,
            List<String> filtrosAplicados,
            List<ReporteColumnResponse> columnas,
            String dataSql,
            MapSqlParameterSource params,
            int page,
            int size,
            ReporteChartResponse grafico
    ) {
        int offset = Math.max(page, 0) * size;
        MapSqlParameterSource dataParams = copy(params)
                .addValue("limit", size)
                .addValue("offset", offset);

        String countSql = "SELECT COUNT(*) FROM (" + dataSql + ") reporte_count";
        Long total = jdbc.queryForObject(countSql, params, Long.class);
        List<Map<String, Object>> filas = normalizeRows(jdbc.queryForList(dataSql + " LIMIT :limit OFFSET :offset", dataParams));

        return new ReportePreviewResponse(
                new ReporteHeaderResponse(nombreReporte, Instant.now(), usuarioNombre, filtrosAplicados),
                columnas,
                filas,
                grafico,
                total == null ? 0 : total,
                page,
                size
        );
    }

    public static ReporteChartResponse chartFromRows(List<Map<String, Object>> rows, String type, String labelField, String valueField, String datasetLabel) {
        List<String> labels = new ArrayList<>();
        List<BigDecimal> data = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            labels.add(String.valueOf(row.getOrDefault(labelField, "")));
            data.add(toBigDecimal(row.get(valueField)));
        }
        return new ReporteChartResponse(type, labels, List.of(new ReporteDatasetResponse(datasetLabel, data, "#0f766e")));
    }

    public static MapSqlParameterSource copy(MapSqlParameterSource source) {
        MapSqlParameterSource copy = new MapSqlParameterSource();
        for (String name : source.getParameterNames()) {
            copy.addValue(name, source.getValue(name));
        }
        return copy;
    }

    public static void addUuidFilter(StringBuilder where, MapSqlParameterSource params, Map<String, Object> filtros, String field, String column) {
        UUID value = uuid(filtros.get(field));
        if (value != null) {
            where.append(" AND ").append(column).append(" = :").append(field);
            params.addValue(field, value);
        }
    }

    public static void addTextFilter(StringBuilder where, MapSqlParameterSource params, Map<String, Object> filtros, String field, String column) {
        String value = text(filtros.get(field));
        if (value != null) {
            where.append(" AND UPPER(").append(column).append(") = :").append(field);
            params.addValue(field, value.toUpperCase(Locale.ROOT));
        }
    }

    public static UUID uuid(Object value) {
        String text = text(value);
        if (text == null) return null;
        return UUID.fromString(text);
    }

    public static Integer integer(Object value) {
        String text = text(value);
        if (text == null) return null;
        return Integer.valueOf(text);
    }

    public static BigDecimal decimal(Object value) {
        String text = text(value);
        if (text == null) return null;
        return new BigDecimal(text);
    }

    public static String text(Object value) {
        if (value == null) return null;
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }

    public static String filtro(String label, Object value) {
        String text = text(value);
        return text == null ? null : label + ": " + text;
    }

    public static List<String> cleanFilters(String... filters) {
        return Arrays.stream(filters).filter(Objects::nonNull).toList();
    }

    public static List<String> headerFilters(ReporteExecutionContext context, String... filters) {
        List<String> lines = new ArrayList<>();
        lines.add("Institución filtrada: " + context.institucionNombre() + " (UUID: " + context.idInstitucion() + ")");
        lines.add("Nota: solo se incluyen datos de su institución.");
        if (context.headerNotes() != null) {
            lines.addAll(context.headerNotes().stream().filter(Objects::nonNull).toList());
        }
        lines.addAll(cleanFilters(filters));
        return lines;
    }

    private static List<Map<String, Object>> normalizeRows(List<Map<String, Object>> rows) {
        return rows.stream().map(row -> {
            Map<String, Object> normalized = new LinkedHashMap<>();
            row.forEach((key, value) -> normalized.put(toCamelCase(key), value));
            return normalized;
        }).toList();
    }

    private static String toCamelCase(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder();
        boolean upperNext = false;
        for (char c : lower.toCharArray()) {
            if (c == '_') {
                upperNext = true;
            } else if (upperNext) {
                result.append(Character.toUpperCase(c));
                upperNext = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
        return BigDecimal.ZERO;
    }
}
