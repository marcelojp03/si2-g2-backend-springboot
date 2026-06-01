package com.uagrm.si2g2.reporte.export;

import com.uagrm.si2g2.reporte.dto.ReporteColumnResponse;
import com.uagrm.si2g2.reporte.dto.ReportePreviewResponse;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
public class CsvReporteExporter implements ReporteExporter {
    @Override public String formato() { return "CSV"; }
    @Override public String contentType() { return "text/csv; charset=UTF-8"; }
    @Override public String extension() { return "csv"; }

    @Override
    public byte[] exportar(ReportePreviewResponse reporte) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("Reporte;").append(escape(reporte.encabezado().nombreReporte())).append('\n');
        csv.append("Generado en;").append(escape(reporte.encabezado().generadoEn())).append('\n');
        csv.append("Usuario;").append(escape(reporte.encabezado().usuario())).append('\n');
        for (String filtro : reporte.encabezado().filtrosAplicados()) {
            csv.append("Filtro;").append(escape(filtro)).append('\n');
        }
        csv.append('\n');

        List<ReporteColumnResponse> columnas = reporte.columnas();
        csv.append(String.join(";", columnas.stream().map(ReporteColumnResponse::header).map(this::escape).toList())).append('\n');
        for (Map<String, Object> fila : reporte.filas()) {
            for (int i = 0; i < columnas.size(); i++) {
                if (i > 0) csv.append(';');
                csv.append(escape(fila.get(columnas.get(i).field())));
            }
            csv.append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escape(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        boolean quote = text.contains(";") || text.contains("\"") || text.contains("\n") || text.contains("\r");
        text = text.replace("\"", "\"\"");
        return quote ? "\"" + text + "\"" : text;
    }
}
