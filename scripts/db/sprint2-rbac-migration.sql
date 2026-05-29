SET search_path TO sia, public;

ALTER TABLE rol
    ADD COLUMN IF NOT EXISTS id_institucion UUID NULL REFERENCES institucion(id) ON DELETE CASCADE;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_rol_nombre_institucion'
    ) THEN
        ALTER TABLE rol
            ADD CONSTRAINT uq_rol_nombre_institucion UNIQUE NULLS NOT DISTINCT (id_institucion, nombre);
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS permiso (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo VARCHAR(60) NOT NULL UNIQUE,
    nombre VARCHAR(120) NOT NULL,
    modulo VARCHAR(60) NOT NULL,
    accion VARCHAR(30) NOT NULL,
    descripcion VARCHAR(255),
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS rol_permiso (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_rol UUID NOT NULL REFERENCES rol(id) ON DELETE CASCADE,
    id_permiso UUID NOT NULL REFERENCES permiso(id) ON DELETE CASCADE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_rol_permiso UNIQUE (id_rol, id_permiso)
);

UPDATE rol
SET es_global = TRUE,
    id_institucion = NULL
WHERE codigo IN ('ADMIN_INSTITUCION', 'DIRECTOR', 'SECRETARIO', 'DOCENTE', 'ESTUDIANTE', 'TUTOR');

INSERT INTO permiso (codigo, nombre, modulo, accion, descripcion)
VALUES
('USUARIOS_READ', 'Usuarios: lectura', 'USUARIOS', 'READ', 'Permite consultar usuarios'),
('USUARIOS_WRITE', 'Usuarios: escritura', 'USUARIOS', 'WRITE', 'Permite crear, editar y desactivar usuarios'),
('CONFIGURACION_READ', 'Configuración: lectura', 'CONFIGURACION', 'READ', 'Permite consultar configuración institucional'),
('CONFIGURACION_WRITE', 'Configuración: escritura', 'CONFIGURACION', 'WRITE', 'Permite modificar configuración institucional'),
('GESTION_READ', 'Gestión académica: lectura', 'GESTION_ACADEMICA', 'READ', 'Permite consultar estructura académica'),
('GESTION_WRITE', 'Gestión académica: escritura', 'GESTION_ACADEMICA', 'WRITE', 'Permite modificar estructura académica'),
('PERSONAS_READ', 'Personas: lectura', 'PERSONAS', 'READ', 'Permite consultar docentes, estudiantes y tutores'),
('PERSONAS_WRITE', 'Personas: escritura', 'PERSONAS', 'WRITE', 'Permite modificar docentes, estudiantes y tutores'),
('OPERACION_READ', 'Operación: lectura', 'OPERACION', 'READ', 'Permite consultar inscripciones y asignaciones'),
('OPERACION_WRITE', 'Operación: escritura', 'OPERACION', 'WRITE', 'Permite modificar inscripciones y asignaciones'),
('ROLES_READ', 'Roles: lectura', 'ROLES', 'READ', 'Permite consultar roles y permisos'),
('ROLES_WRITE', 'Roles: escritura', 'ROLES', 'WRITE', 'Permite crear y editar roles institucionales'),
('MI_AREA_READ', 'Mi área: lectura', 'MI_AREA', 'READ', 'Permite acceder al área operativa del docente')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id, p.id
FROM rol r
JOIN permiso p ON p.codigo IN (
    'USUARIOS_READ','USUARIOS_WRITE','CONFIGURACION_READ','CONFIGURACION_WRITE',
    'GESTION_READ','GESTION_WRITE','PERSONAS_READ','PERSONAS_WRITE',
    'OPERACION_READ','OPERACION_WRITE','ROLES_READ','ROLES_WRITE','MI_AREA_READ'
)
WHERE r.codigo = 'ADMIN_INSTITUCION'
ON CONFLICT (id_rol, id_permiso) DO NOTHING;

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id, p.id
FROM rol r
JOIN permiso p ON p.codigo IN (
    'USUARIOS_READ','USUARIOS_WRITE','CONFIGURACION_READ','GESTION_READ','GESTION_WRITE',
    'PERSONAS_READ','PERSONAS_WRITE','OPERACION_READ','OPERACION_WRITE',
    'ROLES_READ','MI_AREA_READ','AUDITORIA_READ',
    'ASISTENCIA_READ','ASISTENCIA_WRITE','ASISTENCIA_READ_ALL','ASISTENCIA_BACKDATE',
    'CALIFICACIONES_READ','CALIFICACIONES_WRITE','CALIFICACIONES_READ_ALL','CALIFICACIONES_OVERRIDE_CIERRE'
)
WHERE r.codigo = 'DIRECTOR'
ON CONFLICT (id_rol, id_permiso) DO NOTHING;

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id, p.id
FROM rol r
JOIN permiso p ON p.codigo IN (
    'USUARIOS_READ','GESTION_READ','GESTION_WRITE',
    'PERSONAS_READ','PERSONAS_WRITE','OPERACION_READ','OPERACION_WRITE'
)
WHERE r.codigo = 'SECRETARIO'
ON CONFLICT (id_rol, id_permiso) DO NOTHING;

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id, p.id
FROM rol r
JOIN permiso p ON p.codigo IN ('OPERACION_READ','MI_AREA_READ')
WHERE r.codigo = 'DOCENTE'
ON CONFLICT (id_rol, id_permiso) DO NOTHING;
