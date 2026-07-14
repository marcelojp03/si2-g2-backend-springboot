-- HU-NUEVA-19: factores explicables y seguimiento de alertas academicas.
ALTER TABLE sia.alerta_riesgo
    ADD COLUMN IF NOT EXISTS porcentaje_asistencia NUMERIC(5,2),
    ADD COLUMN IF NOT EXISTS promedio_calificaciones NUMERIC(5,2),
    ADD COLUMN IF NOT EXISTS tendencia_notas VARCHAR(20),
    ADD COLUMN IF NOT EXISTS evaluaciones_pendientes INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS materias_reprobadas_historial INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS factores_json TEXT,
    ADD COLUMN IF NOT EXISTS estado_alerta VARCHAR(20) NOT NULL DEFAULT 'ABIERTA';

ALTER TABLE sia.alerta_riesgo
    DROP CONSTRAINT IF EXISTS chk_alerta_riesgo_tendencia;

ALTER TABLE sia.alerta_riesgo
    ADD CONSTRAINT chk_alerta_riesgo_tendencia
    CHECK (tendencia_notas IS NULL OR tendencia_notas IN ('SUBIENDO', 'ESTABLE', 'BAJANDO', 'SIN_DATOS'));

ALTER TABLE sia.alerta_riesgo
    DROP CONSTRAINT IF EXISTS chk_alerta_riesgo_estado;

ALTER TABLE sia.alerta_riesgo
    ADD CONSTRAINT chk_alerta_riesgo_estado
    CHECK (estado_alerta IN ('ABIERTA', 'EN_SEGUIMIENTO', 'ATENDIDA', 'CERRADA'));

CREATE INDEX IF NOT EXISTS idx_alerta_riesgo_gestion_estado
    ON sia.alerta_riesgo (id_institucion, id_gestion_academica, estado_alerta, activa);
