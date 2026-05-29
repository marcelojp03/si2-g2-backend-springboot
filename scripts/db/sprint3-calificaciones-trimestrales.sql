-- =========================================================
-- HU-S2-18 - GESTION DE CALIFICACIONES TRIMESTRALES
-- Modelo Bolivia: SER / SABER / HACER / AUTOEVALUACION
-- PostgreSQL
-- =========================================================

SET search_path TO sia, public;

CREATE TABLE IF NOT EXISTS periodo_trimestral (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_gestion_academica UUID NOT NULL REFERENCES gestion_academica(id) ON DELETE CASCADE,
    numero_trimestre INTEGER NOT NULL CHECK (numero_trimestre BETWEEN 1 AND 3),
    estado VARCHAR(20) NOT NULL DEFAULT 'ABIERTO' CHECK (estado IN ('ABIERTO','EN_CIERRE','CERRADO','REABIERTO')),
    fecha_cierre TIMESTAMPTZ NULL,
    justificacion_cierre VARCHAR(500) NULL,
    id_usuario_cierre UUID NULL REFERENCES usuario(id) ON DELETE SET NULL,
    fecha_reapertura TIMESTAMPTZ NULL,
    justificacion_reapertura VARCHAR(500) NULL,
    id_usuario_reapertura UUID NULL REFERENCES usuario(id) ON DELETE SET NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_periodo_trimestral_gestion_numero UNIQUE (id_institucion, id_gestion_academica, numero_trimestre)
);

CREATE TABLE IF NOT EXISTS actividad_evaluativa (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_periodo_trimestral UUID NOT NULL REFERENCES periodo_trimestral(id) ON DELETE CASCADE,
    id_gestion_academica UUID NOT NULL REFERENCES gestion_academica(id) ON DELETE CASCADE,
    id_curso UUID NOT NULL REFERENCES curso(id) ON DELETE CASCADE,
    id_paralelo UUID NOT NULL REFERENCES paralelo(id) ON DELETE CASCADE,
    id_materia UUID NOT NULL REFERENCES materia(id) ON DELETE CASCADE,
    id_docente UUID NOT NULL REFERENCES docente(id) ON DELETE CASCADE,
    nombre_actividad VARCHAR(150) NOT NULL,
    tipo_actividad VARCHAR(30) NOT NULL,
    dimension VARCHAR(15) NOT NULL CHECK (dimension IN ('SABER','HACER')),
    puntaje_maximo INTEGER NOT NULL CHECK (puntaje_maximo IN (40,45)),
    fecha_actividad TIMESTAMPTZ NOT NULL,
    descripcion VARCHAR(1000) NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'BORRADOR' CHECK (estado IN ('BORRADOR','PUBLICADA','CERRADA')),
    publicado_en TIMESTAMPTZ NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_actividad_evaluativa_periodo_nombre UNIQUE (id_institucion, id_periodo_trimestral, nombre_actividad)
);

CREATE TABLE IF NOT EXISTS calificacion_actividad (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_actividad UUID NOT NULL REFERENCES actividad_evaluativa(id) ON DELETE CASCADE,
    id_estudiante UUID NOT NULL REFERENCES estudiante(id) ON DELETE CASCADE,
    nota_obtenida NUMERIC(5,2) NULL,
    observacion VARCHAR(500) NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE','REGISTRADA','PUBLICADA','MODIFICADA')),
    id_usuario_registro UUID NULL REFERENCES usuario(id) ON DELETE SET NULL,
    id_usuario_modificacion UUID NULL REFERENCES usuario(id) ON DELETE SET NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_calificacion_actividad_estudiante UNIQUE (id_actividad, id_estudiante)
);

CREATE TABLE IF NOT EXISTS calificacion_ser (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_trimestre UUID NOT NULL REFERENCES periodo_trimestral(id) ON DELETE CASCADE,
    id_gestion_academica UUID NOT NULL REFERENCES gestion_academica(id) ON DELETE CASCADE,
    id_curso UUID NOT NULL REFERENCES curso(id) ON DELETE CASCADE,
    id_paralelo UUID NOT NULL REFERENCES paralelo(id) ON DELETE CASCADE,
    id_materia UUID NOT NULL REFERENCES materia(id) ON DELETE CASCADE,
    id_docente UUID NOT NULL REFERENCES docente(id) ON DELETE CASCADE,
    id_estudiante UUID NOT NULL REFERENCES estudiante(id) ON DELETE CASCADE,
    nota_ser NUMERIC(5,2) NOT NULL CHECK (nota_ser BETWEEN 0 AND 10),
    observacion VARCHAR(500) NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'REGISTRADA' CHECK (estado IN ('PENDIENTE','REGISTRADA','PUBLICADA','MODIFICADA')),
    id_usuario_registro UUID NULL REFERENCES usuario(id) ON DELETE SET NULL,
    id_usuario_modificacion UUID NULL REFERENCES usuario(id) ON DELETE SET NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_calificacion_ser UNIQUE (id_estudiante, id_materia, id_trimestre, id_gestion_academica)
);

CREATE TABLE IF NOT EXISTS autoevaluacion_trimestral (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_trimestre UUID NOT NULL REFERENCES periodo_trimestral(id) ON DELETE CASCADE,
    id_gestion_academica UUID NOT NULL REFERENCES gestion_academica(id) ON DELETE CASCADE,
    id_materia UUID NOT NULL REFERENCES materia(id) ON DELETE CASCADE,
    id_estudiante UUID NOT NULL REFERENCES estudiante(id) ON DELETE CASCADE,
    nota_autoevaluacion NUMERIC(5,2) NOT NULL CHECK (nota_autoevaluacion BETWEEN 0 AND 5),
    comentario VARCHAR(1000) NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE','REGISTRADA','PUBLICADA')),
    id_usuario_registro UUID NULL REFERENCES usuario(id) ON DELETE SET NULL,
    id_usuario_modificacion UUID NULL REFERENCES usuario(id) ON DELETE SET NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_autoevaluacion_trimestral UNIQUE (id_estudiante, id_materia, id_trimestre, id_gestion_academica)
);

CREATE INDEX IF NOT EXISTS idx_periodo_trimestral_institucion_gestion
    ON periodo_trimestral (id_institucion, id_gestion_academica, numero_trimestre);

CREATE INDEX IF NOT EXISTS idx_actividad_evaluativa_periodo_dimension
    ON actividad_evaluativa (id_institucion, id_periodo_trimestral, dimension, estado);

CREATE INDEX IF NOT EXISTS idx_calificacion_actividad_actividad
    ON calificacion_actividad (id_actividad, id_estudiante);

CREATE INDEX IF NOT EXISTS idx_calificacion_ser_periodo
    ON calificacion_ser (id_institucion, id_gestion_academica, id_trimestre, id_materia);

CREATE INDEX IF NOT EXISTS idx_autoevaluacion_trimestral_periodo
    ON autoevaluacion_trimestral (id_institucion, id_gestion_academica, id_trimestre, id_materia);
