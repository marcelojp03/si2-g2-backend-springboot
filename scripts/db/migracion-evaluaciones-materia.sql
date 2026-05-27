-- ====================================================================
-- SCRIPT DE MIGRACION: Cambiar estructura de evaluaciones
-- ====================================================================
-- Descripcion: 
--   Migra de una estructura basada en AsignacionDocente a una basada en Materia
--   Las evaluaciones ahora son por materia, no por asignacion docente
--
-- ADVERTENCIA: Ejecutar en orden:
--   1. Agregar columna id_materia a evaluacion (NULL temporal)
--   2. Migrar datos de asignacion a materia
--   3. Crear tabla evaluacion_materia (nueva)
--   4. Hacer backup de datos antiguos
--   5. Cambiar tabla evaluacion
-- ====================================================================

-- PASO 1: Agregar columna id_materia a la tabla evaluacion (temporal)
ALTER TABLE evaluacion ADD COLUMN id_materia UUID;

-- PASO 2: Migrar id_materia desde asignacion_docente
UPDATE evaluacion e 
SET e.id_materia = (
    SELECT ad.id_materia 
    FROM asignacion_docente ad 
    WHERE ad.id = e.id_asignacion_docente
)
WHERE e.id_asignacion_docente IS NOT NULL;

-- PASO 3: Validar que todos los registros tienen id_materia
-- (deberia retornar 0 registros)
SELECT COUNT(*) as registros_sin_materia 
FROM evaluacion 
WHERE id_materia IS NULL;

-- PASO 4: Hacer id_materia NOT NULL
ALTER TABLE evaluacion ALTER COLUMN id_materia SET NOT NULL;

-- PASO 5: Crear tabla evaluacion_materia (nueva)
-- (Esta tabla es identica a evaluacion pero sin id_asignacion_docente)
-- Nota: Si la tabla ya existe, omitir este paso
CREATE TABLE IF NOT EXISTS evaluacion_materia (
    id UUID PRIMARY KEY,
    id_institucion UUID NOT NULL,
    id_materia UUID NOT NULL,
    creado_por UUID,
    periodo INTEGER NOT NULL,
    tipo VARCHAR(40) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    ponderacion NUMERIC(5, 2) NOT NULL,
    escala VARCHAR(15) DEFAULT 'NUMERICA' NOT NULL,
    estado VARCHAR(15) DEFAULT 'ABIERTA' NOT NULL,
    creado_en TIMESTAMP NOT NULL,
    actualizado_en TIMESTAMP NOT NULL,
    CONSTRAINT uk_eval_materia_periodo_nombre UNIQUE(id_institucion, id_materia, periodo, nombre),
    CONSTRAINT fk_eval_materia_institucion FOREIGN KEY(id_institucion) REFERENCES institucion(id),
    CONSTRAINT fk_eval_materia_materia FOREIGN KEY(id_materia) REFERENCES materia(id)
);

-- PASO 6: Hacer backup de la tabla evaluacion antigua (renombrarla)
-- Nota: esto es opcional pero recomendado
ALTER TABLE evaluacion RENAME TO evaluacion_asignacion_backup;

-- PASO 7: Renombrar evaluacion_materia a evaluacion
ALTER TABLE evaluacion_materia RENAME TO evaluacion;

-- PASO 8: Crear indice en id_institucion y id_materia
CREATE INDEX IF NOT EXISTS idx_evaluacion_institucion_materia ON evaluacion(id_institucion, id_materia);
CREATE INDEX IF NOT EXISTS idx_evaluacion_institucion_materia_periodo ON evaluacion(id_institucion, id_materia, periodo);

-- ====================================================================
-- ROLLBACK (si algo falla):
-- ====================================================================
-- ALTER TABLE evaluacion RENAME TO evaluacion_materia;
-- ALTER TABLE evaluacion_asignacion_backup RENAME TO evaluacion;
-- ALTER TABLE evaluacion DROP COLUMN id_materia;
-- DROP TABLE evaluacion_materia;

-- ====================================================================
-- VERIFICACION: Confirmar que la migración fue exitosa
-- ====================================================================
-- SELECT COUNT(*) as total_evaluaciones FROM evaluacion;
-- SELECT DISTINCT id_materia FROM evaluacion LIMIT 10;
