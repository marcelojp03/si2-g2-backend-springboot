BEGIN;

SET LOCAL search_path TO sia, public;

-- Alcance academico de CSM-001: un estudiante por materia y periodo segun su
-- inscripcion activa y las materias asignadas a su paralelo.
CREATE TEMP TABLE target_grade_scope ON COMMIT DROP AS
SELECT DISTINCT ON (i.id_estudiante, ad.id_materia, p.id)
       inst.id AS id_institucion,
       i.id AS id_inscripcion,
       i.id_estudiante,
       i.id_gestion_academica,
       ad.id_materia,
       p.id AS id_periodo_evaluacion,
       p.numero_periodo
FROM institucion inst
JOIN inscripcion i
  ON i.id_institucion = inst.id
 AND i.estado = 'ACTIVA'
JOIN asignacion_docente ad
  ON ad.id_institucion = inst.id
 AND ad.id_gestion_academica = i.id_gestion_academica
 AND ad.id_paralelo = i.id_paralelo
 AND ad.estado = 'ACTIVA'
JOIN periodo_evaluacion p
  ON p.id_institucion = inst.id
 AND p.id_gestion_academica = i.id_gestion_academica
WHERE inst.codigo = 'CSM-001'
ORDER BY i.id_estudiante, ad.id_materia, p.id, i.id;

CREATE INDEX ON target_grade_scope
    (id_periodo_evaluacion, id_materia, id_estudiante);

-- Completa P1 y P2 sin alterar notas existentes. Si el estudiante ya tiene la
-- otra dimension, conserva su tendencia; de lo contrario usa el promedio real
-- de la actividad y 60 solamente como ultimo recurso.
WITH missing_grades AS (
    SELECT s.id_institucion,
           s.id_estudiante,
           a.id AS id_actividad,
           ROUND(COALESCE(
               (
                   SELECT AVG(other_grade.nota_obtenida) * 0.90
                   FROM actividad_evaluativa other_activity
                   JOIN calificacion_actividad other_grade
                     ON other_grade.id_actividad = other_activity.id
                    AND other_grade.id_estudiante = s.id_estudiante
                   WHERE other_activity.id_institucion = s.id_institucion
                     AND other_activity.id_periodo_evaluacion = s.id_periodo_evaluacion
                     AND other_activity.id_materia = s.id_materia
                     AND other_activity.dimension IN ('SABER', 'HACER')
                     AND other_activity.dimension <> a.dimension
               ),
               (
                   SELECT AVG(class_grade.nota_obtenida)
                   FROM calificacion_actividad class_grade
                   WHERE class_grade.id_actividad = a.id
               ),
               60
           ), 2) AS nota
    FROM target_grade_scope s
    JOIN actividad_evaluativa a
      ON a.id_institucion = s.id_institucion
     AND a.id_periodo_evaluacion = s.id_periodo_evaluacion
     AND a.id_materia = s.id_materia
     AND a.dimension IN ('SABER', 'HACER')
    WHERE s.numero_periodo IN (1, 2)
      AND NOT EXISTS (
          SELECT 1
          FROM calificacion_actividad existing_grade
          WHERE existing_grade.id_actividad = a.id
            AND existing_grade.id_estudiante = s.id_estudiante
      )
)
INSERT INTO calificacion_actividad
    (id, id_institucion, id_actividad, id_estudiante, nota_obtenida,
     observacion, estado, creado_en, actualizado_en)
SELECT gen_random_uuid(), id_institucion, id_actividad, id_estudiante, nota,
       'Completada automaticamente para cobertura dimensional',
       'REGISTRADA', NOW(), NOW()
FROM missing_grades
ON CONFLICT (id_actividad, id_estudiante) DO NOTHING;

-- P3 replica la nota individual de P2 para la misma materia y dimension. Al
-- ejecutarse despues del bloque anterior, P2 ya esta completo para todos.
WITH missing_grades AS (
    SELECT s.id_institucion,
           s.id_estudiante,
           a.id AS id_actividad,
           ROUND(COALESCE(
               (
                   SELECT AVG(previous_grade.nota_obtenida)
                   FROM periodo_evaluacion previous_period
                   JOIN actividad_evaluativa previous_activity
                     ON previous_activity.id_institucion = s.id_institucion
                    AND previous_activity.id_periodo_evaluacion = previous_period.id
                    AND previous_activity.id_materia = s.id_materia
                    AND previous_activity.dimension = a.dimension
                   JOIN calificacion_actividad previous_grade
                     ON previous_grade.id_actividad = previous_activity.id
                    AND previous_grade.id_estudiante = s.id_estudiante
                   WHERE previous_period.id_institucion = s.id_institucion
                     AND previous_period.id_gestion_academica = s.id_gestion_academica
                     AND previous_period.numero_periodo = s.numero_periodo - 1
               ),
               (
                   SELECT AVG(class_grade.nota_obtenida)
                   FROM calificacion_actividad class_grade
                   WHERE class_grade.id_actividad = a.id
               ),
               60
           ), 2) AS nota
    FROM target_grade_scope s
    JOIN actividad_evaluativa a
      ON a.id_institucion = s.id_institucion
     AND a.id_periodo_evaluacion = s.id_periodo_evaluacion
     AND a.id_materia = s.id_materia
     AND a.dimension IN ('SABER', 'HACER')
    WHERE s.numero_periodo = 3
      AND NOT EXISTS (
          SELECT 1
          FROM calificacion_actividad existing_grade
          WHERE existing_grade.id_actividad = a.id
            AND existing_grade.id_estudiante = s.id_estudiante
      )
)
INSERT INTO calificacion_actividad
    (id, id_institucion, id_actividad, id_estudiante, nota_obtenida,
     observacion, estado, creado_en, actualizado_en)
SELECT gen_random_uuid(), id_institucion, id_actividad, id_estudiante, nota,
       'Completada automaticamente desde el periodo anterior',
       'REGISTRADA', NOW(), NOW()
FROM missing_grades
ON CONFLICT (id_actividad, id_estudiante) DO NOTHING;

-- Mantiene sincronizado el modelo legacy porque las actividades migradas
-- comparten UUID con evaluacion.
INSERT INTO calificacion
    (id, id_institucion, id_evaluacion, id_inscripcion, nota_numerica,
     creado_en, actualizado_en)
SELECT gen_random_uuid(), s.id_institucion, a.id, s.id_inscripcion,
       ca.nota_obtenida, NOW(), NOW()
FROM target_grade_scope s
JOIN actividad_evaluativa a
  ON a.id_institucion = s.id_institucion
 AND a.id_periodo_evaluacion = s.id_periodo_evaluacion
 AND a.id_materia = s.id_materia
 AND a.dimension IN ('SABER', 'HACER')
JOIN evaluacion e ON e.id = a.id
JOIN calificacion_actividad ca
  ON ca.id_actividad = a.id
 AND ca.id_estudiante = s.id_estudiante
ON CONFLICT (id_evaluacion, id_inscripcion) DO NOTHING;

-- SER y AUTO se calculan sobre el promedio de las actividades ya completas,
-- usando las escalas oficiales de 10 y 5 puntos respectivamente.
WITH activity_averages AS (
    SELECT s.id_institucion,
           s.id_periodo_evaluacion,
           s.id_estudiante,
           s.id_materia,
           AVG(ca.nota_obtenida) AS promedio
    FROM target_grade_scope s
    JOIN actividad_evaluativa a
      ON a.id_institucion = s.id_institucion
     AND a.id_periodo_evaluacion = s.id_periodo_evaluacion
     AND a.id_materia = s.id_materia
     AND a.dimension IN ('SABER', 'HACER')
    JOIN calificacion_actividad ca
      ON ca.id_actividad = a.id
     AND ca.id_estudiante = s.id_estudiante
    GROUP BY s.id_institucion, s.id_periodo_evaluacion,
             s.id_estudiante, s.id_materia
)
INSERT INTO calificacion_ser
    (id, id_institucion, id_periodo_evaluacion, id_estudiante, id_materia,
     nota_ser, observacion_final, estado, creado_en, actualizado_en)
SELECT gen_random_uuid(), id_institucion, id_periodo_evaluacion,
       id_estudiante, id_materia,
       ROUND(LEAST(100, GREATEST(0, promedio)) / 10, 2),
       'Generada desde actividades para cobertura dimensional',
       'REGISTRADA', NOW(), NOW()
FROM activity_averages
ON CONFLICT (id_estudiante, id_materia, id_periodo_evaluacion)
    WHERE id_periodo_evaluacion IS NOT NULL
DO NOTHING;

WITH activity_averages AS (
    SELECT s.id_institucion,
           s.id_periodo_evaluacion,
           s.id_estudiante,
           s.id_materia,
           AVG(ca.nota_obtenida) AS promedio
    FROM target_grade_scope s
    JOIN actividad_evaluativa a
      ON a.id_institucion = s.id_institucion
     AND a.id_periodo_evaluacion = s.id_periodo_evaluacion
     AND a.id_materia = s.id_materia
     AND a.dimension IN ('SABER', 'HACER')
    JOIN calificacion_actividad ca
      ON ca.id_actividad = a.id
     AND ca.id_estudiante = s.id_estudiante
    GROUP BY s.id_institucion, s.id_periodo_evaluacion,
             s.id_estudiante, s.id_materia
)
INSERT INTO autoevaluacion_trimestral
    (id, id_institucion, id_periodo_evaluacion, id_estudiante, id_materia,
     nota_autoevaluacion, comentario, estado, creado_en, actualizado_en)
SELECT gen_random_uuid(), id_institucion, id_periodo_evaluacion,
       id_estudiante, id_materia,
       ROUND(LEAST(100, GREATEST(0, promedio)) / 20, 2),
       'Generada desde actividades para cobertura dimensional',
       'REGISTRADA', NOW(), NOW()
FROM activity_averages
ON CONFLICT (id_estudiante, id_materia, id_periodo_evaluacion)
    WHERE id_periodo_evaluacion IS NOT NULL
DO NOTHING;

-- Si queda un solo campo sin nota, la transaccion completa se revierte.
DO $$
DECLARE
    missing_count BIGINT;
BEGIN
    SELECT COUNT(*) INTO missing_count
    FROM (
        SELECT s.id_estudiante
        FROM target_grade_scope s
        JOIN actividad_evaluativa a
          ON a.id_institucion = s.id_institucion
         AND a.id_periodo_evaluacion = s.id_periodo_evaluacion
         AND a.id_materia = s.id_materia
         AND a.dimension IN ('SABER', 'HACER')
        LEFT JOIN calificacion_actividad ca
          ON ca.id_actividad = a.id
         AND ca.id_estudiante = s.id_estudiante
        WHERE ca.id IS NULL OR ca.nota_obtenida IS NULL

        UNION ALL

        SELECT s.id_estudiante
        FROM target_grade_scope s
        LEFT JOIN calificacion_ser cs
          ON cs.id_periodo_evaluacion = s.id_periodo_evaluacion
         AND cs.id_materia = s.id_materia
         AND cs.id_estudiante = s.id_estudiante
        WHERE cs.id IS NULL OR cs.nota_ser IS NULL

        UNION ALL

        SELECT s.id_estudiante
        FROM target_grade_scope s
        LEFT JOIN autoevaluacion_trimestral auto_grade
          ON auto_grade.id_periodo_evaluacion = s.id_periodo_evaluacion
         AND auto_grade.id_materia = s.id_materia
         AND auto_grade.id_estudiante = s.id_estudiante
        WHERE auto_grade.id IS NULL OR auto_grade.nota_autoevaluacion IS NULL
    ) gaps;

    IF missing_count > 0 THEN
        RAISE EXCEPTION 'Cobertura dimensional incompleta: % campos sin nota', missing_count;
    END IF;
END $$;

COMMIT;
