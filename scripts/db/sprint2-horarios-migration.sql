CREATE TABLE IF NOT EXISTS sia.horario_clase (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL,
    id_asignacion_docente UUID NOT NULL,
    id_aula UUID NOT NULL,
    dia_semana VARCHAR(15) NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVO',
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_horario_institucion
        FOREIGN KEY (id_institucion)
        REFERENCES sia.institucion(id),
    CONSTRAINT fk_horario_asignacion_docente
        FOREIGN KEY (id_asignacion_docente)
        REFERENCES sia.asignacion_docente(id),
    CONSTRAINT fk_horario_aula_institucion
        FOREIGN KEY (id_aula, id_institucion)
        REFERENCES sia.aula(id, id_institucion),
    CONSTRAINT ck_horario_dia_semana
        CHECK (dia_semana IN ('LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO')),
    CONSTRAINT ck_horario_estado
        CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    CONSTRAINT ck_horario_horas
        CHECK (hora_inicio < hora_fin)
);

CREATE INDEX IF NOT EXISTS idx_horario_clase_id_institucion
    ON sia.horario_clase(id_institucion);

CREATE INDEX IF NOT EXISTS idx_horario_clase_id_asignacion_docente
    ON sia.horario_clase(id_asignacion_docente);

CREATE INDEX IF NOT EXISTS idx_horario_clase_id_aula
    ON sia.horario_clase(id_aula);

CREATE INDEX IF NOT EXISTS idx_horario_clase_dia_semana
    ON sia.horario_clase(dia_semana);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_trigger
        WHERE tgname = 'trg_horario_clase_actualizado_en'
    ) THEN
        CREATE TRIGGER trg_horario_clase_actualizado_en
            BEFORE UPDATE ON sia.horario_clase
            FOR EACH ROW
            EXECUTE FUNCTION sia.fn_actualizar_actualizado_en();
    END IF;
END $$;
