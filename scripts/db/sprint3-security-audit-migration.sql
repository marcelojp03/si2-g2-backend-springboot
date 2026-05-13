SET search_path TO sia, public;

ALTER TABLE bitacora_auditoria
    ADD COLUMN IF NOT EXISTS metodo_http VARCHAR(10),
    ADD COLUMN IF NOT EXISTS ruta_recurso VARCHAR(255),
    ADD COLUMN IF NOT EXISTS nombre_funcion VARCHAR(150),
    ADD COLUMN IF NOT EXISTS hash_integridad VARCHAR(128);

CREATE TABLE IF NOT EXISTS password_recovery_challenge (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_usuario UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    correo CITEXT NOT NULL,
    codigo_verificacion VARCHAR(6) NOT NULL,
    token_recuperacion VARCHAR(120),
    intentos_verificacion INTEGER NOT NULL DEFAULT 0,
    verificado BOOLEAN NOT NULL DEFAULT FALSE,
    usado BOOLEAN NOT NULL DEFAULT FALSE,
    expira_en TIMESTAMPTZ NOT NULL,
    verificado_en TIMESTAMPTZ NULL,
    usado_en TIMESTAMPTZ NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_password_recovery_usuario ON password_recovery_challenge (id_usuario);
CREATE INDEX IF NOT EXISTS idx_password_recovery_expira_en ON password_recovery_challenge (expira_en DESC);

INSERT INTO permiso (codigo, nombre, modulo, accion, descripcion)
VALUES ('AUDITORIA_READ', 'Auditoría: lectura', 'AUDITORIA', 'READ', 'Permite consultar la bitácora de auditoría')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id, p.id
FROM rol r
JOIN permiso p ON p.codigo = 'AUDITORIA_READ'
WHERE r.codigo IN ('ADMIN_INSTITUCION', 'DIRECTOR')
ON CONFLICT (id_rol, id_permiso) DO NOTHING;
