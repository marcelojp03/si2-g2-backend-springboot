-- =========================================================
-- SPRINT 2 - ASISTENCIA
-- Sistema de Gestión Académica SaaS
-- PostgreSQL
-- Schema: sia
-- =========================================================

SET search_path TO sia, public;

-- =========================================================
-- LIMPIEZA PARA ENTORNO LOCAL / DESARROLLO
-- =========================================================
-- OJO: Esto elimina las tablas de asistencia si ya existen.
-- Usar solo mientras todavía no hay datos reales de asistencia.

DROP TABLE IF EXISTS asistencia_detalle CASCADE;
DROP TABLE IF EXISTS asistencia_registro CASCADE;

--asistencia
CREATE TABLE asistencia_registro (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_asignacion_docente UUID NOT NULL REFERENCES asignacion_docente(id),
    registrado_por UUID NULL REFERENCES usuario(id) ON DELETE SET NULL,

    fecha DATE NOT NULL,

    estado VARCHAR(15) NOT NULL DEFAULT 'REGISTRADA'
        CHECK (estado IN ('REGISTRADA', 'MODIFICADA', 'ANULADA')),

    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_asistencia_registro_asignacion_fecha
        UNIQUE (id_asignacion_docente, fecha),

    CONSTRAINT ck_asistencia_registro_fecha_no_futura
        CHECK (fecha <= CURRENT_DATE)
);

--detalle de asistencia

CREATE TABLE asistencia_detalle (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    id_asistencia_registro UUID NOT NULL REFERENCES asistencia_registro(id) ON DELETE CASCADE,
    id_inscripcion UUID NOT NULL REFERENCES inscripcion(id),

    estado_asistencia VARCHAR(15) NOT NULL
        CHECK (estado_asistencia IN ('PRESENTE', 'AUSENTE', 'TARDANZA', 'JUSTIFICADO')),

    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_asistencia_detalle_registro_inscripcion
        UNIQUE (id_asistencia_registro, id_inscripcion)
);

-- indexes

CREATE INDEX idx_asistencia_registro_institucion_fecha
    ON asistencia_registro (id_institucion, fecha DESC);

CREATE INDEX idx_asistencia_registro_asignacion
    ON asistencia_registro (id_asignacion_docente);

CREATE INDEX idx_asistencia_registro_registrado_por
    ON asistencia_registro (registrado_por);

CREATE INDEX idx_asistencia_detalle_registro
    ON asistencia_detalle (id_asistencia_registro);

CREATE INDEX idx_asistencia_detalle_inscripcion
    ON asistencia_detalle (id_inscripcion);

CREATE INDEX idx_asistencia_detalle_estado
    ON asistencia_detalle (estado_asistencia);

--triggers

CREATE TRIGGER trg_asistencia_registro_actualizado_en
    BEFORE UPDATE ON asistencia_registro
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();

CREATE TRIGGER trg_asistencia_detalle_actualizado_en
    BEFORE UPDATE ON asistencia_detalle
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();