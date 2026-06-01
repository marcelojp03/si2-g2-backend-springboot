package com.uagrm.si2g2.reporte.dto;

import java.util.List;

public record QbeEntityDefinitionResponse(
        String entity,
        String label,
        List<QbeFieldDefinitionResponse> fields
) {
}
