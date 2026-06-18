package com.uagrm.si2g2.reporte.application;

import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.curso.domain.CursoRepository;
import com.uagrm.si2g2.curso.domain.ParaleloRepository;
import com.uagrm.si2g2.docente.domain.DocenteRepository;
import com.uagrm.si2g2.ia.application.AiIntegrationService;
import com.uagrm.si2g2.ia.dto.ReporteNlIaRequest;
import com.uagrm.si2g2.ia.dto.ReporteNlIaResponse;
import com.uagrm.si2g2.materia.domain.MateriaRepository;
import com.uagrm.si2g2.reporte.dto.ReporteNaturalLanguageRequest;
import com.uagrm.si2g2.reporte.dto.ReportePreviewRequest;
import com.uagrm.si2g2.reporte.dto.ReportePreviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Traduce consultas en lenguaje natural a {@link ReportePreviewRequest} usando IA via FastAPI.
 * La deteccion de intencion y extraccion de filtros la hace la IA; este servicio se encarga de:
 * <ul>
 *   <li>Validar restricciones de seguridad multi-tenant antes de llamar a la IA.</li>
 *   <li>Resolver nombres de entidades (materia, curso, docente) a UUIDs via repositorios locales.</li>
 *   <li>Construir el {@link ReportePreviewRequest} final para el motor de reportes.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ReporteNaturalLanguageService {

    private final ReporteService reporteService;
    private final AiIntegrationService aiIntegrationService;
    private final MateriaRepository materiaRepository;
    private final CursoRepository cursoRepository;
    private final ParaleloRepository paraleloRepository;
    private final DocenteRepository docenteRepository;

    @Transactional(readOnly = true)
    public ReportePreviewResponse preview(ReporteNaturalLanguageRequest request, String authHeader) {
        Translation translation = translate(request, authHeader);
        return reporteService.previewConNotas(
                translation.request(), translation.headerNotes(),
                "NL_PREVIEW", "Vista previa de reporte por lenguaje natural");
    }

    @Transactional(readOnly = true)
    public ReporteService.ExportedReport exportar(ReporteNaturalLanguageRequest request, String formato, String authHeader) {
        Translation translation = translate(request, authHeader);
        return reporteService.exportarConNotas(
                translation.request(), formato, translation.headerNotes(),
                "NL_EXPORTAR", "Exportacion de reporte por lenguaje natural");
    }

    // -- Logica central --------------------------------------------------------

    private Translation translate(ReporteNaturalLanguageRequest request, String authHeader) {
        String raw = request.consulta();
        validateForbiddenRequests(normalize(raw));

        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();

        // FastAPI detecta el reporte adecuado y extrae filtros no-UUID via IA
        ReporteNlIaResponse ia = aiIntegrationService.nlAReporte(new ReporteNlIaRequest(raw), authHeader);

        if ("ERROR".equals(ia.codigoReporte())) {
            throw new IllegalArgumentException(
                    ia.mensajeError() != null ? ia.mensajeError() : "No pude interpretar la consulta. Intente ser mas especifico.");
        }

        Map<String, Object> filtros = ia.filtros() != null ? new LinkedHashMap<>(ia.filtros()) : new LinkedHashMap<>();

        // Resolucion de nombres de entidades a UUIDs (multi-tenant, seguro en backend)
        if (ia.materiaQuery() != null) {
            String mq = normalize(ia.materiaQuery());
            materiaRepository.findAllByIdInstitucion(idInstitucion).stream()
                    .filter(m -> normalize(m.getNombre()).contains(mq) || normalize(m.getCodigo()).contains(mq))
                    .findFirst()
                    .ifPresent(m -> filtros.put("idMateria", m.getId().toString()));
        }

        if (ia.cursoQuery() != null) {
            String cq = normalize(ia.cursoQuery());
            cursoRepository.findAllByIdInstitucion(idInstitucion).stream()
                    .filter(c -> normalize(c.getNombre()).contains(cq))
                    .findFirst()
                    .ifPresent(c -> filtros.put("idCurso", c.getId().toString()));
        }

        if (ia.docenteQuery() != null) {
            String dq = normalize(ia.docenteQuery());
            docenteRepository.findAllByIdInstitucion(idInstitucion).stream()
                    .filter(d -> normalize(d.getNombres()).contains(dq) || normalize(d.getApellidos()).contains(dq))
                    .findFirst()
                    .ifPresent(d -> filtros.put("idDocente", d.getId().toString()));
        }

        // Para ESTUDIANTES_POR_CURSO_PARALELO: si hay idCurso pero no idParalelo, tomar el primero
        if (EstudiantesPorCursoParaleloReporteHandler.CODIGO.equals(ia.codigoReporte())
                && filtros.containsKey("idCurso") && !filtros.containsKey("idParalelo")) {
            UUID idCurso = UUID.fromString((String) filtros.get("idCurso"));
            paraleloRepository.findAllByIdInstitucion(idInstitucion).stream()
                    .filter(p -> p.getIdCurso().equals(idCurso))
                    .findFirst()
                    .ifPresent(p -> filtros.put("idParalelo", p.getId().toString()));
        }

        // pageSize: del filtro IA (si incluye pageSize) o del request original
        int pageSize = filtros.containsKey("pageSize")
                ? ((Number) filtros.remove("pageSize")).intValue()
                : (request.size() != null ? request.size() : 25);

        ReportePreviewRequest translatedRequest = new ReportePreviewRequest(
                ia.codigoReporte(),
                filtros,
                request.presentacion(),
                request.page(),
                pageSize
        );

        return new Translation(translatedRequest, List.of(
                "Consulta original: " + raw,
                "Reporte detectado: " + ia.codigoReporte()
                        + " -- confianza " + String.format("%.0f%%", ia.confianza() * 100),
                "Modo: lenguaje natural con IA; id_institucion inyectado en backend (multi-tenant seguro)"
        ));
    }

    // -- Seguridad -------------------------------------------------------------

    private void validateForbiddenRequests(String normalized) {
        if (containsAny(normalized,
                "comparar con colegio", "comparar con institucion", "otra institucion",
                "otro colegio", "otra unidad educativa", "todos los colegios")) {
            throw new IllegalArgumentException("Solo puedes consultar datos de tu propia institucion");
        }
        if (containsAny(normalized, "uuid de otra institucion", "institution uuid de otra")) {
            throw new IllegalArgumentException("No puedes consultar ni forzar el UUID de otra institucion");
        }
        if (containsAny(normalized, "asistencia docente", "profesores que faltaron", "docentes que faltaron")) {
            throw new IllegalArgumentException("No existe un modelo de asistencia docente en este modulo");
        }
    }

    // -- Utilidades ------------------------------------------------------------

    private String normalize(String value) {
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) return true;
        }
        return false;
    }

    private record Translation(ReportePreviewRequest request, List<String> headerNotes) {}
}