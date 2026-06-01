package com.uagrm.si2g2.reporte.export;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.uagrm.si2g2.reporte.dto.ReporteColumnResponse;
import com.uagrm.si2g2.reporte.dto.ReportePreviewResponse;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

@Component
public class PdfReporteExporter implements ReporteExporter {
    @Override public String formato() { return "PDF"; }
    @Override public String contentType() { return "application/pdf"; }
    @Override public String extension() { return "pdf"; }

    @Override
    public byte[] exportar(ReportePreviewResponse reporte) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html(reporte), null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF", e);
        }
    }

    private String html(ReportePreviewResponse reporte) {
        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html><head><meta charset='UTF-8'/><style>")
                .append("body{font-family:Arial,sans-serif;color:#111827;font-size:11px} h1{font-size:18px;margin-bottom:4px}")
                .append(".meta{margin-bottom:12px;color:#374151}.meta div{margin:2px 0}")
                .append("table{width:100%;border-collapse:collapse}th{background:#0f766e;color:white;text-align:left}")
                .append("th,td{border:1px solid #d1d5db;padding:5px}tr:nth-child(even){background:#f9fafb}")
                .append("</style></head><body>");
        html.append("<h1>").append(escape(reporte.encabezado().nombreReporte())).append("</h1><div class='meta'>")
                .append("<div><b>Generado en:</b> ").append(escape(reporte.encabezado().generadoEn())).append("</div>")
                .append("<div><b>Usuario:</b> ").append(escape(reporte.encabezado().usuario())).append("</div>");
        for (String filtro : reporte.encabezado().filtrosAplicados()) {
            html.append("<div><b>Filtro:</b> ").append(escape(filtro)).append("</div>");
        }
        html.append("</div><table><thead><tr>");
        for (ReporteColumnResponse col : reporte.columnas()) html.append("<th>").append(escape(col.header())).append("</th>");
        html.append("</tr></thead><tbody>");
        reporte.filas().forEach(row -> {
            html.append("<tr>");
            for (ReporteColumnResponse col : reporte.columnas()) html.append("<td>").append(escape(row.get(col.field()))).append("</td>");
            html.append("</tr>");
        });
        html.append("</tbody></table></body></html>");
        return html.toString();
    }

    private String escape(Object value) {
        return String.valueOf(value == null ? "" : value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
