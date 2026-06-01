package com.uagrm.si2g2.reporte.application;

import com.uagrm.si2g2.reporte.dto.ReporteColumnResponse;
import com.uagrm.si2g2.reporte.dto.ReporteFilterDefinitionResponse;
import com.uagrm.si2g2.reporte.dto.ReporteMetadataResponse;
import com.uagrm.si2g2.reporte.query.ReporteQuerySupport;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;

public abstract class AbstractReporteHandler implements ReporteHandler {

    protected final NamedParameterJdbcTemplate jdbc;

    protected AbstractReporteHandler(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    protected ReporteMetadataResponse metadata(String codigo, String nombre, String descripcion, String tipo, boolean grafico, List<ReporteFilterDefinitionResponse> filtros) {
        return new ReporteMetadataResponse(codigo, nombre, descripcion, tipo, grafico, filtros, ReporteQuerySupport.EXPORT_FORMATS);
    }

    protected ReporteColumnResponse col(String field, String header, String type) {
        return new ReporteColumnResponse(field, header, type);
    }

    protected ReporteFilterDefinitionResponse filter(String field, String label, String type, boolean required) {
        return new ReporteFilterDefinitionResponse(field, label, type, required, List.of());
    }
}
