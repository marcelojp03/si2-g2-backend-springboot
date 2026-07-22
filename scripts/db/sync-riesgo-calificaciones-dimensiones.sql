BEGIN;

SET LOCAL search_path TO sia, public;

ALTER TABLE actividad_evaluativa
    ADD COLUMN IF NOT EXISTS id_periodo_evaluacion UUID;

ALTER TABLE calificacion_ser
    ADD COLUMN IF NOT EXISTS id_periodo_evaluacion UUID,
    ADD COLUMN IF NOT EXISTS observacion_final TEXT;

ALTER TABLE autoevaluacion_trimestral
    ADD COLUMN IF NOT EXISTS id_periodo_evaluacion UUID;

-- El modelo nuevo no escribe estas columnas legacy. Se conservan para no perder datos históricos.
ALTER TABLE actividad_evaluativa
    ALTER COLUMN id_periodo_trimestral DROP NOT NULL,
    ALTER COLUMN id_gestion_academica DROP NOT NULL,
    ALTER COLUMN id_curso DROP NOT NULL,
    ALTER COLUMN id_paralelo DROP NOT NULL,
    ALTER COLUMN tipo_actividad DROP NOT NULL;

ALTER TABLE calificacion_ser
    ALTER COLUMN id_trimestre DROP NOT NULL,
    ALTER COLUMN id_gestion_academica DROP NOT NULL,
    ALTER COLUMN id_curso DROP NOT NULL,
    ALTER COLUMN id_paralelo DROP NOT NULL,
    ALTER COLUMN id_docente DROP NOT NULL;

ALTER TABLE autoevaluacion_trimestral
    ALTER COLUMN id_trimestre DROP NOT NULL,
    ALTER COLUMN id_gestion_academica DROP NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_actividad_periodo_evaluacion') THEN
        ALTER TABLE actividad_evaluativa ADD CONSTRAINT fk_actividad_periodo_evaluacion
            FOREIGN KEY (id_periodo_evaluacion) REFERENCES periodo_evaluacion(id) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_calificacion_ser_periodo_evaluacion') THEN
        ALTER TABLE calificacion_ser ADD CONSTRAINT fk_calificacion_ser_periodo_evaluacion
            FOREIGN KEY (id_periodo_evaluacion) REFERENCES periodo_evaluacion(id) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_autoevaluacion_periodo_evaluacion') THEN
        ALTER TABLE autoevaluacion_trimestral ADD CONSTRAINT fk_autoevaluacion_periodo_evaluacion
            FOREIGN KEY (id_periodo_evaluacion) REFERENCES periodo_evaluacion(id) ON DELETE CASCADE;
    END IF;
END $$;

DROP INDEX IF EXISTS uq_actividad_periodo_evaluacion_nombre;
CREATE UNIQUE INDEX uq_actividad_periodo_evaluacion_nombre
    ON actividad_evaluativa (id_institucion, id_periodo_evaluacion, id_materia, nombre_actividad)
    WHERE id_periodo_evaluacion IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_actividad_periodo_evaluacion_dimension
    ON actividad_evaluativa (id_institucion, id_periodo_evaluacion, dimension, estado);
CREATE UNIQUE INDEX IF NOT EXISTS uq_calificacion_ser_periodo_evaluacion
    ON calificacion_ser (id_estudiante, id_materia, id_periodo_evaluacion)
    WHERE id_periodo_evaluacion IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_autoevaluacion_periodo_evaluacion
    ON autoevaluacion_trimestral (id_estudiante, id_materia, id_periodo_evaluacion)
    WHERE id_periodo_evaluacion IS NOT NULL;

CREATE TABLE IF NOT EXISTS dimension (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID REFERENCES institucion(id) ON DELETE CASCADE,
    nombre VARCHAR(50) NOT NULL,
    descripcion TEXT,
    peso_default INTEGER NOT NULL DEFAULT 0,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    es_global BOOLEAN NOT NULL DEFAULT FALSE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_dimension_institucion_nombre UNIQUE (id_institucion, nombre)
);

CREATE TABLE IF NOT EXISTS periodo_dimension (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_periodo_evaluacion UUID NOT NULL REFERENCES periodo_evaluacion(id) ON DELETE CASCADE,
    id_dimension UUID NOT NULL REFERENCES dimension(id) ON DELETE RESTRICT,
    ponderacion INTEGER NOT NULL DEFAULT 0,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_periodo_dimension UNIQUE (id_periodo_evaluacion, id_dimension)
);

COMMIT;
