-- =========================================================
-- SPRINT 2 — CORRECCIONES DEL ESQUEMA
-- Aplicar sobre un entorno que YA ejecutó:
--   db-script.sql + sprint-aulas + sprint2-rbac + sprint2-asistencia
--   + sprint2-calificaciones + sprint2-security-audit + sprint2-horarios
-- Idempotente: se puede ejecutar varias veces sin error.
-- =========================================================

SET search_path TO sia, public;

-- =========================================================
-- 1. HORARIO_CLASE — prevenir solapamientos
-- =========================================================
-- Bloquea dos clases activas en la misma aula, mismo día, con franja
-- horaria solapada. También bloquea que un mismo docente tenga dos
-- clases simultáneas (cruzando contra asignacion_docente).

CREATE OR REPLACE FUNCTION sia.fn_validar_solapamiento_horario()
RETURNS TRIGGER AS $$
DECLARE
    v_id_docente UUID;
BEGIN
    IF NEW.estado <> 'ACTIVO' THEN
        RETURN NEW;
    END IF;

    -- Solapamiento por aula
    IF EXISTS (
        SELECT 1 FROM sia.horario_clase h
        WHERE h.id <> NEW.id
          AND h.id_institucion = NEW.id_institucion
          AND h.id_aula = NEW.id_aula
          AND h.dia_semana = NEW.dia_semana
          AND h.estado = 'ACTIVO'
          AND h.hora_inicio < NEW.hora_fin
          AND h.hora_fin    > NEW.hora_inicio
    ) THEN
        RAISE EXCEPTION 'Solapamiento de horario en el aula seleccionada (mismo día y franja horaria)';
    END IF;

    -- Solapamiento por docente (a través de asignacion_docente)
    SELECT a.id_docente INTO v_id_docente
    FROM sia.asignacion_docente a
    WHERE a.id = NEW.id_asignacion_docente;

    IF v_id_docente IS NOT NULL AND EXISTS (
        SELECT 1 FROM sia.horario_clase h
        JOIN sia.asignacion_docente a2 ON a2.id = h.id_asignacion_docente
        WHERE h.id <> NEW.id
          AND h.id_institucion = NEW.id_institucion
          AND a2.id_docente   = v_id_docente
          AND h.dia_semana    = NEW.dia_semana
          AND h.estado        = 'ACTIVO'
          AND h.hora_inicio   < NEW.hora_fin
          AND h.hora_fin      > NEW.hora_inicio
    ) THEN
        RAISE EXCEPTION 'El docente ya tiene asignada otra clase en el mismo día y franja horaria';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_validar_solapamiento_horario ON sia.horario_clase;
CREATE TRIGGER trg_validar_solapamiento_horario
    BEFORE INSERT OR UPDATE ON sia.horario_clase
    FOR EACH ROW EXECUTE FUNCTION sia.fn_validar_solapamiento_horario();

-- =========================================================
-- 2. HORARIO_CLASE — FK compuesta hacia asignacion_docente
-- =========================================================
-- Garantiza coherencia multi-tenant: la asignación debe pertenecer
-- a la misma institución que el horario.

-- Necesitamos primero un UNIQUE (id, id_institucion) sobre asignacion_docente
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_asignacion_id_institucion'
    ) THEN
        ALTER TABLE sia.asignacion_docente
            ADD CONSTRAINT uq_asignacion_id_institucion UNIQUE (id, id_institucion);
    END IF;
END $$;

-- Reemplazar la FK simple por la compuesta
ALTER TABLE sia.horario_clase DROP CONSTRAINT IF EXISTS fk_horario_asignacion_docente;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_horario_asignacion_institucion'
    ) THEN
        ALTER TABLE sia.horario_clase
            ADD CONSTRAINT fk_horario_asignacion_institucion
            FOREIGN KEY (id_asignacion_docente, id_institucion)
            REFERENCES sia.asignacion_docente (id, id_institucion);
    END IF;
END $$;

-- =========================================================
-- 3. ASISTENCIA_REGISTRO — quitar bloqueo de fecha futura
-- =========================================================
-- Permite pre-cargar planillas (caso de uso válido al iniciar el periodo).
-- La validación de fecha razonable se mantiene en la capa de servicio.

ALTER TABLE sia.asistencia_registro
    DROP CONSTRAINT IF EXISTS ck_asistencia_registro_fecha_no_futura;

-- =========================================================
-- 4. CALIFICACION_CAMBIO — índice por id_usuario
-- =========================================================
-- Habilita auditoría eficiente "qué cambió este usuario".

CREATE INDEX IF NOT EXISTS idx_calificacion_cambio_usuario
    ON sia.calificacion_cambio (id_usuario, fecha_cambio DESC);

-- =========================================================
-- 5. ASISTENCIA_DETALLE — id_institucion explícito (opcional)
-- =========================================================
-- Añade la columna y la backfilléa desde el registro padre.
-- Mejora performance en reportes multi-tenant y reduce JOINs.

ALTER TABLE sia.asistencia_detalle
    ADD COLUMN IF NOT EXISTS id_institucion UUID NULL;

UPDATE sia.asistencia_detalle d
SET id_institucion = r.id_institucion
FROM sia.asistencia_registro r
WHERE d.id_asistencia_registro = r.id
  AND d.id_institucion IS NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM sia.asistencia_detalle WHERE id_institucion IS NULL LIMIT 1
    ) THEN
        RAISE NOTICE 'Quedan filas en asistencia_detalle sin id_institucion (verificar antes de aplicar NOT NULL).';
    ELSE
        BEGIN
            ALTER TABLE sia.asistencia_detalle ALTER COLUMN id_institucion SET NOT NULL;
        EXCEPTION WHEN others THEN
            RAISE NOTICE 'No se pudo aplicar NOT NULL a asistencia_detalle.id_institucion: %', SQLERRM;
        END;

        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint WHERE conname = 'fk_asistencia_detalle_institucion'
        ) THEN
            ALTER TABLE sia.asistencia_detalle
                ADD CONSTRAINT fk_asistencia_detalle_institucion
                FOREIGN KEY (id_institucion) REFERENCES sia.institucion(id) ON DELETE CASCADE;
        END IF;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_asistencia_detalle_institucion
    ON sia.asistencia_detalle (id_institucion);

-- =========================================================
-- FIN
-- =========================================================
