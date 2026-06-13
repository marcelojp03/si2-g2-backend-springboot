-- ============================================================
-- CLEANUP: Elimina datos de negocio duplicados/inconsistentes
-- Mantiene: instituciones, roles, usuarios (con roles), planes,
--           suscripciones, configuraciones institucionales
-- ============================================================
-- Ejecutar SOLO contra la DB objetivo, NO en producción sin verificar
-- ============================================================

BEGIN;

-- 1. Eliminar datos de calificaciones (hojas)
DELETE FROM sia.calificacion_cambio;
DELETE FROM sia.observacion_ser;
DELETE FROM sia.autoevaluacion_trimestral;
DELETE FROM sia.calificacion_ser;
DELETE FROM sia.calificacion_actividad;
DELETE FROM sia.actividad_evaluativa;
DELETE FROM sia.evaluacion_materia;
DELETE FROM sia.evaluacion;
DELETE FROM sia.calificacion;
DELETE FROM sia.periodo_evaluacion;

-- 2. Eliminar asistencias
DELETE FROM sia.asistencia_detalle;
DELETE FROM sia.asistencia_registro;

-- 3. Eliminar horarios
DELETE FROM sia.horario_clase;

-- 4. Eliminar inscripciones y asignaciones
DELETE FROM sia.inscripcion;
DELETE FROM sia.estudiante_tutor;
DELETE FROM sia.asignacion_docente;

-- 5. Eliminar estudiantes, tutores, docentes (y usuarios ligados que no sean admin)
DELETE FROM sia.estudiante;
DELETE FROM sia.tutor;
DELETE FROM sia.docente;

-- 6. Eliminar estructura académica
DELETE FROM sia.curso_materia;
DELETE FROM sia.paralelo;
DELETE FROM sia.curso;
DELETE FROM sia.aula;
DELETE FROM sia.materia;

-- 7. Eliminar gestiones académicas (se recrean)
DELETE FROM sia.gestion_academica;

-- 8. Limpiar usuarios de estudiantes/docentes (no los admins)
--    Se borran usuarios cuyo único rol es ESTUDIANTE, DOCENTE o TUTOR
--    y los que NO tienen rol SUPER_ADMIN, ADMIN_INSTITUCION, DIRECTOR, SECRETARIO
DELETE FROM sia.usuario_rol ur
WHERE ur.id_usuario IN (
    SELECT u.id FROM sia.usuario u
    WHERE u.id NOT IN (
        SELECT ur2.id_usuario FROM sia.usuario_rol ur2
        JOIN sia.rol r ON r.id = ur2.id_rol
        WHERE r.codigo IN ('SUPER_ADMIN', 'ADMIN_INSTITUCION', 'DIRECTOR', 'SECRETARIO')
    )
);

DELETE FROM sia.usuario u
WHERE u.id NOT IN (
    SELECT ur2.id_usuario FROM sia.usuario_rol ur2
    JOIN sia.rol r ON r.id = ur2.id_rol
    WHERE r.codigo IN ('SUPER_ADMIN', 'ADMIN_INSTITUCION', 'DIRECTOR', 'SECRETARIO')
);

-- 9. Limpiar configuraciones institucionales (se recrean)
DELETE FROM sia.configuracion_institucion;

COMMIT;

-- ============================================================
-- VERIFICACIÓN POST-LIMPIEZA
-- ============================================================
SELECT 'instituciones' tabla, count(*) FROM sia.institucion
UNION ALL SELECT 'roles', count(*) FROM sia.rol
UNION ALL SELECT 'usuarios', count(*) FROM sia.usuario
UNION ALL SELECT 'usuario_rol', count(*) FROM sia.usuario_rol
UNION ALL SELECT 'planes', count(*) FROM sia.plan_suscripcion
UNION ALL SELECT 'suscripciones', count(*) FROM sia.suscripcion_institucion
UNION ALL SELECT 'cursos (esperado 0)', count(*) FROM sia.curso
UNION ALL SELECT 'materias (esperado 0)', count(*) FROM sia.materia
UNION ALL SELECT 'estudiantes (esperado 0)', count(*) FROM sia.estudiante
UNION ALL SELECT 'docentes (esperado 0)', count(*) FROM sia.docente
ORDER BY tabla;
