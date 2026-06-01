package com.uagrm.si2g2.reporte.export;

import com.uagrm.si2g2.reporte.dto.ReporteColumnResponse;
import com.uagrm.si2g2.reporte.dto.ReportePreviewResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@Component
public class XlsxReporteExporter implements ReporteExporter {
    @Override public String formato() { return "XLSX"; }
    @Override public String contentType() { return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"; }
    @Override public String extension() { return "xlsx"; }

    @Override
    public byte[] exportar(ReportePreviewResponse reporte) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Reporte");
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            int rowIndex = 0;
            rowIndex = writePair(sheet, rowIndex, "Reporte", reporte.encabezado().nombreReporte(), headerStyle);
            rowIndex = writePair(sheet, rowIndex, "Generado en", reporte.encabezado().generadoEn().toString(), headerStyle);
            rowIndex = writePair(sheet, rowIndex, "Usuario", reporte.encabezado().usuario(), headerStyle);
            for (String filtro : reporte.encabezado().filtrosAplicados()) {
                rowIndex = writePair(sheet, rowIndex, "Filtro", filtro, headerStyle);
            }
            rowIndex++;

            List<ReporteColumnResponse> columnas = reporte.columnas();
            Row header = sheet.createRow(rowIndex++);
            for (int i = 0; i < columnas.size(); i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columnas.get(i).header());
                cell.setCellStyle(headerStyle);
            }
            for (Map<String, Object> fila : reporte.filas()) {
                Row row = sheet.createRow(rowIndex++);
                for (int i = 0; i < columnas.size(); i++) {
                    Object value = fila.get(columnas.get(i).field());
                    Cell cell = row.createCell(i);
                    if (value instanceof Number number) cell.setCellValue(number.doubleValue());
                    else cell.setCellValue(value == null ? "" : String.valueOf(value));
                }
            }
            for (int i = 0; i < columnas.size(); i++) sheet.autoSizeColumn(i);
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el archivo XLSX", e);
        }
    }

    private int writePair(Sheet sheet, int rowIndex, String key, String value, CellStyle style) {
        Row row = sheet.createRow(rowIndex++);
        Cell keyCell = row.createCell(0);
        keyCell.setCellValue(key);
        keyCell.setCellStyle(style);
        row.createCell(1).setCellValue(value == null ? "" : value);
        return rowIndex;
    }
}
