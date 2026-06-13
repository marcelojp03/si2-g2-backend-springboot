-- ============================================================
-- SPRINT FINAL — Notificaciones (HU-S3-22)
-- ============================================================

CREATE TABLE IF NOT EXISTS sia.notificacion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES sia.institucion(id) ON DELETE CASCADE,
    id_usuario UUID NOT NULL REFERENCES sia.usuario(id) ON DELETE CASCADE,
    titulo VARCHAR(200) NOT NULL,
    mensaje TEXT,
    tipo VARCHAR(30) NOT NULL DEFAULT 'SISTEMA' CHECK (tipo IN ('SISTEMA', 'COMUNICADO', 'ASISTENCIA', 'CALIFICACION', 'ALERTA')),
    referencia_tipo VARCHAR(50),
    referencia_id UUID,
    leida BOOLEAN NOT NULL DEFAULT FALSE,
    leida_en TIMESTAMPTZ,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_notificacion_id_institucion UNIQUE (id, id_institucion)
);

CREATE INDEX IF NOT EXISTS idx_notificacion_usuario_leida
    ON sia.notificacion (id_usuario, leida)
    WHERE leida = FALSE;

CREATE INDEX IF NOT EXISTS idx_notificacion_institucion
    ON sia.notificacion (id_institucion, creado_en DESC);

CREATE TABLE IF NOT EXISTS sia.sesion_dispositivo (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_usuario UUID NOT NULL REFERENCES sia.usuario(id) ON DELETE CASCADE,
    id_institucion UUID NOT NULL REFERENCES sia.institucion(id) ON DELETE CASCADE,
    token_dispositivo VARCHAR(512) NOT NULL,
    plataforma VARCHAR(20) NOT NULL CHECK (plataforma IN ('ANDROID', 'IOS')),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_sesion_dispositivo_token UNIQUE (id_usuario, token_dispositivo)
);
