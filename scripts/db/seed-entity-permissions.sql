-- Copiá y pegá esto en tu consola SQL (psql, DBeaver, etc.)
-- Crea los 44 permisos entity-level y limpia los obsoletos

BEGIN;

-- Forzar schema objetivo para entornos donde search_path apunta a public
SET LOCAL search_path TO sia, public;

-- Prechecks para evitar errores ambiguos de relaciones inexistentes
DO $$
BEGIN
    IF to_regnamespace('sia') IS NULL THEN
        RAISE EXCEPTION 'Schema sia no existe. Ejecuta primero scripts/db/db-script.sql';
    END IF;

    IF to_regclass('sia.rol_permiso') IS NULL OR to_regclass('sia.permiso') IS NULL OR to_regclass('sia.rol') IS NULL THEN
        RAISE EXCEPTION 'Faltan tablas base de seguridad (sia.rol, sia.permiso, sia.rol_permiso). Ejecuta primero scripts/db/db-script.sql';
    END IF;
END $$;

-- Limpiar permisos obsoletos de roles
DELETE FROM rol_permiso WHERE id_permiso IN (
    SELECT id FROM permiso WHERE codigo IN (
        'USUARIOS_WRITE','CONFIGURACION_WRITE','GESTION_WRITE','PERSONAS_WRITE',
        'OPERACION_WRITE','ROLES_WRITE','ASISTENCIA_WRITE','CALIFICACIONES_WRITE',
        'GESTION_CREATE','GESTION_UPDATE','GESTION_DELETE','GESTION_READ',
        'PERSONAS_CREATE','PERSONAS_UPDATE','PERSONAS_DELETE','PERSONAS_READ',
        'OPERACION_CREATE','OPERACION_UPDATE','OPERACION_DELETE','OPERACION_READ'
    )
);

-- Borrar permisos obsoletos
DELETE FROM permiso WHERE codigo IN (
    'USUARIOS_WRITE','CONFIGURACION_WRITE','GESTION_WRITE','PERSONAS_WRITE',
    'OPERACION_WRITE','ROLES_WRITE','ASISTENCIA_WRITE','CALIFICACIONES_WRITE',
    'GESTION_CREATE','GESTION_UPDATE','GESTION_DELETE','GESTION_READ',
    'PERSONAS_CREATE','PERSONAS_UPDATE','PERSONAS_DELETE','PERSONAS_READ',
    'OPERACION_CREATE','OPERACION_UPDATE','OPERACION_DELETE','OPERACION_READ'
);

-- Actualizar labels viejos
UPDATE permiso SET nombre = 'Usuarios: ver', descripcion = 'Permite consultar usuarios' WHERE codigo = 'USUARIOS_READ';
UPDATE permiso SET nombre = 'Configuración: ver', descripcion = 'Permite consultar configuración institucional' WHERE codigo = 'CONFIGURACION_READ';
UPDATE permiso SET nombre = 'Auditoría: ver', descripcion = 'Permite consultar la bitácora de auditoría' WHERE codigo = 'AUDITORIA_READ';
UPDATE permiso SET nombre = 'Mi área: ver', descripcion = 'Permite acceder al área operativa del docente' WHERE codigo = 'MI_AREA_READ';
UPDATE permiso SET nombre = 'Roles: ver', descripcion = 'Permite consultar roles y permisos' WHERE codigo = 'ROLES_READ';

-- Insertar permisos entity-level
INSERT INTO permiso (codigo, nombre, modulo, accion, descripcion) VALUES
    ('GESTIONES_CREATE', 'Gestiones: crear', 'GESTIONES', 'CREATE', 'Permite crear gestiones académicas'),
    ('GESTIONES_UPDATE', 'Gestiones: editar', 'GESTIONES', 'UPDATE', 'Permite modificar gestiones académicas'),
    ('GESTIONES_DELETE', 'Gestiones: eliminar', 'GESTIONES', 'DELETE', 'Permite eliminar gestiones académicas'),
    ('GESTIONES_READ', 'Gestiones: ver', 'GESTIONES', 'READ', 'Permite consultar gestiones académicas'),
    ('CURSOS_CREATE', 'Cursos: crear', 'CURSOS', 'CREATE', 'Permite crear cursos'),
    ('CURSOS_UPDATE', 'Cursos: editar', 'CURSOS', 'UPDATE', 'Permite modificar cursos'),
    ('CURSOS_DELETE', 'Cursos: eliminar', 'CURSOS', 'DELETE', 'Permite eliminar cursos'),
    ('CURSOS_READ', 'Cursos: ver', 'CURSOS', 'READ', 'Permite consultar cursos'),
    ('PARALELOS_CREATE', 'Paralelos: crear', 'PARALELOS', 'CREATE', 'Permite crear paralelos'),
    ('PARALELOS_UPDATE', 'Paralelos: editar', 'PARALELOS', 'UPDATE', 'Permite modificar paralelos'),
    ('PARALELOS_DELETE', 'Paralelos: eliminar', 'PARALELOS', 'DELETE', 'Permite eliminar paralelos'),
    ('PARALELOS_READ', 'Paralelos: ver', 'PARALELOS', 'READ', 'Permite consultar paralelos'),
    ('MATERIAS_CREATE', 'Materias: crear', 'MATERIAS', 'CREATE', 'Permite crear materias'),
    ('MATERIAS_UPDATE', 'Materias: editar', 'MATERIAS', 'UPDATE', 'Permite modificar materias'),
    ('MATERIAS_DELETE', 'Materias: eliminar', 'MATERIAS', 'DELETE', 'Permite eliminar materias'),
    ('MATERIAS_READ', 'Materias: ver', 'MATERIAS', 'READ', 'Permite consultar materias'),
    ('AULAS_CREATE', 'Aulas: crear', 'AULAS', 'CREATE', 'Permite crear aulas'),
    ('AULAS_UPDATE', 'Aulas: editar', 'AULAS', 'UPDATE', 'Permite modificar aulas'),
    ('AULAS_DELETE', 'Aulas: eliminar', 'AULAS', 'DELETE', 'Permite eliminar aulas'),
    ('AULAS_READ', 'Aulas: ver', 'AULAS', 'READ', 'Permite consultar aulas'),
    ('HORARIOS_CREATE', 'Horarios: crear', 'HORARIOS', 'CREATE', 'Permite crear horarios de clase'),
    ('HORARIOS_UPDATE', 'Horarios: editar', 'HORARIOS', 'UPDATE', 'Permite modificar horarios de clase'),
    ('HORARIOS_DELETE', 'Horarios: eliminar', 'HORARIOS', 'DELETE', 'Permite eliminar horarios de clase'),
    ('HORARIOS_READ', 'Horarios: ver', 'HORARIOS', 'READ', 'Permite consultar horarios de clase'),
    ('DOCENTES_CREATE', 'Docentes: crear', 'DOCENTES', 'CREATE', 'Permite crear docentes'),
    ('DOCENTES_UPDATE', 'Docentes: editar', 'DOCENTES', 'UPDATE', 'Permite modificar docentes'),
    ('DOCENTES_DELETE', 'Docentes: eliminar', 'DOCENTES', 'DELETE', 'Permite eliminar docentes'),
    ('DOCENTES_READ', 'Docentes: ver', 'DOCENTES', 'READ', 'Permite consultar docentes'),
    ('ESTUDIANTES_CREATE', 'Estudiantes: crear', 'ESTUDIANTES', 'CREATE', 'Permite crear estudiantes'),
    ('ESTUDIANTES_UPDATE', 'Estudiantes: editar', 'ESTUDIANTES', 'UPDATE', 'Permite modificar estudiantes'),
    ('ESTUDIANTES_DELETE', 'Estudiantes: eliminar', 'ESTUDIANTES', 'DELETE', 'Permite eliminar estudiantes'),
    ('ESTUDIANTES_READ', 'Estudiantes: ver', 'ESTUDIANTES', 'READ', 'Permite consultar estudiantes'),
    ('TUTORES_CREATE', 'Tutores: crear', 'TUTORES', 'CREATE', 'Permite crear tutores'),
    ('TUTORES_UPDATE', 'Tutores: editar', 'TUTORES', 'UPDATE', 'Permite modificar tutores'),
    ('TUTORES_DELETE', 'Tutores: eliminar', 'TUTORES', 'DELETE', 'Permite eliminar tutores'),
    ('TUTORES_READ', 'Tutores: ver', 'TUTORES', 'READ', 'Permite consultar tutores'),
    ('INSCRIPCIONES_CREATE', 'Inscripciones: crear', 'INSCRIPCIONES', 'CREATE', 'Permite crear inscripciones'),
    ('INSCRIPCIONES_UPDATE', 'Inscripciones: editar', 'INSCRIPCIONES', 'UPDATE', 'Permite modificar inscripciones'),
    ('INSCRIPCIONES_DELETE', 'Inscripciones: eliminar', 'INSCRIPCIONES', 'DELETE', 'Permite eliminar inscripciones'),
    ('INSCRIPCIONES_READ', 'Inscripciones: ver', 'INSCRIPCIONES', 'READ', 'Permite consultar inscripciones'),
    ('ASIGNACIONES_CREATE', 'Asignaciones: crear', 'ASIGNACIONES', 'CREATE', 'Permite crear asignaciones docentes'),
    ('ASIGNACIONES_UPDATE', 'Asignaciones: editar', 'ASIGNACIONES', 'UPDATE', 'Permite modificar asignaciones docentes'),
    ('ASIGNACIONES_DELETE', 'Asignaciones: eliminar', 'ASIGNACIONES', 'DELETE', 'Permite eliminar asignaciones docentes'),
    ('ASIGNACIONES_READ', 'Asignaciones: ver', 'ASIGNACIONES', 'READ', 'Permite consultar asignaciones docentes')
ON CONFLICT (codigo) DO NOTHING;

-- Asignar todos los permisos al ADMIN_INSTITUCION
INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id, p.id FROM rol r
CROSS JOIN permiso p
WHERE r.codigo = 'ADMIN_INSTITUCION'
  AND p.estado = 'ACTIVO'
ON CONFLICT (id_rol, id_permiso) DO NOTHING;

COMMIT;
