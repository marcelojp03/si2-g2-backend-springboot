-- HU-NUEVA-19: unicidad y limpieza de alertas activas.

ALTER TABLE sia.alerta_riesgo
    ADD COLUMN IF NOT EXISTS datos_vigentes BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS ultima_evaluacion_valida_en TIMESTAMPTZ;

UPDATE sia.alerta_riesgo
SET datos_vigentes = TRUE
WHERE datos_vigentes IS NULL;

ALTER TABLE sia.alerta_riesgo
    ALTER COLUMN datos_vigentes SET DEFAULT TRUE,
    ALTER COLUMN datos_vigentes SET NOT NULL;

-- Conserva la alerta activa mas reciente y cierra duplicados historicos antes
-- de crear el indice parcial.
UPDATE sia.alerta_riesgo
SET activa = FALSE,
    actualizado_en = NOW()
WHERE activa = TRUE
  AND estado_alerta = 'CERRADA';

WITH duplicadas AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY id_institucion, id_estudiante, id_gestion_academica
               ORDER BY procesado_en DESC, creado_en DESC, id
           ) AS posicion
    FROM sia.alerta_riesgo
    WHERE activa = TRUE
)
UPDATE sia.alerta_riesgo alerta
SET activa = FALSE,
    estado_alerta = 'CERRADA',
    actualizado_en = NOW()
FROM duplicadas
WHERE alerta.id = duplicadas.id
  AND duplicadas.posicion > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uq_alerta_riesgo_activa_estudiante_gestion
    ON sia.alerta_riesgo (id_institucion, id_estudiante, id_gestion_academica)
    WHERE activa = TRUE;

CREATE TABLE IF NOT EXISTS sia.alerta_riesgo_seguimiento (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_alerta_riesgo UUID NOT NULL,
    id_institucion UUID NOT NULL,
    estado_anterior VARCHAR(20) NOT NULL,
    estado_nuevo VARCHAR(20) NOT NULL,
    observacion TEXT,
    id_usuario UUID,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_alerta_seguimiento_alerta_tenant
        FOREIGN KEY (id_alerta_riesgo, id_institucion)
        REFERENCES sia.alerta_riesgo (id, id_institucion) ON DELETE CASCADE,
    CONSTRAINT fk_alerta_seguimiento_institucion
        FOREIGN KEY (id_institucion) REFERENCES sia.institucion (id),
    CONSTRAINT fk_alerta_seguimiento_usuario
        FOREIGN KEY (id_usuario) REFERENCES sia.usuario (id),
    CONSTRAINT chk_alerta_seguimiento_estado_anterior
        CHECK (estado_anterior IN ('ABIERTA', 'EN_SEGUIMIENTO', 'ATENDIDA', 'CERRADA')),
    CONSTRAINT chk_alerta_seguimiento_estado_nuevo
        CHECK (estado_nuevo IN ('ABIERTA', 'EN_SEGUIMIENTO', 'ATENDIDA', 'CERRADA')),
    CONSTRAINT chk_alerta_seguimiento_transicion
        CHECK (estado_anterior <> estado_nuevo)
);

CREATE INDEX IF NOT EXISTS idx_alerta_seguimiento_historial
    ON sia.alerta_riesgo_seguimiento (id_institucion, id_alerta_riesgo, creado_en, id);
CREATE INDEX IF NOT EXISTS idx_alerta_seguimiento_usuario
    ON sia.alerta_riesgo_seguimiento (id_usuario)
    WHERE id_usuario IS NOT NULL;
