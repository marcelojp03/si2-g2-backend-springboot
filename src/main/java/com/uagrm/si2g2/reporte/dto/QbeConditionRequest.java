package com.uagrm.si2g2.reporte.dto;

public record QbeConditionRequest(
        String campo,
        String operador,
        String valor,
        String valorHasta
) {
}
