-- ============================================================
-- Migración: Dimensiones Dinámicas y Escalabilidad
-- Fecha: 2026-06-04
-- Descripción: Agrega soporte para dimensiones de evaluación
--              configurables por institución/gestión
-- ============================================================

-- Requisito para gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1. Tabla de Dimensiones (globales + por institución)
CREATE TABLE IF NOT EXISTS sia.dimension (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID REFERENCES sia.institucion(id) ON DELETE CASCADE,
    nombre VARCHAR(50) NOT NULL,
    descripcion TEXT,
    peso_default INTEGER NOT NULL DEFAULT 0,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    es_global BOOLEAN NOT NULL DEFAULT FALSE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_dimension_institucion_nombre UNIQUE (id_institucion, nombre)
);

-- Si la tabla ya existía sin default en id, lo corregimos para re-ejecuciones
ALTER TABLE sia.dimension
    ALTER COLUMN id SET DEFAULT gen_random_uuid();

-- Asegurar defaults de timestamps en tablas preexistentes
ALTER TABLE sia.dimension
    ALTER COLUMN creado_en SET DEFAULT NOW(),
    ALTER COLUMN actualizado_en SET DEFAULT NOW();

-- Índices
CREATE INDEX IF NOT EXISTS idx_dimension_institucion ON sia.dimension (id_institucion);
CREATE INDEX IF NOT EXISTS idx_dimension_estado ON sia.dimension (estado);
CREATE INDEX IF NOT EXISTS idx_dimension_global ON sia.dimension (es_global) WHERE es_global = TRUE;

-- 2. Tabla de ponderaciones por período (reemplaza columnas fijas para escalabilidad)
CREATE TABLE IF NOT EXISTS sia.periodo_dimension (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_periodo_evaluacion UUID NOT NULL REFERENCES sia.periodo_evaluacion(id) ON DELETE CASCADE,
    id_dimension UUID NOT NULL REFERENCES sia.dimension(id) ON DELETE RESTRICT,
    ponderacion INTEGER NOT NULL DEFAULT 0,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_periodo_dimension UNIQUE (id_periodo_evaluacion, id_dimension)
);

CREATE INDEX IF NOT EXISTS idx_periodo_dimension_periodo ON sia.periodo_dimension (id_periodo_evaluacion);

-- 3. Tabla de solicitudes de eliminación de dimensiones (workflow de aprobación)
CREATE TABLE IF NOT EXISTS sia.solicitud_eliminacion_dimension (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES sia.institucion(id) ON DELETE CASCADE,
    id_periodo_evaluacion UUID NOT NULL REFERENCES sia.periodo_evaluacion(id) ON DELETE CASCADE,
    id_dimension UUID NOT NULL REFERENCES sia.dimension(id) ON DELETE CASCADE,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    id_usuario_solicitud UUID NOT NULL,
    fecha_solicitud TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    id_usuario_resolucion UUID,
    fecha_resolucion TIMESTAMPTZ,
    observacion TEXT,
    CONSTRAINT uq_solicitud_dimension_periodo UNIQUE (id_periodo_evaluacion, id_dimension)
);

CREATE INDEX IF NOT EXISTS idx_solicitud_dimension_estado ON sia.solicitud_eliminacion_dimension (estado);

-- 4. Ampliar dimensión en actividad_evaluativa para aceptar cualquier nombre de dimensión
ALTER TABLE sia.actividad_evaluativa DROP CONSTRAINT IF EXISTS actividad_evaluativa_dimension_check;
ALTER TABLE sia.actividad_evaluativa ALTER COLUMN dimension TYPE VARCHAR(50);

-- 5. Insertar dimensiones por defecto (globales) para el sistema educativo boliviano
INSERT INTO sia.dimension (id, id_institucion, nombre, descripcion, peso_default, estado, es_global, creado_en, actualizado_en) VALUES
    (gen_random_uuid(), NULL, 'SER', 'Dimensión del Ser: evalúa valores, comportamiento y actitudes del estudiante', 10, 'ACTIVO', TRUE, NOW(), NOW()),
    (gen_random_uuid(), NULL, 'SABER', 'Dimensión del Saber: evalúa conocimientos teóricos y conceptuales', 45, 'ACTIVO', TRUE, NOW(), NOW()),
    (gen_random_uuid(), NULL, 'HACER', 'Dimensión del Hacer: evalúa habilidades prácticas y aplicación', 40, 'ACTIVO', TRUE, NOW(), NOW()),
    (gen_random_uuid(), NULL, 'AUTOEVALUACION', 'Dimensión de Autoevaluación: reflexión del estudiante sobre su propio aprendizaje', 5, 'ACTIVO', TRUE, NOW(), NOW())
ON CONFLICT (id_institucion, nombre) DO NOTHING;

-- 6. Función trigger para actualizar actualizado_en
CREATE OR REPLACE FUNCTION sia.fn_actualizar_actualizado_en()
RETURNS TRIGGER AS $$
BEGIN
    NEW.actualizado_en = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Aplicar triggers a nuevas tablas
DROP TRIGGER IF EXISTS trg_dimension_actualizado_en ON sia.dimension;
CREATE TRIGGER trg_dimension_actualizado_en
    BEFORE UPDATE ON sia.dimension
    FOR EACH ROW EXECUTE FUNCTION sia.fn_actualizar_actualizado_en();

DROP TRIGGER IF EXISTS trg_periodo_dimension_actualizado_en ON sia.periodo_dimension;
CREATE TRIGGER trg_periodo_dimension_actualizado_en
    BEFORE UPDATE ON sia.periodo_dimension
    FOR EACH ROW EXECUTE FUNCTION sia.fn_actualizar_actualizado_en();
