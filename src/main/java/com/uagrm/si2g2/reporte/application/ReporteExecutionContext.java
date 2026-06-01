package com.uagrm.si2g2.reporte.application;

import com.uagrm.si2g2.reporte.dto.ReportePresentacionRequest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ReporteExecutionContext(
        UUID idInstitucion,
        String institucionNombre,
        UUID idUsuario,
        String usuarioNombre,
        Map<String, Object> filtros,
        List<String> headerNotes,
        ReportePresentacionRequest presentacion,
        int page,
        int size
) {
}
