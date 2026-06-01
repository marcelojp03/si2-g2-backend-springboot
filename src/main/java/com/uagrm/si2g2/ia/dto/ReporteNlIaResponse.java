package com.uagrm.si2g2.ia.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Respuesta de FastAPI POST /api/ia/reporte/nl-a-reporte.
 * Contiene el código del handler elegido por GPT-4o-mini y los filtros no-UUID.
 * Los campos *Query son nombres de entidades que Spring Boot resolverá a UUID via DB.
 */
public record ReporteNlIaResponse(
        @JsonProperty("codigo_reporte")  String codigoReporte,
        @JsonProperty("filtros")         Map<String, Object> filtros,
        @JsonProperty("materia_query")   String materiaQuery,
        @JsonProperty("curso_query")     String cursoQuery,
        @JsonProperty("docente_query")   String docenteQuery,
        @JsonProperty("confianza")       double confianza,
        @JsonProperty("mensaje_error")   String mensajeError
) {}
