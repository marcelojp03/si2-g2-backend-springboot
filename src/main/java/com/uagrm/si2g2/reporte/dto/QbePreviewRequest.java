package com.uagrm.si2g2.reporte.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record QbePreviewRequest(
        @NotBlank String entidad,
        List<QbeConditionRequest> condiciones,
        List<String> columnas,
        ReportePresentacionRequest presentacion,
        Integer page,
        Integer size
) {
}
