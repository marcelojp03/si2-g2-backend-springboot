-- =========================================================
-- SPRINT 2 - EVALUACIONES POR MATERIA (HU-S2-18)
-- Estructura alineada con entidad Evaluacion (id_materia)
-- Schema: sia
-- =========================================================

SET search_path TO sia, public;

CREATE TABLE IF NOT EXISTS evaluacion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_materia UUID NOT NULL REFERENCES materia(id),
    creado_por UUID NULL REFERENCES usuario(id) ON DELETE SET NULL,
    periodo INTEGER NOT NULL CHECK (periodo >= 1),
    tipo VARCHAR(40) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    ponderacion NUMERIC(5,2) NOT NULL CHECK (ponderacion > 0 AND ponderacion <= 100),
    escala VARCHAR(15) NOT NULL DEFAULT 'NUMERICA'
        CHECK (escala IN ('NUMERICA', 'LITERAL')),
    estado VARCHAR(15) NOT NULL DEFAULT 'ABIERTA'
        CHECK (estado IN ('ABIERTA', 'CERRADA', 'ANULADA')),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_evaluacion_materia_periodo_nombre
        UNIQUE (id_institucion, id_materia, periodo, nombre)
);

CREATE TABLE IF NOT EXISTS calificacion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_evaluacion UUID NOT NULL REFERENCES evaluacion(id) ON DELETE CASCADE,
    id_inscripcion UUID NOT NULL REFERENCES inscripcion(id),
    registrado_por UUID NULL REFERENCES usuario(id) ON DELETE SET NULL,
    nota_numerica NUMERIC(8,2) NULL,
    nota_literal VARCHAR(5) NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_calificacion_evaluacion_inscripcion
        UNIQUE (id_evaluacion, id_inscripcion),
    CONSTRAINT ck_calificacion_valor_unico
        CHECK (
            (nota_numerica IS NOT NULL AND nota_literal IS NULL)
            OR (nota_numerica IS NULL AND nota_literal IS NOT NULL)
        )
);

CREATE TABLE IF NOT EXISTS calificacion_cambio (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_calificacion UUID NOT NULL REFERENCES calificacion(id) ON DELETE CASCADE,
    id_usuario UUID NULL REFERENCES usuario(id) ON DELETE SET NULL,
    valor_anterior VARCHAR(30) NULL,
    valor_nuevo VARCHAR(30) NOT NULL,
    razon VARCHAR(255) NOT NULL,
    fecha_cambio TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_evaluacion_institucion_materia
    ON evaluacion (id_institucion, id_materia);

CREATE INDEX IF NOT EXISTS idx_evaluacion_institucion_materia_periodo
    ON evaluacion (id_institucion, id_materia, periodo);

CREATE INDEX IF NOT EXISTS idx_evaluacion_estado ON evaluacion (estado);

CREATE INDEX IF NOT EXISTS idx_calificacion_institucion_evaluacion
    ON calificacion (id_institucion, id_evaluacion);

CREATE INDEX IF NOT EXISTS idx_calificacion_inscripcion ON calificacion (id_inscripcion);

CREATE INDEX IF NOT EXISTS idx_calificacion_cambio_calificacion
    ON calificacion_cambio (id_calificacion, fecha_cambio DESC);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_evaluacion_actualizado_en') THEN
        CREATE TRIGGER trg_evaluacion_actualizado_en
            BEFORE UPDATE ON evaluacion
            FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_calificacion_actualizado_en') THEN
        CREATE TRIGGER trg_calificacion_actualizado_en
            BEFORE UPDATE ON calificacion
            FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
    END IF;
END $$;
