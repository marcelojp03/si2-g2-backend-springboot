package com.uagrm.si2g2.reporte.export;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ReporteExporterRegistry {
    private final Map<String, ReporteExporter> exporters;

    public ReporteExporterRegistry(List<ReporteExporter> exporters) {
        this.exporters = exporters.stream().collect(Collectors.toUnmodifiableMap(ReporteExporter::formato, Function.identity()));
    }

    public ReporteExporter get(String formato) {
        ReporteExporter exporter = exporters.get(formato.toUpperCase(Locale.ROOT));
        if (exporter == null) throw new EntityNotFoundException("Formato de exportación no soportado: " + formato);
        return exporter;
    }
}
