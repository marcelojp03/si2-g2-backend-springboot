package com.uagrm.si2g2.reporte.dto;

import java.util.List;

public record ReportePresentacionRequest(
        String formato,
        String tipoGrafico,
        String ejeX,
        String ejeY,
        String agrupacion,
        List<String> series,
        Boolean mostrarLeyenda,
        Boolean mostrarEtiquetas
) {
}
