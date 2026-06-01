package com.uagrm.si2g2.reporte.dto;

import java.util.List;

public record QbeFieldDefinitionResponse(
        String field,
        String label,
        String type,
        List<String> operators
) {
}
