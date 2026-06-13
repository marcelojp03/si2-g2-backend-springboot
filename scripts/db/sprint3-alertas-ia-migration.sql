-- ============================================================
-- SPRINT FINAL — Alertas IA (HU-S3-26, HU-S3-27)
-- ============================================================

CREATE TABLE IF NOT EXISTS sia.alerta_riesgo (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES sia.institucion(id) ON DELETE CASCADE,
    id_estudiante UUID NOT NULL REFERENCES sia.estudiante(id) ON DELETE CASCADE,
    id_gestion_academica UUID NOT NULL REFERENCES sia.gestion_academica(id) ON DELETE CASCADE,
    nivel_riesgo VARCHAR(20) NOT NULL CHECK (nivel_riesgo IN ('BAJO', 'MEDIO', 'ALTO', 'CRITICO')),
    motivo TEXT,
    score_ia NUMERIC(5,4),
    procesado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_alerta_riesgo_id_institucion UNIQUE (id, id_institucion)
);

CREATE INDEX IF NOT EXISTS idx_alerta_riesgo_estudiante
    ON sia.alerta_riesgo (id_estudiante, activa);

CREATE INDEX IF NOT EXISTS idx_alerta_riesgo_institucion_nivel
    ON sia.alerta_riesgo (id_institucion, nivel_riesgo, activa);

CREATE TABLE IF NOT EXISTS sia.recomendacion_ia (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_alerta_riesgo UUID NOT NULL REFERENCES sia.alerta_riesgo(id) ON DELETE CASCADE,
    descripcion TEXT NOT NULL,
    tipo_accion VARCHAR(50),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_recomendacion_ia_id UNIQUE (id)
);
