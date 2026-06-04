-- =========================================================
-- SPRINT CALIFICACIONES DINÁMICAS
-- Modelo Boliviano: SER / SABER / HACER / AUTO
-- Resolución Ministerial 001/2026
-- =========================================================
--取代旧 tables: periodo_trimestral, actividad_evaluativa,
-- calificacion_actividad, calificacion_ser, autoevaluacion_trimestral
-- 新: periodo_evaluacion (dinámico: BIMESTRAL/TRIMESTRAL/SEMESTRAL/ANUAL)
-- =========================================================

BEGIN;

-- =========================================================
-- 1. Modificar gestion_academica: agregar tipo_periodo y cantidad_periodos
-- =========================================================

ALTER TABLE gestion_academica
ADD COLUMN IF NOT EXISTS tipo_periodo VARCHAR(20) DEFAULT 'BIMESTRAL'
CHECK (tipo_periodo IN ('BIMESTRAL', 'TRIMESTRAL', 'SEMESTRAL', 'ANUAL'));

ALTER TABLE gestion_academica
ADD COLUMN IF NOT EXISTS cantidad_periodos INTEGER DEFAULT 4;

-- =========================================================
-- 2. Crear tabla periodo_evaluacion (reemplaza periodo_trimestral)
-- =========================================================

CREATE TABLE periodo_evaluacion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_gestion_academica UUID NOT NULL REFERENCES gestion_academica(id) ON DELETE CASCADE,
    numero_periodo INTEGER NOT NULL,
    tipo_periodo VARCHAR(20) NOT NULL CHECK (tipo_periodo IN ('BIMESTRAL', 'TRIMESTRAL', 'SEMESTRAL', 'ANUAL')),
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ABIERTO' CHECK (estado IN ('ABIERTO', 'EN_CIERRE', 'CERRADO', 'REABIERTO')),
    peso_ser INTEGER NOT NULL DEFAULT 10,
    peso_saber INTEGER NOT NULL DEFAULT 45,
    peso_hacer INTEGER NOT NULL DEFAULT 40,
    peso_auto INTEGER NOT NULL DEFAULT 5,
    fecha_cierre TIMESTAMPTZ NULL,
    justificacion_cierre VARCHAR(500) NULL,
    id_usuario_cierre UUID NULL REFERENCES usuario(id) ON DELETE SET NULL,
    fecha_reapertura TIMESTAMPTZ NULL,
    justificacion_reapertura VARCHAR(500) NULL,
    id_usuario_reapertura UUID NULL REFERENCES usuario(id) ON DELETE SET NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_periodo_evaluacion UNIQUE (id_institucion, id_gestion_academica, numero_periodo),
    CONSTRAINT ck_periodo_evaluacion_fechas CHECK (fecha_fin >= fecha_inicio),
    CONSTRAINT ck_periodo_evaluacion_numero CHECK (numero_periodo >= 1 AND numero_periodo <= 12)
);

CREATE INDEX idx_periodo_evaluacion_institucion_gestion
    ON periodo_evaluacion (id_institucion, id_gestion_academica, estado);
CREATE INDEX idx_periodo_evaluacion_estado
    ON periodo_evaluacion (id_institucion, estado);

-- =========================================================
-- 3. Modificar actividad_evaluativa
-- =========================================================

ALTER TABLE actividad_evaluativa
DROP CONSTRAINT IF EXISTS fk_actividad_periodo_trimestral,
DROP COLUMN IF EXISTS id_periodo_trimestral,
DROP COLUMN IF EXISTS id_gestion_academica,
DROP COLUMN IF EXISTS id_curso,
DROP COLUMN IF EXISTS id_paralelo,
DROP COLUMN IF EXISTS tipo_actividad,
DROP CONSTRAINT IF EXISTS ck_actividad_dimension;

ALTER TABLE actividad_evaluativa
ADD COLUMN id_periodo_evaluacion UUID REFERENCES periodo_evaluacion(id) ON DELETE CASCADE,
ADD COLUMN id_materia UUID NOT NULL REFERENCES materia(id) ON DELETE CASCADE,
ADD COLUMN id_docente UUID NOT NULL REFERENCES docente(id) ON DELETE CASCADE,
ADD COLUMN dimension VARCHAR(15) NOT NULL CHECK (dimension IN ('SABER', 'HACER')),
ADD COLUMN nombre_actividad VARCHAR(150) NOT NULL,
ADD COLUMN fecha_actividad DATE NOT NULL,
ADD COLUMN descripcion_evidencia TEXT,
ADD COLUMN puntaje_maximo INTEGER NOT NULL DEFAULT 100 CHECK (puntaje_maximo > 0 AND puntaje_maximo <= 100),
ADD COLUMN estado VARCHAR(20) NOT NULL DEFAULT 'BORRADOR' CHECK (estado IN ('BORRADOR', 'PUBLICADA', 'CERRADA')),
ADD COLUMN publicado_en TIMESTAMPTZ NULL,
ADD COLUMN id_usuario_registro UUID NULL REFERENCES usuario(id) ON DELETE SET NULL,
ADD COLUMN id_usuario_modificacion UUID NULL REFERENCES usuario(id) ON DELETE SET NULL;

DROP INDEX IF EXISTS idx_actividad_evaluativa_periodo_dimension;
DROP INDEX IF EXISTS uq_actividad_evaluativa_periodo_nombre;

ALTER TABLE actividad_evaluativa
DROP CONSTRAINT IF EXISTS uq_actividad_evaluativa_periodo_nombre,
ADD CONSTRAINT uq_actividad_evaluativa_periodo_nombre
    UNIQUE (id_institucion, id_periodo_evaluacion, nombre_actividad);

CREATE INDEX idx_actividad_evaluativa_periodo_dimension
    ON actividad_evaluativa (id_institucion, id_periodo_evaluacion, dimension, estado);
CREATE INDEX idx_actividad_evaluativa_materia
    ON actividad_evaluativa (id_institucion, id_materia);

-- =========================================================
-- 4. Modificar calificacion_actividad
-- =========================================================

ALTER TABLE calificacion_actividad
DROP COLUMN IF EXISTS id_usuario_registro,
DROP COLUMN IF EXISTS id_usuario_modificacion;

ALTER TABLE calificacion_actividad
ADD COLUMN observacion TEXT,
ADD COLUMN estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE', 'REGISTRADA', 'PUBLICADA', 'MODIFICADA')),
ADD COLUMN id_usuario_registro UUID NULL REFERENCES usuario(id) ON DELETE SET NULL,
ADD COLUMN id_usuario_modificacion UUID NULL REFERENCES usuario(id) ON DELETE SET NULL;

DROP INDEX IF EXISTS idx_calificacion_actividad_actividad;

CREATE INDEX idx_calificacion_actividad_actividad
    ON calificacion_actividad (id_actividad, id_estudiante);
CREATE INDEX idx_calificacion_actividad_estado
    ON calificacion_actividad (id_institucion, estado);

-- =========================================================
-- 5. Crear tabla observacion_ser (bitácora cualitativa)
-- =========================================================

CREATE TABLE observacion_ser (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_periodo_evaluacion UUID NOT NULL REFERENCES periodo_evaluacion(id) ON DELETE CASCADE,
    id_estudiante UUID NOT NULL REFERENCES estudiante(id) ON DELETE CASCADE,
    id_docente UUID NOT NULL REFERENCES docente(id) ON DELETE CASCADE,
    id_materia UUID NOT NULL REFERENCES materia(id) ON DELETE CASCADE,
    fecha_observacion DATE NOT NULL,
    comportamiento VARCHAR(50) NOT NULL CHECK (comportamiento IN ('RESPETO', 'PUNTUALIDAD', 'SOLIDARIDAD', 'HONESTIDAD', 'PARTICIPACION', 'RESPONSABILIDAD', 'OTRO')),
    descripcion TEXT,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_observacion_ser_periodo_estudiante
    ON observacion_ser (id_institucion, id_periodo_evaluacion, id_estudiante);
CREATE INDEX idx_observacion_ser_fecha
    ON observacion_ser (id_institucion, id_docente, fecha_observacion);

-- =========================================================
-- 6. Modificar calificacion_ser (cierre global trimestral)
-- =========================================================

ALTER TABLE calificacion_ser
DROP COLUMN IF EXISTS id_trimestre,
DROP COLUMN IF EXISTS id_gestion_academica,
DROP COLUMN IF EXISTS id_curso,
DROP COLUMN IF EXISTS id_paralelo,
DROP COLUMN IF EXISTS estado;

ALTER TABLE calificacion_ser
ADD COLUMN id_periodo_evaluacion UUID NOT NULL REFERENCES periodo_evaluacion(id) ON DELETE CASCADE,
ADD COLUMN id_materia UUID NOT NULL REFERENCES materia(id) ON DELETE CASCADE,
ADD COLUMN observacion_final TEXT,
ADD COLUMN estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE', 'REGISTRADA', 'PUBLICADA')),
ADD COLUMN id_usuario_registro UUID NULL REFERENCES usuario(id) ON DELETE SET NULL,
ADD COLUMN id_usuario_modificacion UUID NULL REFERENCES usuario(id) ON DELETE SET NULL,
ADD COLUMN CHECK (nota_ser >= 0 AND nota_ser <= 10);

DROP INDEX IF EXISTS idx_calificacion_ser_periodo;
DROP CONSTRAINT IF EXISTS uq_calificacion_ser;

ALTER TABLE calificacion_ser
ADD CONSTRAINT uq_calificacion_ser
    UNIQUE (id_estudiante, id_materia, id_periodo_evaluacion);

CREATE INDEX idx_calificacion_ser_periodo
    ON calificacion_ser (id_institucion, id_periodo_evaluacion, id_materia);

-- =========================================================
-- 7. Modificar autoevaluacion_trimestral
-- =========================================================

ALTER TABLE autoevaluacion_trimestral
DROP COLUMN IF EXISTS id_trimestre,
DROP COLUMN IF EXISTS id_gestion_academica,
DROP COLUMN IF EXISTS estado,
DROP COLUMN IF EXISTS id_usuario_registro,
DROP COLUMN IF EXISTS id_usuario_modificacion;

ALTER TABLE autoevaluacion_trimestral
ADD COLUMN id_periodo_evaluacion UUID NOT NULL REFERENCES periodo_evaluacion(id) ON DELETE CASCADE,
ADD COLUMN id_materia UUID NOT NULL REFERENCES materia(id) ON DELETE CASCADE,
ADD COLUMN comentario TEXT,
ADD COLUMN estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE', 'REGISTRADA', 'PUBLICADA')),
ADD COLUMN id_usuario_registro UUID NULL REFERENCES usuario(id) ON DELETE SET NULL,
ADD COLUMN id_usuario_modificacion UUID NULL REFERENCES usuario(id) ON DELETE SET NULL,
ADD COLUMN CHECK (nota_autoevaluacion >= 0 AND nota_autoevaluacion <= 5);

DROP INDEX IF EXISTS idx_autoevaluacion_trimestral_periodo;
DROP CONSTRAINT IF EXISTS uq_autoevaluacion_trimestral;

ALTER TABLE autoevaluacion_trimestral
ADD CONSTRAINT uq_autoevaluacion_trimestral
    UNIQUE (id_estudiante, id_materia, id_periodo_evaluacion);

CREATE INDEX idx_autoevaluacion_trimestral_periodo
    ON autoevaluacion_trimestral (id_institucion, id_periodo_evaluacion, id_materia);

-- =========================================================
-- 8. Eliminar tablas viejas (periodo_trimestral)
-- =========================================================

DROP TABLE IF EXISTS periodo_trimestral CASCADE;

COMMIT;

-- =========================================================
-- NOTA: Los pesos son fijos según Resolución 001/2026:
--   SER = 10, SABER = 45, HACER = 40, AUTO = 5
--   Total = 100 puntos, aprobación = 51
-- =========================================================