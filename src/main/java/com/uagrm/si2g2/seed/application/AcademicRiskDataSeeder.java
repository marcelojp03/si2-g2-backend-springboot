package com.uagrm.si2g2.seed.application;

import com.uagrm.si2g2.academico.domain.GestionAcademica;
import com.uagrm.si2g2.academico.domain.GestionAcademicaRepository;
import com.uagrm.si2g2.institucion.domain.Institucion;
import com.uagrm.si2g2.institucion.domain.InstitucionRepository;
import com.uagrm.si2g2.seed.dto.AcademicRiskSeedResult;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AcademicRiskDataSeeder {
    private static final String EVALUATION_PREFIX = "RIESGO-SEED-";

    private final InstitucionRepository institucionRepository;
    private final GestionAcademicaRepository gestionRepository;
    private final NamedParameterJdbcTemplate jdbc;

    @Transactional
    public AcademicRiskSeedResult seed(String institutionCode) {
        String code = institutionCode == null ? "" : institutionCode.trim().toUpperCase();
        Institucion institution = institucionRepository.findByCodigo(code)
                .orElseThrow(() -> new EntityNotFoundException("Institucion no encontrada: " + code));
        GestionAcademica management = gestionRepository.findByIdInstitucionAndActivaTrue(institution.getId())
                .orElseThrow(() -> new EntityNotFoundException("La institucion no tiene una gestion activa"));

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("institutionId", institution.getId())
                .addValue("managementId", management.getId())
                .addValue("evaluationPrefix", EVALUATION_PREFIX + "%");

        seedPeriods(management, params);
        seedEvaluations(params);
        seedGrades(params);
        seedDimensionGrades(params);
        seedAttendanceRegisters(management, params);
        seedAttendanceDetails(params);

        Map<String, Integer> profiles = expectedProfiles(params);
        return new AcademicRiskSeedResult(code,
                count("periodo_evaluacion", "id_institucion = :institutionId AND id_gestion_academica = :managementId", params),
                count("evaluacion", "id_institucion = :institutionId AND nombre LIKE :evaluationPrefix", params),
                count("calificacion", "id_institucion = :institutionId AND id_evaluacion IN "
                        + "(SELECT id FROM sia.evaluacion WHERE id_institucion = :institutionId AND nombre LIKE :evaluationPrefix)", params),
                count("asistencia_registro", "id_institucion = :institutionId AND fecha >= :attendanceStart "
                        + "AND fecha < :attendanceEnd", params),
                count("asistencia_detalle", "id_institucion = :institutionId AND id_asistencia_registro IN "
                        + "(SELECT id FROM sia.asistencia_registro WHERE id_institucion = :institutionId "
                        + "AND fecha >= :attendanceStart AND fecha < :attendanceEnd)", params),
                profiles);
    }

    private void seedPeriods(GestionAcademica management, MapSqlParameterSource params) {
        LocalDate start = management.getFechaInicio();
        long totalDays = ChronoUnit.DAYS.between(start, management.getFechaFin()) + 1;
        long periodDays = totalDays / 3;
        for (int number = 1; number <= 3; number++) {
            LocalDate periodStart = start.plusDays((number - 1L) * periodDays);
            LocalDate periodEnd = number == 3
                    ? management.getFechaFin()
                    : start.plusDays(number * periodDays - 1);
            MapSqlParameterSource periodParams = copy(params)
                    .addValue("number", number)
                    .addValue("periodStart", periodStart)
                    .addValue("periodEnd", periodEnd);
            jdbc.update("""
                    INSERT INTO sia.periodo_evaluacion
                        (id, id_institucion, id_gestion_academica, numero_periodo, tipo_periodo,
                         fecha_inicio, fecha_fin, estado, peso_ser, peso_saber, peso_hacer, peso_auto,
                         creado_en, actualizado_en)
                    VALUES (gen_random_uuid(), :institutionId, :managementId, :number, 'TRIMESTRAL',
                            :periodStart, :periodEnd, 'ABIERTO', 10, 45, 40, 5, NOW(), NOW())
                    ON CONFLICT (id_institucion, id_gestion_academica, numero_periodo) DO NOTHING
                    """, periodParams);
        }
        params.addValue("attendanceStart", start.plusWeeks(2));
        params.addValue("attendanceEnd", start.plusWeeks(12));
    }

    private void seedEvaluations(MapSqlParameterSource params) {
        jdbc.update("""
                WITH assignments AS (
                    SELECT a.id, a.id_materia
                    FROM sia.asignacion_docente a
                    WHERE a.id_institucion = :institutionId
                      AND a.id_gestion_academica = :managementId
                      AND a.estado = 'ACTIVA'
                )
                INSERT INTO sia.evaluacion
                    (id, id_institucion, id_asignacion_docente, id_materia, periodo, tipo, nombre, ponderacion,
                     escala, estado, creado_en, actualizado_en)
                SELECT gen_random_uuid(), :institutionId, a.id, a.id_materia, p.numero_periodo,
                       seed.tipo,
                       'RIESGO-SEED-P' || p.numero_periodo || '-E' || seed.numero,
                       50, 'NUMERICA', 'ABIERTA',
                       p.fecha_inicio::timestamp + (seed.numero * INTERVAL '1 day'),
                       NOW()
                FROM assignments a
                JOIN sia.periodo_evaluacion p
                  ON p.id_institucion = :institutionId
                 AND p.id_gestion_academica = :managementId
                CROSS JOIN (VALUES (1, 'PARCIAL'), (2, 'TRABAJO_PRACTICO')) AS seed(numero, tipo)
                ON CONFLICT (id_asignacion_docente, periodo, nombre) DO NOTHING
                """, params);
    }

    private void seedGrades(MapSqlParameterSource params) {
        jdbc.update("""
                WITH removed_cross_parallel_grades AS (
                    DELETE FROM sia.calificacion c
                    USING sia.evaluacion ev, sia.asignacion_docente ad, sia.inscripcion i
                    WHERE c.id_evaluacion = ev.id
                      AND ev.id_asignacion_docente = ad.id
                      AND c.id_inscripcion = i.id
                      AND ev.id_institucion = :institutionId
                      AND ev.nombre LIKE :evaluationPrefix
                      AND i.id_paralelo <> ad.id_paralelo
                    RETURNING c.id
                ), enrolled AS (
                    SELECT i.id AS enrollment_id, i.id_paralelo, i.id_estudiante,
                           MOD((ROW_NUMBER() OVER (ORDER BY e.codigo_estudiante) - 1)::integer, 20) AS profile
                    FROM sia.inscripcion i
                    JOIN sia.estudiante e ON e.id = i.id_estudiante AND e.id_institucion = i.id_institucion
                    WHERE i.id_institucion = :institutionId
                      AND i.id_gestion_academica = :managementId
                      AND i.estado = 'ACTIVA'
                ), grade_data AS (
                    SELECT en.enrollment_id, en.profile, ev.id AS evaluation_id,
                           LEAST(100, GREATEST(0,
                           CASE
                               WHEN en.profile BETWEEN 1 AND 10 THEN
                                    CASE WHEN ev.periodo = 1 AND ev.nombre LIKE '%-E1' THEN 80
                                         WHEN ev.periodo = 1 THEN 85
                                         WHEN ev.periodo = 2 AND ev.nombre LIKE '%-E1' THEN 90
                                         WHEN ev.periodo = 2 THEN 95
                                         WHEN ev.nombre LIKE '%-E1' THEN 92 ELSE 96 END
                               WHEN en.profile BETWEEN 11 AND 14 THEN
                                    CASE WHEN ev.periodo = 1 AND ev.nombre LIKE '%-E1' THEN 55
                                         WHEN ev.periodo = 1 THEN 50
                                         WHEN ev.periodo = 2 AND ev.nombre LIKE '%-E1' THEN 42
                                         WHEN ev.periodo = 2 THEN 35
                                         WHEN ev.nombre LIKE '%-E1' THEN 38 ELSE 32 END
                               WHEN en.profile BETWEEN 15 AND 17 THEN
                                    CASE WHEN ev.periodo = 1 THEN 40
                                         WHEN ev.periodo = 2 AND ev.nombre LIKE '%-E1' THEN 25
                                         WHEN ev.periodo = 2 THEN 20
                                         WHEN ev.nombre LIKE '%-E1' THEN 22 ELSE 18 END
                               ELSE
                                    CASE WHEN ev.periodo = 1 THEN 25
                                         WHEN ev.periodo = 2 AND ev.nombre LIKE '%-E1' THEN 10
                                         WHEN ev.periodo = 2 THEN 5
                                         WHEN ev.nombre LIKE '%-E1' THEN 8 ELSE 4 END
                           END
                           + (MOD(MOD(hashtextextended(
                                 en.id_estudiante::text || ':' || ev.id::text, 0), 1301) + 1301, 1301)::numeric / 100 - 6.5)
                           ))::numeric(8,2) AS provisional_grade
                    FROM enrolled en
                    JOIN sia.asignacion_docente ad
                      ON ad.id_paralelo = en.id_paralelo
                     AND ad.id_institucion = :institutionId
                     AND ad.id_gestion_academica = :managementId
                     AND ad.estado = 'ACTIVA'
                    JOIN sia.evaluacion ev
                      ON ev.id_institucion = :institutionId
                      AND ev.id_asignacion_docente = ad.id
                      AND ev.nombre LIKE :evaluationPrefix
                      AND ev.periodo IN (1, 2, 3)
                ), ranked_grades AS (
                    SELECT grade_data.*,
                           ROW_NUMBER() OVER (
                               PARTITION BY evaluation_id
                               ORDER BY provisional_grade, enrollment_id) AS position,
                           COUNT(*) OVER (PARTITION BY evaluation_id) AS total
                    FROM grade_data
                )
                INSERT INTO sia.calificacion
                    (id, id_institucion, id_evaluacion, id_inscripcion, nota_numerica,
                     creado_en, actualizado_en)
                SELECT gen_random_uuid(), :institutionId, evaluation_id, enrollment_id,
                       ROUND(LEAST(100, GREATEST(0,
                           provisional_grade * 0.85
                           + CASE WHEN total = 1 THEN provisional_grade * 0.15
                                  ELSE ((position - 1)::numeric * 100 / (total - 1)) * 0.15
                             END
                       )), 2),
                       NOW(), NOW()
                FROM ranked_grades
                ON CONFLICT (id_evaluacion, id_inscripcion) DO UPDATE
                    SET nota_numerica = EXCLUDED.nota_numerica,
                        nota_literal = NULL,
                        actualizado_en = NOW()
                """, params);
    }

    private void seedAttendanceRegisters(GestionAcademica management, MapSqlParameterSource params) {
        params.addValue("attendanceStart", management.getFechaInicio().plusWeeks(2));
        params.addValue("attendanceEnd", management.getFechaInicio().plusWeeks(12));
        jdbc.update("""
                WITH active_assignments AS (
                    SELECT id
                    FROM sia.asignacion_docente
                    WHERE id_institucion = :institutionId
                      AND id_gestion_academica = :managementId
                      AND estado = 'ACTIVA'
                )
                INSERT INTO sia.asistencia_registro
                    (id, id_institucion, id_asignacion_docente, fecha, estado, creado_en, actualizado_en)
                SELECT gen_random_uuid(), :institutionId, a.id,
                       CAST(:attendanceStart AS date) + (week.number * 7), 'REGISTRADA', NOW(), NOW()
                FROM active_assignments a
                CROSS JOIN generate_series(0, 9) AS week(number)
                ON CONFLICT (id_asignacion_docente, fecha) DO NOTHING
                """, params);
    }

    private void seedDimensionGrades(MapSqlParameterSource params) {
        jdbc.update("""
                INSERT INTO sia.dimension
                    (id, id_institucion, nombre, descripcion, peso_default, estado, es_global,
                     creado_en, actualizado_en)
                SELECT gen_random_uuid(), NULL, seed.nombre, seed.descripcion, seed.peso,
                       'ACTIVO', TRUE, NOW(), NOW()
                FROM (VALUES
                    ('SER', 'Valores, comportamiento y actitudes', 10),
                    ('SABER', 'Conocimientos teoricos y conceptuales', 45),
                    ('HACER', 'Habilidades practicas y aplicacion', 40),
                    ('AUTOEVALUACION', 'Reflexion del estudiante sobre su aprendizaje', 5)
                ) AS seed(nombre, descripcion, peso)
                WHERE NOT EXISTS (
                    SELECT 1 FROM sia.dimension d
                    WHERE d.id_institucion IS NULL AND d.nombre = seed.nombre
                )
                """, params);

        jdbc.update("""
                INSERT INTO sia.periodo_dimension
                    (id, id_periodo_evaluacion, id_dimension, ponderacion, creado_en, actualizado_en)
                SELECT gen_random_uuid(), p.id, d.id, d.peso_default, NOW(), NOW()
                FROM sia.periodo_evaluacion p
                CROSS JOIN sia.dimension d
                WHERE p.id_institucion = :institutionId
                  AND p.id_gestion_academica = :managementId
                  AND d.id_institucion IS NULL
                  AND d.nombre IN ('SER', 'SABER', 'HACER', 'AUTOEVALUACION')
                ON CONFLICT (id_periodo_evaluacion, id_dimension) DO UPDATE
                    SET ponderacion = EXCLUDED.ponderacion, actualizado_en = NOW()
                """, params);

        jdbc.update("""
                INSERT INTO sia.actividad_evaluativa
                    (id, id_institucion, id_periodo_evaluacion, id_materia, id_docente,
                     nombre_actividad, dimension, fecha_actividad, descripcion_evidencia,
                     puntaje_maximo, estado, publicado_en, creado_en, actualizado_en)
                SELECT ev.id, ev.id_institucion, p.id, ev.id_materia, ad.id_docente,
                       ev.nombre || '-' || LEFT(ad.id::text, 8),
                       CASE WHEN ev.tipo = 'TRABAJO_PRACTICO' THEN 'HACER' ELSE 'SABER' END,
                       ev.creado_en::date, 'Migrada desde evaluacion academica',
                       100, 'PUBLICADA', NOW(), ev.creado_en, NOW()
                FROM sia.evaluacion ev
                JOIN sia.asignacion_docente ad ON ad.id = ev.id_asignacion_docente
                JOIN sia.periodo_evaluacion p
                  ON p.id_institucion = ev.id_institucion
                 AND p.id_gestion_academica = ad.id_gestion_academica
                 AND p.numero_periodo = ev.periodo
                WHERE ev.id_institucion = :institutionId
                  AND ev.nombre LIKE :evaluationPrefix
                ON CONFLICT (id) DO UPDATE
                    SET id_periodo_evaluacion = EXCLUDED.id_periodo_evaluacion,
                        id_materia = EXCLUDED.id_materia,
                        id_docente = EXCLUDED.id_docente,
                        nombre_actividad = EXCLUDED.nombre_actividad,
                        dimension = EXCLUDED.dimension,
                        estado = EXCLUDED.estado,
                        actualizado_en = NOW()
                """, params);

        jdbc.update("""
                WITH removed_cross_parallel_grades AS (
                    DELETE FROM sia.calificacion_actividad ca
                    USING sia.actividad_evaluativa ae, sia.evaluacion ev, sia.asignacion_docente ad
                    WHERE ca.id_actividad = ae.id
                      AND ae.id = ev.id
                      AND ev.id_asignacion_docente = ad.id
                      AND ev.id_institucion = :institutionId
                      AND ev.nombre LIKE :evaluationPrefix
                      AND NOT EXISTS (
                          SELECT 1
                          FROM sia.inscripcion i
                          WHERE i.id_institucion = ca.id_institucion
                            AND i.id_estudiante = ca.id_estudiante
                            AND i.id_paralelo = ad.id_paralelo
                            AND i.id_gestion_academica = :managementId
                            AND i.estado = 'ACTIVA'
                      )
                    RETURNING ca.id
                )
                INSERT INTO sia.calificacion_actividad
                    (id, id_institucion, id_actividad, id_estudiante, nota_obtenida,
                     estado, creado_en, actualizado_en)
                SELECT gen_random_uuid(), c.id_institucion, c.id_evaluacion, i.id_estudiante,
                       c.nota_numerica, 'REGISTRADA', c.creado_en, NOW()
                FROM sia.calificacion c
                JOIN sia.evaluacion ev ON ev.id = c.id_evaluacion
                JOIN sia.inscripcion i ON i.id = c.id_inscripcion
                WHERE ev.id_institucion = :institutionId
                  AND ev.nombre LIKE :evaluationPrefix
                  AND c.nota_numerica IS NOT NULL
                ON CONFLICT (id_actividad, id_estudiante) DO UPDATE
                    SET nota_obtenida = EXCLUDED.nota_obtenida,
                        estado = 'REGISTRADA', actualizado_en = NOW()
                """, params);

        jdbc.update("""
                WITH averages AS (
                    SELECT c.id_institucion, p.id AS period_id, i.id_estudiante, ev.id_materia,
                           ROUND(AVG(c.nota_numerica) / 10, 2) AS note
                    FROM sia.calificacion c
                    JOIN sia.evaluacion ev ON ev.id = c.id_evaluacion
                    JOIN sia.inscripcion i ON i.id = c.id_inscripcion
                    JOIN sia.periodo_evaluacion p
                      ON p.id_institucion = ev.id_institucion
                     AND p.id_gestion_academica = i.id_gestion_academica
                     AND p.numero_periodo = ev.periodo
                    WHERE ev.id_institucion = :institutionId
                      AND ev.nombre LIKE :evaluationPrefix
                      AND c.nota_numerica IS NOT NULL
                    GROUP BY c.id_institucion, p.id, i.id_estudiante, ev.id_materia
                )
                INSERT INTO sia.calificacion_ser
                    (id, id_institucion, id_periodo_evaluacion, id_estudiante, id_materia,
                     nota_ser, observacion_final, estado, creado_en, actualizado_en)
                SELECT gen_random_uuid(), id_institucion, period_id, id_estudiante, id_materia,
                       note, 'Generada desde calificaciones academicas', 'REGISTRADA', NOW(), NOW()
                FROM averages
                ON CONFLICT (id_estudiante, id_materia, id_periodo_evaluacion)
                    WHERE id_periodo_evaluacion IS NOT NULL
                DO UPDATE SET nota_ser = EXCLUDED.nota_ser,
                              observacion_final = EXCLUDED.observacion_final,
                              estado = 'REGISTRADA', actualizado_en = NOW()
                """, params);

        jdbc.update("""
                WITH averages AS (
                    SELECT c.id_institucion, p.id AS period_id, i.id_estudiante, ev.id_materia,
                           ROUND(AVG(c.nota_numerica) / 20, 2) AS note
                    FROM sia.calificacion c
                    JOIN sia.evaluacion ev ON ev.id = c.id_evaluacion
                    JOIN sia.inscripcion i ON i.id = c.id_inscripcion
                    JOIN sia.periodo_evaluacion p
                      ON p.id_institucion = ev.id_institucion
                     AND p.id_gestion_academica = i.id_gestion_academica
                     AND p.numero_periodo = ev.periodo
                    WHERE ev.id_institucion = :institutionId
                      AND ev.nombre LIKE :evaluationPrefix
                      AND c.nota_numerica IS NOT NULL
                    GROUP BY c.id_institucion, p.id, i.id_estudiante, ev.id_materia
                )
                INSERT INTO sia.autoevaluacion_trimestral
                    (id, id_institucion, id_periodo_evaluacion, id_estudiante, id_materia,
                     nota_autoevaluacion, comentario, estado, creado_en, actualizado_en)
                SELECT gen_random_uuid(), id_institucion, period_id, id_estudiante, id_materia,
                       note, 'Generada desde calificaciones academicas', 'REGISTRADA', NOW(), NOW()
                FROM averages
                ON CONFLICT (id_estudiante, id_materia, id_periodo_evaluacion)
                    WHERE id_periodo_evaluacion IS NOT NULL
                DO UPDATE SET nota_autoevaluacion = EXCLUDED.nota_autoevaluacion,
                              comentario = EXCLUDED.comentario,
                              estado = 'REGISTRADA', actualizado_en = NOW()
                """, params);
    }

    private void seedAttendanceDetails(MapSqlParameterSource params) {
        jdbc.update("""
                WITH enrolled AS (
                    SELECT i.id AS enrollment_id, i.id_paralelo,
                           MOD((ROW_NUMBER() OVER (ORDER BY e.codigo_estudiante) - 1)::integer, 20) AS profile
                    FROM sia.inscripcion i
                    JOIN sia.estudiante e ON e.id = i.id_estudiante AND e.id_institucion = i.id_institucion
                    WHERE i.id_institucion = :institutionId
                      AND i.id_gestion_academica = :managementId
                      AND i.estado = 'ACTIVA'
                ), sessions AS (
                    SELECT ar.id AS attendance_id, ad.id_paralelo,
                           ROW_NUMBER() OVER (PARTITION BY ad.id_paralelo ORDER BY ar.fecha) AS session_number
                    FROM sia.asistencia_registro ar
                    JOIN sia.asignacion_docente ad ON ad.id = ar.id_asignacion_docente
                    WHERE ar.id_institucion = :institutionId
                      AND ad.id_gestion_academica = :managementId
                      AND ar.fecha >= :attendanceStart
                      AND ar.fecha < :attendanceEnd
                )
                INSERT INTO sia.asistencia_detalle
                    (id, id_institucion, id_asistencia_registro, id_inscripcion,
                     estado_asistencia, creado_en, actualizado_en)
                SELECT gen_random_uuid(), :institutionId, s.attendance_id, en.enrollment_id,
                       CASE
                           WHEN en.profile BETWEEN 1 AND 10 THEN 'PRESENTE'
                           WHEN en.profile BETWEEN 11 AND 14 AND s.session_number <= 7 THEN 'PRESENTE'
                           WHEN en.profile BETWEEN 15 AND 17 AND s.session_number <= 4 THEN 'PRESENTE'
                           WHEN en.profile BETWEEN 18 AND 19 AND s.session_number = 1 THEN 'PRESENTE'
                           ELSE 'AUSENTE'
                       END,
                       NOW(), NOW()
                FROM enrolled en
                JOIN sessions s ON s.id_paralelo = en.id_paralelo
                ON CONFLICT (id_asistencia_registro, id_inscripcion) DO UPDATE
                    SET estado_asistencia = EXCLUDED.estado_asistencia,
                        actualizado_en = NOW()
                """, params);
    }

    private Map<String, Integer> expectedProfiles(MapSqlParameterSource params) {
        String sql = """
                WITH profiles AS (
                    SELECT MOD((ROW_NUMBER() OVER (ORDER BY e.codigo_estudiante) - 1)::integer, 20) AS profile
                    FROM sia.inscripcion i
                    JOIN sia.estudiante e ON e.id = i.id_estudiante AND e.id_institucion = i.id_institucion
                    WHERE i.id_institucion = :institutionId
                      AND i.id_gestion_academica = :managementId
                      AND i.estado = 'ACTIVA'
                )
                SELECT COUNT(*) FILTER (WHERE profile BETWEEN 1 AND 10) AS low,
                       COUNT(*) FILTER (WHERE profile BETWEEN 11 AND 14) AS medium,
                       COUNT(*) FILTER (WHERE profile BETWEEN 15 AND 17) AS high,
                       COUNT(*) FILTER (WHERE profile = 0 OR profile BETWEEN 18 AND 19) AS critical,
                       0 AS no_data
                FROM profiles
                """;
        return jdbc.queryForObject(sql, params, (rs, rowNum) -> {
            Map<String, Integer> result = new LinkedHashMap<>();
            result.put("BAJO", rs.getInt("low"));
            result.put("MEDIO", rs.getInt("medium"));
            result.put("ALTO", rs.getInt("high"));
            result.put("CRITICO", rs.getInt("critical"));
            result.put("SIN_DATOS", rs.getInt("no_data"));
            return result;
        });
    }

    private int count(String table, String where, MapSqlParameterSource params) {
        Integer value = jdbc.queryForObject("SELECT COUNT(*) FROM sia." + table + " WHERE " + where,
                params, Integer.class);
        return value == null ? 0 : value;
    }

    private MapSqlParameterSource copy(MapSqlParameterSource source) {
        return new MapSqlParameterSource(source.getValues());
    }
}
