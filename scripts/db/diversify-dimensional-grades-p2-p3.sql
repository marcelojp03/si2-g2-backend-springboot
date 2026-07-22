BEGIN;

SET LOCAL search_path TO sia, public;

-- Corrige datos sinteticos repetidos sin usar random(): la variacion queda
-- estable entre ejecuciones y conserva aproximadamente el perfil de riesgo.
CREATE TEMP TABLE diversified_activity_grades ON COMMIT DROP AS
WITH source_grades AS (
    SELECT ca.id,
           ca.id_actividad,
           ca.id_estudiante,
           a.id_materia,
           p.numero_periodo,
           CASE
               WHEN ca.observacion = 'Diversificada por alumno para datos sinteticos' THEN
                   ca.nota_obtenida
                   - (MOD(MOD(hashtextextended(
                         ca.id_estudiante::text || ':' || p.numero_periodo, 0), 601) + 601, 601)::numeric / 100 - 3)
                   - (MOD(MOD(hashtextextended(
                         ca.id_estudiante::text || ':' || ca.id_actividad::text, 0), 401) + 401, 401)::numeric / 100 - 2)
               WHEN ca.observacion = 'Diversificada v2 por alumno para datos sinteticos' THEN
                   ca.nota_obtenida
                   - (MOD(MOD(hashtextextended(
                         ca.id_estudiante::text || ':' || p.numero_periodo, 0), 1301) + 1301, 1301)::numeric / 100 - 6.5)
                   - (MOD(MOD(hashtextextended(
                         ca.id_estudiante::text || ':' || ca.id_actividad::text, 0), 801) + 801, 801)::numeric / 100 - 4)
                   - (MOD(MOD(hashtextextended(
                         ca.id_estudiante::text || ':' || a.id_materia::text, 0), 401) + 401, 401)::numeric / 100 - 2)
               ELSE ca.nota_obtenida
           END AS nota_base
    FROM calificacion_actividad ca
    JOIN actividad_evaluativa a ON a.id = ca.id_actividad
    JOIN periodo_evaluacion p ON p.id = a.id_periodo_evaluacion
    JOIN institucion inst ON inst.id = ca.id_institucion
    WHERE inst.codigo = 'CSM-001'
      AND p.numero_periodo IN (2, 3)
      AND ca.nota_obtenida IS NOT NULL
      AND ca.observacion IS DISTINCT FROM 'Diversificada v3 por alumno para datos sinteticos'
), provisional_grades AS (
    SELECT id,
           id_actividad,
           id_estudiante,
           ROUND(LEAST(100, GREATEST(0,
           nota_base
           + (MOD(MOD(hashtextextended(
                 id_estudiante::text || ':' || numero_periodo, 0), 1301) + 1301, 1301)::numeric / 100 - 6.5)
           + (MOD(MOD(hashtextextended(
                 id_estudiante::text || ':' || id_actividad::text, 0), 801) + 801, 801)::numeric / 100 - 4)
           + (MOD(MOD(hashtextextended(
                 id_estudiante::text || ':' || id_materia::text, 0), 401) + 401, 401)::numeric / 100 - 2)
           )), 2) AS nota_previa
    FROM source_grades
), ranked_grades AS (
    SELECT provisional_grades.*,
           ROW_NUMBER() OVER (
               PARTITION BY id_actividad ORDER BY nota_previa, id_estudiante) AS posicion,
           COUNT(*) OVER (PARTITION BY id_actividad) AS cantidad
    FROM provisional_grades
)
SELECT id,
       id_actividad,
       id_estudiante,
       ROUND(LEAST(100, GREATEST(0,
           nota_previa * 0.85
           + CASE WHEN cantidad = 1 THEN nota_previa * 0.15
                  ELSE ((posicion - 1)::numeric * 100 / (cantidad - 1)) * 0.15
             END
       )), 2) AS nota_nueva
FROM ranked_grades;

UPDATE calificacion_actividad ca
SET nota_obtenida = d.nota_nueva,
    observacion = 'Diversificada v3 por alumno para datos sinteticos',
    actualizado_en = NOW()
FROM diversified_activity_grades d
WHERE d.id = ca.id;

-- Las actividades migradas comparten UUID con evaluacion; sincroniza la nota
-- legacy para que ambos modelos entreguen el mismo valor.
UPDATE calificacion c
SET nota_numerica = d.nota_nueva,
    actualizado_en = NOW()
FROM diversified_activity_grades d
JOIN actividad_evaluativa a ON a.id = d.id_actividad
JOIN inscripcion i ON i.id_estudiante = d.id_estudiante
                       AND i.id_institucion = a.id_institucion
JOIN periodo_evaluacion p ON p.id = a.id_periodo_evaluacion
WHERE c.id_evaluacion = a.id
  AND c.id_inscripcion = i.id
  AND i.id_gestion_academica = p.id_gestion_academica;

CREATE TEMP TABLE diversified_averages ON COMMIT DROP AS
SELECT ca.id_institucion,
       a.id_periodo_evaluacion,
       ca.id_estudiante,
       a.id_materia,
       AVG(ca.nota_obtenida) AS promedio
FROM calificacion_actividad ca
JOIN actividad_evaluativa a ON a.id = ca.id_actividad
JOIN periodo_evaluacion p ON p.id = a.id_periodo_evaluacion
JOIN institucion inst ON inst.id = ca.id_institucion
WHERE inst.codigo = 'CSM-001'
  AND p.numero_periodo IN (2, 3)
  AND a.dimension IN ('SABER', 'HACER')
  AND ca.nota_obtenida IS NOT NULL
  AND EXISTS (SELECT 1 FROM diversified_activity_grades)
GROUP BY ca.id_institucion, a.id_periodo_evaluacion,
         ca.id_estudiante, a.id_materia;

UPDATE calificacion_ser cs
SET nota_ser = ROUND(LEAST(100, GREATEST(0, av.promedio)) / 10, 2),
    observacion_final = 'Recalculada desde actividades diversificadas',
    actualizado_en = NOW()
FROM diversified_averages av
WHERE cs.id_institucion = av.id_institucion
  AND cs.id_periodo_evaluacion = av.id_periodo_evaluacion
  AND cs.id_estudiante = av.id_estudiante
  AND cs.id_materia = av.id_materia;

UPDATE autoevaluacion_trimestral auto_grade
SET nota_autoevaluacion = ROUND(LEAST(100, GREATEST(0, av.promedio)) / 20, 2),
    comentario = 'Recalculada desde actividades diversificadas',
    actualizado_en = NOW()
FROM diversified_averages av
WHERE auto_grade.id_institucion = av.id_institucion
  AND auto_grade.id_periodo_evaluacion = av.id_periodo_evaluacion
  AND auto_grade.id_estudiante = av.id_estudiante
  AND auto_grade.id_materia = av.id_materia;

-- Las alertas persistidas deben recalcularse antes de considerarse vigentes.
UPDATE alerta_riesgo ar
SET datos_vigentes = FALSE,
    actualizado_en = NOW()
FROM institucion inst
WHERE inst.id = ar.id_institucion
  AND inst.codigo = 'CSM-001'
  AND ar.activa = TRUE
  AND EXISTS (SELECT 1 FROM diversified_activity_grades);

DO $$
DECLARE
    out_of_range BIGINT;
BEGIN
    SELECT COUNT(*) INTO out_of_range
    FROM diversified_activity_grades
    WHERE nota_nueva < 0 OR nota_nueva > 100;

    IF out_of_range > 0 THEN
        RAISE EXCEPTION 'Diversificacion invalida: % notas fuera de rango', out_of_range;
    END IF;
END $$;

COMMIT;
