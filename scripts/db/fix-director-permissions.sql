-- Sincroniza permisos del rol DIRECTOR con PermissionCatalog (gestión institucional).
-- Ejecutar si un director no puede crear/editar datos académicos.
-- Alternativa: reiniciar el backend (PermissionInitializer hace lo mismo al arrancar).

SET search_path TO sia, public;

INSERT INTO permiso (codigo, nombre, modulo, accion, descripcion)
VALUES
('AUDITORIA_READ', 'Auditoría: lectura', 'AUDITORIA', 'READ', 'Permite consultar la bitácora de auditoría'),
('ASISTENCIA_READ', 'Asistencia: lectura', 'ASISTENCIA', 'READ', 'Permite consultar registros y plantillas de asistencia'),
('ASISTENCIA_WRITE', 'Asistencia: escritura', 'ASISTENCIA', 'WRITE', 'Permite registrar y modificar asistencia institucional'),
('ASISTENCIA_READ_ALL', 'Asistencia: lectura institucional', 'ASISTENCIA', 'READ_ALL', 'Permite consultar asistencias de todos los docentes de la institución'),
('ASISTENCIA_BACKDATE', 'Asistencia: fechas pasadas', 'ASISTENCIA', 'BACKDATE', 'Permite registrar o modificar asistencia de fechas pasadas'),
('CALIFICACIONES_READ', 'Calificaciones: lectura', 'CALIFICACIONES', 'READ', 'Permite consultar evaluaciones y calificaciones'),
('CALIFICACIONES_WRITE', 'Calificaciones: escritura', 'CALIFICACIONES', 'WRITE', 'Permite registrar y modificar evaluaciones y calificaciones'),
('CALIFICACIONES_READ_ALL', 'Calificaciones: lectura institucional', 'CALIFICACIONES', 'READ_ALL', 'Permite consultar calificaciones de todos los docentes de la institucion'),
('CALIFICACIONES_OVERRIDE_CIERRE', 'Calificaciones: cierre', 'CALIFICACIONES', 'OVERRIDE_CIERRE', 'Permite modificar calificaciones en evaluaciones cerradas')
ON CONFLICT (codigo) DO NOTHING;

DELETE FROM rol_permiso
WHERE id_rol = (SELECT id FROM rol WHERE codigo = 'DIRECTOR');

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id, p.id
FROM rol r
JOIN permiso p ON p.codigo IN (
    'USUARIOS_READ', 'USUARIOS_WRITE',
    'CONFIGURACION_READ', 'CONFIGURACION_WRITE',
    'GESTION_READ', 'GESTION_WRITE',
    'PERSONAS_READ', 'PERSONAS_WRITE',
    'OPERACION_READ', 'OPERACION_WRITE',
    'ROLES_READ',
    'MI_AREA_READ',
    'AUDITORIA_READ',
    'ASISTENCIA_READ', 'ASISTENCIA_WRITE', 'ASISTENCIA_READ_ALL', 'ASISTENCIA_BACKDATE',
    'CALIFICACIONES_READ', 'CALIFICACIONES_WRITE', 'CALIFICACIONES_READ_ALL', 'CALIFICACIONES_OVERRIDE_CIERRE'
)
WHERE r.codigo = 'DIRECTOR'
ON CONFLICT (id_rol, id_permiso) DO NOTHING;
