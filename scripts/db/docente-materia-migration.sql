-- Especialidades del docente vinculadas a materias institucionales (N:M)
SET search_path TO sia, public;

CREATE TABLE IF NOT EXISTS docente_materia (
    id_docente UUID NOT NULL REFERENCES docente(id) ON DELETE CASCADE,
    id_materia UUID NOT NULL REFERENCES materia(id) ON DELETE CASCADE,
    PRIMARY KEY (id_docente, id_materia)
);

CREATE INDEX IF NOT EXISTS idx_docente_materia_materia ON docente_materia (id_materia);
