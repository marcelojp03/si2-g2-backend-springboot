package com.uagrm.si2g2.reporte.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Servicio de reportes configurables para HU-SE-11 (asistencia),
 * HU-SE-12 (calificaciones) y HU-SE-13 (inscripciones).
 * Usa JDBC directo para obtener datos del tenant activo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteService {

    private final JdbcTemplate jdbc;

    // -------------------------------------------------------------------------
    // HU-SE-11: Reporte de Asistencia
    // -------------------------------------------------------------------------

    /**
     * Resumen de asistencia por paralelo/gestión para la institución del tenant activo.
     *
     * @param idGestion  UUID de la gestión (obligatorio)
     * @param idParalelo UUID del paralelo (opcional)
     */
    public List<Map<String, Object>> reporteAsistencia(UUID idInstitucion, UUID idGestion, UUID idParalelo) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    e.codigo_estudiante,
                    e.nombre,
                    e.apellido,
                    m.nombre                        AS materia,
                    p.nombre                        AS paralelo,
                    COUNT(ra.id)                    AS total_registros,
                    SUM(CASE WHEN ra.estado = 'PRESENTE' THEN 1 ELSE 0 END) AS presentes,
                    SUM(CASE WHEN ra.estado = 'TARDANZA' THEN 1 ELSE 0 END) AS tardanzas,
                    SUM(CASE WHEN ra.estado = 'AUSENTE'  THEN 1 ELSE 0 END) AS ausentes,
                    ROUND(
                        (SUM(CASE WHEN ra.estado IN ('PRESENTE','TARDANZA') THEN 1 ELSE 0 END)::NUMERIC
                         / NULLIF(COUNT(ra.id), 0)) * 100, 2
                    ) AS porcentaje_asistencia
                FROM sia.registro_asistencia ra
                JOIN sia.sesion_asistencia sa ON sa.id = ra.id_sesion_asistencia
                JOIN sia.inscripcion i ON i.id = ra.id_inscripcion
                JOIN sia.estudiante e ON e.id = i.id_estudiante
                JOIN sia.asignacion_docente ad ON ad.id = sa.id_asignacion_docente
                JOIN sia.paralelo p ON p.id = ad.id_paralelo
                JOIN sia.materia m ON m.id = ad.id_materia
                WHERE sa.id_institucion = ?
                  AND ad.id_gestion_academica = ?
                """);

        List<Object> params = new ArrayList<>(Arrays.asList(idInstitucion, idGestion));

        if (idParalelo != null) {
            sql.append(" AND ad.id_paralelo = ? ");
            params.add(idParalelo);
        }

        sql.append("""
                GROUP BY e.codigo_estudiante, e.nombre, e.apellido, m.nombre, p.nombre
                ORDER BY e.apellido, e.nombre, m.nombre
                """);

        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    // -------------------------------------------------------------------------
    // HU-SE-12: Reporte de Calificaciones
    // -------------------------------------------------------------------------

    public List<Map<String, Object>> reporteCalificaciones(UUID idInstitucion, UUID idGestion, UUID idParalelo) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    e.codigo_estudiante,
                    e.nombre,
                    e.apellido,
                    m.nombre                           AS materia,
                    p.nombre                           AS paralelo,
                    te.nombre                          AS tipo_evaluacion,
                    ev.nombre                          AS evaluacion,
                    ev.nota_maxima,
                    rc.nota                            AS nota_obtenida,
                    CASE WHEN rc.nota >= 51 THEN 'Aprobado' ELSE 'Reprobado' END AS resultado
                FROM sia.registro_calificacion rc
                JOIN sia.evaluacion ev ON ev.id = rc.id_evaluacion
                JOIN sia.tipo_evaluacion te ON te.id = ev.id_tipo_evaluacion
                JOIN sia.inscripcion i ON i.id = rc.id_inscripcion
                JOIN sia.estudiante e ON e.id = i.id_estudiante
                JOIN sia.asignacion_docente ad ON ad.id = ev.id_asignacion_docente
                JOIN sia.paralelo p ON p.id = ad.id_paralelo
                JOIN sia.materia m ON m.id = ad.id_materia
                WHERE rc.id_institucion = ?
                  AND ad.id_gestion_academica = ?
                """);

        List<Object> params = new ArrayList<>(Arrays.asList(idInstitucion, idGestion));

        if (idParalelo != null) {
            sql.append(" AND ad.id_paralelo = ? ");
            params.add(idParalelo);
        }

        sql.append(" ORDER BY e.apellido, e.nombre, m.nombre, te.nombre, ev.nombre ");

        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    // -------------------------------------------------------------------------
    // HU-SE-13: Reporte de Inscripciones
    // -------------------------------------------------------------------------

    public List<Map<String, Object>> reporteInscripciones(UUID idInstitucion, UUID idGestion, UUID idCurso) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    e.codigo_estudiante,
                    e.nombre,
                    e.apellido,
                    c.nombre                     AS curso,
                    p.nombre                     AS paralelo,
                    g.nombre                     AS gestion,
                    i.estado                     AS estado_inscripcion,
                    i.creado_en                  AS fecha_inscripcion
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

        sql.append(" ORDER BY c.nombre, p.nombre, e.apellido, e.nombre ");

        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    // -------------------------------------------------------------------------
    // Resumen gerencial por gestión
    // -------------------------------------------------------------------------

    public Map<String, Object> reporteGerencial(UUID idInstitucion, UUID idGestion) {
        Map<String, Object> resumen = new LinkedHashMap<>();

        // Total estudiantes inscritos
        Long totalEstudiantes = jdbc.queryForObject(
                "SELECT COUNT(DISTINCT id_estudiante) FROM sia.inscripcion WHERE id_institucion = ? AND id_gestion_academica = ?",
                Long.class, idInstitucion, idGestion);
        resumen.put("totalEstudiantes", totalEstudiantes);

        // Total docentes asignados
        Long totalDocentes = jdbc.queryForObject(
                "SELECT COUNT(DISTINCT id_docente) FROM sia.asignacion_docente WHERE id_institucion = ? AND id_gestion_academica = ?",
                Long.class, idInstitucion, idGestion);
        resumen.put("totalDocentes", totalDocentes);

        // Total sesiones de asistencia
        Long totalSesiones = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sia.sesion_asistencia WHERE id_institucion = ? AND id_gestion_academica = ?",
                Long.class, idInstitucion, idGestion);
        resumen.put("totalSesiones", totalSesiones);

        // Promedio asistencia
        Double promedioAsistencia = jdbc.queryForObject(
                """
                SELECT ROUND(AVG(pct)::NUMERIC, 2) FROM (
                    SELECT
                        SUM(CASE WHEN ra.estado IN ('PRESENTE','TARDANZA') THEN 1 ELSE 0 END)::FLOAT
                        / NULLIF(COUNT(ra.id),0) * 100 AS pct
                    FROM sia.registro_asistencia ra
                    JOIN sia.sesion_asistencia sa ON sa.id = ra.id_sesion_asistencia
                    WHERE sa.id_institucion = ? AND sa.id_gestion_academica = ?
                    GROUP BY ra.id_inscripcion
                ) sub
                """,
                Double.class, idInstitucion, idGestion);
        resumen.put("promedioAsistencia", promedioAsistencia);

        return resumen;
    }
}
