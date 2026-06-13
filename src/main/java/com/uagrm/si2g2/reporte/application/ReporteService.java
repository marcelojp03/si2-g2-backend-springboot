package com.uagrm.si2g2.reporte.application;

import com.uagrm.si2g2.auth.domain.Usuario;
import com.uagrm.si2g2.auditoria.application.AuditoriaService;
import com.uagrm.si2g2.common.SecurityUtils;
import com.uagrm.si2g2.institucion.domain.Institucion;
import com.uagrm.si2g2.institucion.domain.InstitucionRepository;
import com.uagrm.si2g2.reporte.dto.ReporteMetadataResponse;
import com.uagrm.si2g2.reporte.dto.ReportePreviewRequest;
import com.uagrm.si2g2.reporte.dto.ReportePreviewResponse;
import com.uagrm.si2g2.reporte.export.ReporteExporter;
import com.uagrm.si2g2.reporte.export.ReporteExporterRegistry;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteService {

    private static final int MAX_PREVIEW_SIZE = 100;
    private static final int MAX_EXPORT_ROWS = 10_000;

    private final JdbcTemplate jdbc;
    private final ReporteRegistry registry;
    private final ReporteExporterRegistry exporterRegistry;
    private final AuditoriaService auditoriaService;
    private final InstitucionRepository institucionRepository;

    @Transactional(readOnly = true)
    public List<ReporteMetadataResponse> catalogo() {
        return registry.list().stream().map(ReporteHandler::metadata).toList();
    }

    @Transactional(readOnly = true)
    public ReportePreviewResponse preview(ReportePreviewRequest request) {
        ReportePreviewResponse response = ejecutar(request, sanitizePage(request.page()), sanitizeSize(request.size()), List.of());
        auditoriaService.registrar(SecurityUtils.requireCurrentInstitutionId(), SecurityUtils.currentUserId(),
                "REPORTES", "PREVIEW", "reporte", request.codigoReporte(), true, "Vista previa de reporte");
        return response;
    }

    @Transactional(readOnly = true)
    public ReportePreviewResponse previewConNotas(ReportePreviewRequest request, List<String> headerNotes, String auditOperation, String auditMessage) {
        ReportePreviewResponse response = ejecutar(request, sanitizePage(request.page()), sanitizeSize(request.size()), headerNotes);
        auditoriaService.registrar(SecurityUtils.requireCurrentInstitutionId(), SecurityUtils.currentUserId(),
                "REPORTES", auditOperation, "reporte", request.codigoReporte(), true, auditMessage);
        return response;
    }

    @Transactional(readOnly = true)
    public ExportedReport exportar(ReportePreviewRequest request, String formato) {
        ReportePreviewResponse response = ejecutar(request, 0, MAX_EXPORT_ROWS, List.of());
        if (response.totalRegistros() > MAX_EXPORT_ROWS) {
            throw new IllegalArgumentException("El reporte supera " + MAX_EXPORT_ROWS + " registros. Aplique filtros más específicos antes de exportar.");
        }
        ReporteExporter exporter = exporterRegistry.get(formato);
        byte[] bytes = exporter.exportar(response);
        auditoriaService.registrar(SecurityUtils.requireCurrentInstitutionId(), SecurityUtils.currentUserId(),
                "REPORTES", "EXPORTAR_" + exporter.formato(), "reporte", request.codigoReporte(), true, "Exportación de reporte");
        return new ExportedReport(bytes, exporter.contentType(), fileName(response.encabezado().nombreReporte(), exporter.extension()));
    }

    @Transactional(readOnly = true)
    public ExportedReport exportarConNotas(ReportePreviewRequest request, String formato, List<String> headerNotes, String auditOperation, String auditMessage) {
        ReportePreviewResponse response = ejecutar(request, 0, MAX_EXPORT_ROWS, headerNotes);
        if (response.totalRegistros() > MAX_EXPORT_ROWS) {
            throw new IllegalArgumentException("El reporte supera " + MAX_EXPORT_ROWS + " registros. Aplique filtros más específicos antes de exportar.");
        }
        ReporteExporter exporter = exporterRegistry.get(formato);
        byte[] bytes = exporter.exportar(response);
        auditoriaService.registrar(SecurityUtils.requireCurrentInstitutionId(), SecurityUtils.currentUserId(),
                "REPORTES", auditOperation + "_" + exporter.formato(), "reporte", request.codigoReporte(), true, auditMessage);
        return new ExportedReport(bytes, exporter.contentType(), fileName(response.encabezado().nombreReporte(), exporter.extension()));
    }

    private ReportePreviewResponse ejecutar(ReportePreviewRequest request, int page, int size, List<String> headerNotes) {
        Usuario user = SecurityUtils.currentUser();
        UUID idInstitucion = SecurityUtils.requireCurrentInstitutionId();
        Institucion institucion = institucionRepository.findById(idInstitucion)
                .orElseThrow(() -> new EntityNotFoundException("Institución no encontrada: " + idInstitucion));
        ReporteExecutionContext context = new ReporteExecutionContext(
                idInstitucion,
                institucion.getNombre(),
                SecurityUtils.currentUserId(),
                user == null ? "Usuario" : (user.getNombres() + " " + user.getApellidos()).trim(),
                request.filtros() == null ? Map.of() : request.filtros(),
                headerNotes,
                request.presentacion(),
                page,
                size
        );
        return registry.get(request.codigoReporte()).ejecutar(context);
    }

    private int sanitizePage(Integer page) { return Math.max(page == null ? 0 : page, 0); }
    private int sanitizeSize(Integer size) { int value = size == null ? 25 : size; return Math.max(1, Math.min(value, MAX_PREVIEW_SIZE)); }

    private String fileName(String reportName, String extension) {
        String normalized = Normalizer.normalize(reportName.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return normalized + "." + extension;
    }

    public record ExportedReport(byte[] bytes, String contentType, String fileName) {}

    public ExportedReport exportarDatos(List<Map<String, Object>> data, List<String> columnas,
                                         List<String> headers, String nombreBase, String formato) {
        var encabezado = new com.uagrm.si2g2.reporte.dto.ReporteHeaderResponse(
                nombreBase.replace("-", " "), null, null, null);
        var cols = java.util.stream.IntStream.range(0, columnas.size())
                .mapToObj(i -> new com.uagrm.si2g2.reporte.dto.ReporteColumnResponse(
                        columnas.get(i), headers.size() > i ? headers.get(i) : columnas.get(i), null))
                .toList();
        var respuesta = new com.uagrm.si2g2.reporte.dto.ReportePreviewResponse(
                encabezado, cols, data, null, data.size(), 0, data.size());
        return exportar(respuesta, nombreBase, formato);
    }

    public ExportedReport exportar(ReportePreviewResponse reporte, String nombreBase, String formato) {
        var exporter = exporterRegistry.get(formato);
        var bytes = exporter.exportar(reporte);
        return new ExportedReport(bytes, exporter.contentType(), nombreBase + "." + exporter.extension());
    }

    public List<Map<String, Object>> reporteAsistencia(UUID idInstitucion, UUID idGestion, UUID idParalelo) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    e.codigo_estudiante,
                    e.nombres AS nombre,
                    e.apellidos AS apellido,
                    m.nombre AS materia,
                    p.nombre AS paralelo,
                    COUNT(ar.id) AS total_registros,
                    SUM(CASE WHEN adt.estado_asistencia = 'PRESENTE' THEN 1 ELSE 0 END) AS presentes,
                    SUM(CASE WHEN adt.estado_asistencia = 'TARDANZA' THEN 1 ELSE 0 END) AS tardanzas,
                    SUM(CASE WHEN adt.estado_asistencia = 'AUSENTE' THEN 1 ELSE 0 END) AS ausentes,
                    ROUND(
                        (SUM(CASE WHEN adt.estado_asistencia IN ('PRESENTE','TARDANZA') THEN 1 ELSE 0 END)::NUMERIC
                         / NULLIF(COUNT(ar.id), 0)) * 100, 2
                    ) AS porcentaje_asistencia
                FROM sia.asistencia_registro ar
                JOIN sia.asistencia_detalle adt ON adt.id_asistencia_registro = ar.id
                JOIN sia.inscripcion i ON i.id = adt.id_inscripcion
                JOIN sia.estudiante e ON e.id = i.id_estudiante
                JOIN sia.asignacion_docente ad ON ad.id = ar.id_asignacion_docente
                JOIN sia.paralelo p ON p.id = ad.id_paralelo
                JOIN sia.materia m ON m.id = ad.id_materia
                WHERE ar.id_institucion = ?
                  AND ad.id_gestion_academica = ?
                """);

        List<Object> params = new ArrayList<>(Arrays.asList(idInstitucion, idGestion));
        if (idParalelo != null) {
            sql.append(" AND ad.id_paralelo = ? ");
            params.add(idParalelo);
        }
        sql.append("""
                GROUP BY e.codigo_estudiante, e.nombres, e.apellidos, m.nombre, p.nombre
                ORDER BY e.apellidos, e.nombres, m.nombre
                """);
        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    public List<Map<String, Object>> reporteCalificaciones(UUID idInstitucion, UUID idGestion, UUID idParalelo) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    e.codigo_estudiante,
                    e.nombres AS nombre,
                    e.apellidos AS apellido,
                    m.nombre AS materia,
                    p.nombre AS paralelo,
                    ev.tipo AS tipo_evaluacion,
                    ev.nombre AS evaluacion,
                    100 AS nota_maxima,
                    cal.nota_numerica AS nota_obtenida,
                    CASE WHEN cal.nota_numerica >= 51 THEN 'Aprobado' ELSE 'Reprobado' END AS resultado
                FROM sia.calificacion cal
                JOIN sia.evaluacion ev ON ev.id = cal.id_evaluacion
                JOIN sia.inscripcion i ON i.id = cal.id_inscripcion
                JOIN sia.estudiante e ON e.id = i.id_estudiante
                JOIN sia.asignacion_docente ad ON ad.id = ev.id_asignacion_docente
                JOIN sia.paralelo p ON p.id = ad.id_paralelo
                JOIN sia.materia m ON m.id = ad.id_materia
                WHERE cal.id_institucion = ?
                  AND ad.id_gestion_academica = ?
                """);

        List<Object> params = new ArrayList<>(Arrays.asList(idInstitucion, idGestion));
        if (idParalelo != null) {
            sql.append(" AND ad.id_paralelo = ? ");
            params.add(idParalelo);
        }
        sql.append(" ORDER BY e.apellidos, e.nombres, m.nombre, ev.tipo, ev.nombre ");
        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    public List<Map<String, Object>> reporteInscripciones(UUID idInstitucion, UUID idGestion, UUID idCurso) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    e.codigo_estudiante,
                    e.nombres AS nombre,
                    e.apellidos AS apellido,
                    c.nombre AS curso,
                    p.nombre AS paralelo,
                    g.nombre AS gestion,
                    i.estado AS estado_inscripcion,
                    i.creado_en AS fecha_inscripcion
                FROM sia.inscripcion i
                JOIN sia.estudiante e ON e.id = i.id_estudiante
                JOIN sia.paralelo p ON p.id = i.id_paralelo
                JOIN sia.curso c ON c.id = p.id_curso
                JOIN sia.gestion_academica g ON g.id = i.id_gestion_academica
                WHERE i.id_institucion = ?
                  AND i.id_gestion_academica = ?
                """);

        List<Object> params = new ArrayList<>(Arrays.asList(idInstitucion, idGestion));
        if (idCurso != null) {
            sql.append(" AND c.id = ? ");
            params.add(idCurso);
        }
        sql.append(" ORDER BY c.nombre, p.nombre, e.apellidos, e.nombres ");
        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    public List<Map<String, Object>> rendimientoEstudiante(UUID idInstitucion, UUID idEstudiante, UUID idGestion) {
        return jdbc.queryForList("""
            SELECT
                m.nombre AS materia,
                pe.numero_periodo AS periodo,
                ROUND(AVG(ca.nota_obtenida)::numeric, 2) AS promedio_actividades,
                COALESCE(cs.nota_ser, 0) AS nota_ser,
                COALESCE(ae.nota_autoevaluacion, 0) AS autoevaluacion,
                ROUND(
                    (AVG(ca.nota_obtenida) * 0.85 + COALESCE(cs.nota_ser, 0) + COALESCE(ae.nota_autoevaluacion, 0))::numeric, 2
                ) AS nota_consolidada,
                CASE WHEN ROUND(
                    (AVG(ca.nota_obtenida) * 0.85 + COALESCE(cs.nota_ser, 0) + COALESCE(ae.nota_autoevaluacion, 0))::numeric, 2
                ) >= 51 THEN 'APROBADO' ELSE 'EN_RIESGO' END AS estado
            FROM sia.calificacion_actividad ca
            JOIN sia.actividad_evaluativa ae_act ON ae_act.id = ca.id_actividad
            JOIN sia.materia m ON m.id = ae_act.id_materia
            JOIN sia.periodo_evaluacion pe ON pe.id = ae_act.id_periodo_evaluacion
            LEFT JOIN sia.calificacion_ser cs ON cs.id_estudiante = ca.id_estudiante AND cs.id_materia = m.id AND cs.id_periodo_evaluacion = pe.id
            LEFT JOIN sia.autoevaluacion_trimestral ae ON ae.id_estudiante = ca.id_estudiante AND ae.id_materia = m.id AND ae.id_periodo_evaluacion = pe.id
            WHERE ca.id_institucion = ? AND ca.id_estudiante = ?
            GROUP BY m.nombre, pe.numero_periodo, cs.nota_ser, ae.nota_autoevaluacion
            ORDER BY m.nombre, pe.numero_periodo
            """, idInstitucion, idEstudiante);
    }

    public List<Map<String, Object>> rankingParalelo(UUID idInstitucion, UUID idParalelo, UUID idGestion) {
        return jdbc.queryForList("""
            SELECT
                e.codigo_estudiante,
                e.nombres || ' ' || e.apellidos AS estudiante,
                ROUND(AVG(ca.nota_obtenida)::numeric, 2) AS promedio,
                RANK() OVER (ORDER BY AVG(ca.nota_obtenida) DESC) AS ranking
            FROM sia.calificacion_actividad ca
            JOIN sia.actividad_evaluativa ae_act ON ae_act.id = ca.id_actividad
            JOIN sia.estudiante e ON e.id = ca.id_estudiante
            JOIN sia.inscripcion i ON i.id_estudiante = e.id AND i.id_institucion = ca.id_institucion
            WHERE ca.id_institucion = ? AND i.id_paralelo = ? AND i.id_gestion_academica = ?
            GROUP BY e.codigo_estudiante, e.nombres, e.apellidos
            ORDER BY ranking
            """, idInstitucion, idParalelo, idGestion);
    }

    public List<Map<String, Object>> riesgoAcademico(UUID idInstitucion, UUID idGestion,
                                                       Double umbralAsistencia, Double umbralPromedio) {
        double asisUmbral = umbralAsistencia != null ? umbralAsistencia : 60.0;
        double promUmbral = umbralPromedio != null ? umbralPromedio : 51.0;
        return jdbc.queryForList("""
            SELECT
                e.codigo_estudiante,
                e.nombres || ' ' || e.apellidos AS estudiante,
                c.nombre AS curso,
                p.nombre AS paralelo,
                ROUND(AVG(ca.nota_obtenida)::numeric, 2) AS promedio_calificaciones,
                CASE
                    WHEN AVG(ca.nota_obtenida) < ? THEN 'CRITICO'
                    WHEN AVG(ca.nota_obtenida) < ? THEN 'ALTO'
                    WHEN AVG(ca.nota_obtenida) < ? THEN 'MEDIO'
                    ELSE 'BAJO'
                END AS nivel_riesgo
            FROM sia.calificacion_actividad ca
            JOIN sia.actividad_evaluativa ae_act ON ae_act.id = ca.id_actividad
            JOIN sia.periodo_evaluacion pe ON pe.id = ae_act.id_periodo_evaluacion
            JOIN sia.estudiante e ON e.id = ca.id_estudiante
            JOIN sia.inscripcion i ON i.id_estudiante = e.id AND i.id_institucion = ca.id_institucion
            JOIN sia.paralelo p ON p.id = i.id_paralelo
            JOIN sia.curso c ON c.id = p.id_curso
            WHERE ca.id_institucion = ? AND pe.id_gestion_academica = ?
            GROUP BY e.codigo_estudiante, e.nombres, e.apellidos, c.nombre, p.nombre
            HAVING AVG(ca.nota_obtenida) < ?
            ORDER BY AVG(ca.nota_obtenida)
            """, promUmbral * 0.5, promUmbral * 0.7, promUmbral, idInstitucion, idGestion, promUmbral);
    }

    public Map<String, Object> reporteGerencial(UUID idInstitucion, UUID idGestion) {
        Map<String, Object> resumen = new LinkedHashMap<>();
        Long totalEstudiantes = jdbc.queryForObject(
                "SELECT COUNT(DISTINCT id_estudiante) FROM sia.inscripcion WHERE id_institucion = ? AND id_gestion_academica = ?",
                Long.class, idInstitucion, idGestion);
        resumen.put("totalEstudiantes", totalEstudiantes);

        Long totalDocentes = jdbc.queryForObject(
                "SELECT COUNT(DISTINCT id_docente) FROM sia.asignacion_docente WHERE id_institucion = ? AND id_gestion_academica = ?",
                Long.class, idInstitucion, idGestion);
        resumen.put("totalDocentes", totalDocentes);

        Long totalSesiones = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sia.asistencia_registro ar JOIN sia.asignacion_docente ad ON ad.id = ar.id_asignacion_docente WHERE ar.id_institucion = ? AND ad.id_gestion_academica = ?",
                Long.class, idInstitucion, idGestion);
        resumen.put("totalSesiones", totalSesiones);

        Double promedioAsistencia = jdbc.queryForObject(
                """
                SELECT ROUND(AVG(pct)::NUMERIC, 2) FROM (
                    SELECT
                        SUM(CASE WHEN adt.estado_asistencia IN ('PRESENTE','TARDANZA') THEN 1 ELSE 0 END)::FLOAT
                        / NULLIF(COUNT(adt.id),0) * 100 AS pct
                    FROM sia.asistencia_detalle adt
                    JOIN sia.asistencia_registro ar ON ar.id = adt.id_asistencia_registro
                    JOIN sia.asignacion_docente ad ON ad.id = ar.id_asignacion_docente
                    WHERE ar.id_institucion = ? AND ad.id_gestion_academica = ?
                    GROUP BY adt.id_inscripcion
                ) sub
                """,
                Double.class, idInstitucion, idGestion);
        resumen.put("promedioAsistencia", promedioAsistencia);

        return resumen;
    }
}
