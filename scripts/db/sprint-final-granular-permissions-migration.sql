-- Migración: permisos granulares (CREATE/UPDATE/DELETE/READ)
-- Reemplaza los permisos coarse _WRITE por granulares por acción
-- El PermissionInitializer sincronizará esto automáticamente al iniciar,
-- pero este script asegura consistencia para DB existentes.

-- Eliminar permisos coarse legacy (si ya no serán usados)
DELETE FROM rol_permiso WHERE id_permiso IN (
    SELECT id FROM permiso WHERE codigo IN (
        'USUARIOS_WRITE', 'CONFIGURACION_WRITE', 'GESTION_WRITE',
        'PERSONAS_WRITE', 'OPERACION_WRITE', 'ROLES_WRITE',
        'ASISTENCIA_WRITE', 'CALIFICACIONES_WRITE'
    )
);
DELETE FROM permiso WHERE codigo IN (
    'USUARIOS_WRITE', 'CONFIGURACION_WRITE', 'GESTION_WRITE',
    'PERSONAS_WRITE', 'OPERACION_WRITE', 'ROLES_WRITE',
    'ASISTENCIA_WRITE', 'CALIFICACIONES_WRITE'
);

-- Insertar nuevos permisos granulares
INSERT INTO permiso (codigo, nombre, modulo, accion, descripcion) VALUES
    -- USUARIOS
    ('USUARIOS_CREATE', 'Usuarios: crear', 'USUARIOS', 'CREATE', 'Permite crear usuarios'),
    ('USUARIOS_UPDATE', 'Usuarios: editar', 'USUARIOS', 'UPDATE', 'Permite editar usuarios existentes'),
    ('USUARIOS_DELETE', 'Usuarios: eliminar', 'USUARIOS', 'DELETE', 'Permite desactivar usuarios'),

    -- CONFIGURACION
    ('CONFIGURACION_CREATE', 'Configuración: crear', 'CONFIGURACION', 'CREATE', 'Permite crear configuraciones institucionales'),
    ('CONFIGURACION_UPDATE', 'Configuración: editar', 'CONFIGURACION', 'UPDATE', 'Permite modificar configuraciones institucionales'),
    ('CONFIGURACION_DELETE', 'Configuración: eliminar', 'CONFIGURACION', 'DELETE', 'Permite eliminar configuraciones institucionales'),

    -- GESTION
    ('GESTION_CREATE', 'Gestión académica: crear', 'GESTION_ACADEMICA', 'CREATE', 'Permite crear estructura académica'),
    ('GESTION_UPDATE', 'Gestión académica: editar', 'GESTION_ACADEMICA', 'UPDATE', 'Permite modificar estructura académica'),
    ('GESTION_DELETE', 'Gestión académica: eliminar', 'GESTION_ACADEMICA', 'DELETE', 'Permite eliminar estructura académica'),

    -- PERSONAS
    ('PERSONAS_CREATE', 'Personas: crear', 'PERSONAS', 'CREATE', 'Permite crear docentes, estudiantes y tutores'),
    ('PERSONAS_UPDATE', 'Personas: editar', 'PERSONAS', 'UPDATE', 'Permite modificar docentes, estudiantes y tutores'),
    ('PERSONAS_DELETE', 'Personas: eliminar', 'PERSONAS', 'DELETE', 'Permite eliminar docentes, estudiantes y tutores'),

    -- OPERACION
    ('OPERACION_CREATE', 'Operación: crear', 'OPERACION', 'CREATE', 'Permite crear inscripciones y asignaciones'),
    ('OPERACION_UPDATE', 'Operación: editar', 'OPERACION', 'UPDATE', 'Permite modificar inscripciones y asignaciones'),
    ('OPERACION_DELETE', 'Operación: eliminar', 'OPERACION', 'DELETE', 'Permite eliminar inscripciones y asignaciones'),

    -- ROLES
    ('ROLES_CREATE', 'Roles: crear', 'ROLES', 'CREATE', 'Permite crear roles institucionales'),
    ('ROLES_UPDATE', 'Roles: editar', 'ROLES', 'UPDATE', 'Permite editar roles institucionales'),
    ('ROLES_DELETE', 'Roles: eliminar', 'ROLES', 'DELETE', 'Permite eliminar roles institucionales'),

    -- ASISTENCIA
    ('ASISTENCIA_CREATE', 'Asistencia: crear', 'ASISTENCIA', 'CREATE', 'Permite crear registros de asistencia'),
    ('ASISTENCIA_UPDATE', 'Asistencia: editar', 'ASISTENCIA', 'UPDATE', 'Permite modificar registros de asistencia'),
    ('ASISTENCIA_DELETE', 'Asistencia: eliminar', 'ASISTENCIA', 'DELETE', 'Permite eliminar registros de asistencia'),

    -- CALIFICACIONES
    ('CALIFICACIONES_CREATE', 'Calificaciones: crear', 'CALIFICACIONES', 'CREATE', 'Permite crear evaluaciones y calificaciones'),
    ('CALIFICACIONES_UPDATE', 'Calificaciones: editar', 'CALIFICACIONES', 'UPDATE', 'Permite modificar evaluaciones y calificaciones'),
    ('CALIFICACIONES_DELETE', 'Calificaciones: eliminar', 'CALIFICACIONES', 'DELETE', 'Permite eliminar evaluaciones y calificaciones'),

    -- REPORTES
    ('REPORTES_CREATE', 'Reportes: crear', 'REPORTES', 'CREATE', 'Permite crear reportes institucionales'),
    ('REPORTES_UPDATE', 'Reportes: editar', 'REPORTES', 'UPDATE', 'Permite modificar reportes institucionales'),
    ('REPORTES_DELETE', 'Reportes: eliminar', 'REPORTES', 'DELETE', 'Permite eliminar reportes institucionales')
ON CONFLICT (codigo) DO NOTHING;

-- Reasignar ADMIN_INSTITUCION con todos los permisos granulares
INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id, p.id FROM rol r
JOIN permiso p ON p.codigo IN (
    'USUARIOS_CREATE','USUARIOS_UPDATE','USUARIOS_DELETE','USUARIOS_READ',
    'CONFIGURACION_CREATE','CONFIGURACION_UPDATE','CONFIGURACION_DELETE','CONFIGURACION_READ',
    'GESTION_CREATE','GESTION_UPDATE','GESTION_DELETE','GESTION_READ',
    'PERSONAS_CREATE','PERSONAS_UPDATE','PERSONAS_DELETE','PERSONAS_READ',
    'OPERACION_CREATE','OPERACION_UPDATE','OPERACION_DELETE','OPERACION_READ',
    'ROLES_CREATE','ROLES_UPDATE','ROLES_DELETE','ROLES_READ',
    'MI_AREA_READ','AUDITORIA_READ',
    'ASISTENCIA_CREATE','ASISTENCIA_UPDATE','ASISTENCIA_DELETE','ASISTENCIA_READ','ASISTENCIA_READ_ALL','ASISTENCIA_BACKDATE',
    'CALIFICACIONES_CREATE','CALIFICACIONES_UPDATE','CALIFICACIONES_DELETE','CALIFICACIONES_READ','CALIFICACIONES_READ_ALL','CALIFICACIONES_OVERRIDE_CIERRE',
    'REPORTES_CREATE','REPORTES_UPDATE','REPORTES_DELETE','REPORTES_READ','REPORTES_EXPORT','REPORTES_WRITE')
WHERE r.codigo = 'ADMIN_INSTITUCION'
ON CONFLICT (id_rol, id_permiso) DO NOTHING;

-- Reasignar DIRECTOR
INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id, p.id FROM rol r
JOIN permiso p ON p.codigo IN (
    'USUARIOS_CREATE','USUARIOS_UPDATE','USUARIOS_DELETE','USUARIOS_READ',
    'CONFIGURACION_CREATE','CONFIGURACION_UPDATE','CONFIGURACION_DELETE','CONFIGURACION_READ',
    'GESTION_CREATE','GESTION_UPDATE','GESTION_DELETE','GESTION_READ',
    'PERSONAS_CREATE','PERSONAS_UPDATE','PERSONAS_DELETE','PERSONAS_READ',
    'OPERACION_CREATE','OPERACION_UPDATE','OPERACION_DELETE','OPERACION_READ',
    'ROLES_READ','MI_AREA_READ','AUDITORIA_READ',
    'ASISTENCIA_CREATE','ASISTENCIA_UPDATE','ASISTENCIA_DELETE','ASISTENCIA_READ','ASISTENCIA_READ_ALL','ASISTENCIA_BACKDATE',
    'CALIFICACIONES_CREATE','CALIFICACIONES_UPDATE','CALIFICACIONES_DELETE','CALIFICACIONES_READ','CALIFICACIONES_READ_ALL','CALIFICACIONES_OVERRIDE_CIERRE',
    'REPORTES_CREATE','REPORTES_UPDATE','REPORTES_DELETE','REPORTES_READ','REPORTES_EXPORT','REPORTES_WRITE')
WHERE r.codigo = 'DIRECTOR'
ON CONFLICT (id_rol, id_permiso) DO NOTHING;

-- Reasignar SECRETARIO
INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id, p.id FROM rol r
JOIN permiso p ON p.codigo IN (
    'USUARIOS_READ',
    'GESTION_CREATE','GESTION_UPDATE','GESTION_DELETE','GESTION_READ',
    'PERSONAS_CREATE','PERSONAS_UPDATE','PERSONAS_DELETE','PERSONAS_READ',
    'OPERACION_CREATE','OPERACION_UPDATE','OPERACION_DELETE','OPERACION_READ',
    'ASISTENCIA_READ','ASISTENCIA_READ_ALL',
    'CALIFICACIONES_READ','CALIFICACIONES_READ_ALL',
    'REPORTES_READ','REPORTES_EXPORT')
WHERE r.codigo = 'SECRETARIO'
ON CONFLICT (id_rol, id_permiso) DO NOTHING;

-- Reasignar DOCENTE
INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id, p.id FROM rol r
JOIN permiso p ON p.codigo IN (
    'OPERACION_READ','MI_AREA_READ',
    'ASISTENCIA_READ',
    'CALIFICACIONES_CREATE','CALIFICACIONES_UPDATE','CALIFICACIONES_READ',
    'REPORTES_READ')
WHERE r.codigo = 'DOCENTE'
ON CONFLICT (id_rol, id_permiso) DO NOTHING;
