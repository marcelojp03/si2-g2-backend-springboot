SET search_path TO sia;

CREATE TABLE IF NOT EXISTS aula (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    codigo VARCHAR(30) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    capacidad INTEGER NOT NULL,
    ubicacion VARCHAR(180),
    recursos VARCHAR(500),
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_aula_codigo UNIQUE (id_institucion, codigo),
    CONSTRAINT uq_aula_nombre UNIQUE (id_institucion, nombre),
    CONSTRAINT uq_aula_id_institucion UNIQUE (id, id_institucion),
    CONSTRAINT ck_aula_capacidad CHECK (capacidad > 0)
);

CREATE INDEX IF NOT EXISTS idx_aula_institucion_estado ON aula (id_institucion, estado);
CREATE INDEX IF NOT EXISTS idx_aula_capacidad ON aula (id_institucion, capacidad);
