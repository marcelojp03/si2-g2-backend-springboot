package com.uagrm.si2g2.reporte.application;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ReporteRegistry {

    private final Map<String, ReporteHandler> handlers;

    public ReporteRegistry(List<ReporteHandler> handlers) {
        this.handlers = handlers.stream().collect(Collectors.toUnmodifiableMap(ReporteHandler::codigo, Function.identity()));
    }

    public ReporteHandler get(String codigo) {
        ReporteHandler handler = handlers.get(codigo);
        if (handler == null) {
            throw new EntityNotFoundException("Reporte no encontrado: " + codigo);
        }
        return handler;
    }

    public List<ReporteHandler> list() {
        return handlers.values().stream()
                .sorted(Comparator.comparing(handler -> handler.metadata().nombre()))
                .toList();
    }
}
