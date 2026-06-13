-- ============================================================
-- SPRINT 3 / SPRINT FINAL — Comunicados (HU-S3-21)
-- ============================================================

CREATE TABLE IF NOT EXISTS sia.comunicado (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES sia.institucion(id) ON DELETE CASCADE,
    titulo VARCHAR(200) NOT NULL,
    contenido TEXT NOT NULL,
    tipo VARCHAR(30) NOT NULL DEFAULT 'AVISO' CHECK (tipo IN ('AVISO', 'CIRCULAR', 'EVENTO', 'URGENTE')),
    destinatarios VARCHAR(50) NOT NULL DEFAULT 'TODOS' CHECK (destinatarios IN ('TODOS', 'DOCENTES', 'ESTUDIANTES', 'TUTORES', 'ADMINISTRATIVOS')),
    estado VARCHAR(15) NOT NULL DEFAULT 'BORRADOR' CHECK (estado IN ('BORRADOR', 'PUBLICADO', 'ARCHIVADO')),
    publicado_en TIMESTAMPTZ,
    publicado_por UUID REFERENCES sia.usuario(id) ON DELETE SET NULL,
    creado_por UUID NOT NULL REFERENCES sia.usuario(id) ON DELETE CASCADE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_comunicado_id_institucion UNIQUE (id, id_institucion)
);

CREATE INDEX IF NOT EXISTS idx_comunicado_institucion_estado
    ON sia.comunicado (id_institucion, estado);

CREATE INDEX IF NOT EXISTS idx_comunicado_institucion_tipo
    ON sia.comunicado (id_institucion, tipo);

CREATE INDEX IF NOT EXISTS idx_comunicado_publicado
    ON sia.comunicado (id_institucion, estado, publicado_en DESC)
    WHERE estado = 'PUBLICADO';
