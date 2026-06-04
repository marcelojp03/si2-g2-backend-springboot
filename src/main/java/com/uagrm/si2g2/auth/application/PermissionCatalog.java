package com.uagrm.si2g2.auth.application;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

public final class PermissionCatalog {

    private PermissionCatalog() {}

    public static final String USUARIOS_CREATE = "USUARIOS_CREATE";
    public static final String USUARIOS_UPDATE = "USUARIOS_UPDATE";
    public static final String USUARIOS_DELETE = "USUARIOS_DELETE";
    public static final String USUARIOS_READ = "USUARIOS_READ";

    public static final String CONFIGURACION_CREATE = "CONFIGURACION_CREATE";
    public static final String CONFIGURACION_UPDATE = "CONFIGURACION_UPDATE";
    public static final String CONFIGURACION_DELETE = "CONFIGURACION_DELETE";
    public static final String CONFIGURACION_READ = "CONFIGURACION_READ";

    public static final String GESTIONES_CREATE = "GESTIONES_CREATE";
    public static final String GESTIONES_UPDATE = "GESTIONES_UPDATE";
    public static final String GESTIONES_DELETE = "GESTIONES_DELETE";
    public static final String GESTIONES_READ = "GESTIONES_READ";

    public static final String CURSOS_CREATE = "CURSOS_CREATE";
    public static final String CURSOS_UPDATE = "CURSOS_UPDATE";
    public static final String CURSOS_DELETE = "CURSOS_DELETE";
    public static final String CURSOS_READ = "CURSOS_READ";

    public static final String PARALELOS_CREATE = "PARALELOS_CREATE";
    public static final String PARALELOS_UPDATE = "PARALELOS_UPDATE";
    public static final String PARALELOS_DELETE = "PARALELOS_DELETE";
    public static final String PARALELOS_READ = "PARALELOS_READ";

    public static final String MATERIAS_CREATE = "MATERIAS_CREATE";
    public static final String MATERIAS_UPDATE = "MATERIAS_UPDATE";
    public static final String MATERIAS_DELETE = "MATERIAS_DELETE";
    public static final String MATERIAS_READ = "MATERIAS_READ";

    public static final String AULAS_CREATE = "AULAS_CREATE";
    public static final String AULAS_UPDATE = "AULAS_UPDATE";
    public static final String AULAS_DELETE = "AULAS_DELETE";
    public static final String AULAS_READ = "AULAS_READ";

    public static final String HORARIOS_CREATE = "HORARIOS_CREATE";
    public static final String HORARIOS_UPDATE = "HORARIOS_UPDATE";
    public static final String HORARIOS_DELETE = "HORARIOS_DELETE";
    public static final String HORARIOS_READ = "HORARIOS_READ";

    public static final String DOCENTES_CREATE = "DOCENTES_CREATE";
    public static final String DOCENTES_UPDATE = "DOCENTES_UPDATE";
    public static final String DOCENTES_DELETE = "DOCENTES_DELETE";
    public static final String DOCENTES_READ = "DOCENTES_READ";

    public static final String ESTUDIANTES_CREATE = "ESTUDIANTES_CREATE";
    public static final String ESTUDIANTES_UPDATE = "ESTUDIANTES_UPDATE";
    public static final String ESTUDIANTES_DELETE = "ESTUDIANTES_DELETE";
    public static final String ESTUDIANTES_READ = "ESTUDIANTES_READ";

    public static final String TUTORES_CREATE = "TUTORES_CREATE";
    public static final String TUTORES_UPDATE = "TUTORES_UPDATE";
    public static final String TUTORES_DELETE = "TUTORES_DELETE";
    public static final String TUTORES_READ = "TUTORES_READ";

    public static final String INSCRIPCIONES_CREATE = "INSCRIPCIONES_CREATE";
    public static final String INSCRIPCIONES_UPDATE = "INSCRIPCIONES_UPDATE";
    public static final String INSCRIPCIONES_DELETE = "INSCRIPCIONES_DELETE";
    public static final String INSCRIPCIONES_READ = "INSCRIPCIONES_READ";

    public static final String ASIGNACIONES_CREATE = "ASIGNACIONES_CREATE";
    public static final String ASIGNACIONES_UPDATE = "ASIGNACIONES_UPDATE";
    public static final String ASIGNACIONES_DELETE = "ASIGNACIONES_DELETE";
    public static final String ASIGNACIONES_READ = "ASIGNACIONES_READ";

    public static final String ROLES_CREATE = "ROLES_CREATE";
    public static final String ROLES_UPDATE = "ROLES_UPDATE";
    public static final String ROLES_DELETE = "ROLES_DELETE";
    public static final String ROLES_READ = "ROLES_READ";

    public static final String MI_AREA_READ = "MI_AREA_READ";
    public static final String AUDITORIA_READ = "AUDITORIA_READ";

    public static final String ASISTENCIA_CREATE = "ASISTENCIA_CREATE";
    public static final String ASISTENCIA_UPDATE = "ASISTENCIA_UPDATE";
    public static final String ASISTENCIA_DELETE = "ASISTENCIA_DELETE";
    public static final String ASISTENCIA_READ = "ASISTENCIA_READ";
    public static final String ASISTENCIA_READ_ALL = "ASISTENCIA_READ_ALL";
    public static final String ASISTENCIA_BACKDATE = "ASISTENCIA_BACKDATE";

    public static final String CALIFICACIONES_CREATE = "CALIFICACIONES_CREATE";
    public static final String CALIFICACIONES_UPDATE = "CALIFICACIONES_UPDATE";
    public static final String CALIFICACIONES_DELETE = "CALIFICACIONES_DELETE";
    public static final String CALIFICACIONES_READ = "CALIFICACIONES_READ";
    public static final String CALIFICACIONES_READ_ALL = "CALIFICACIONES_READ_ALL";
    public static final String CALIFICACIONES_OVERRIDE_CIERRE = "CALIFICACIONES_OVERRIDE_CIERRE";

    public static final String REPORTES_CREATE = "REPORTES_CREATE";
    public static final String REPORTES_UPDATE = "REPORTES_UPDATE";
    public static final String REPORTES_DELETE = "REPORTES_DELETE";
    public static final String REPORTES_READ = "REPORTES_READ";
    public static final String REPORTES_EXPORT = "REPORTES_EXPORT";
    public static final String REPORTES_WRITE = "REPORTES_WRITE";

    public static final Set<String> ADMIN_INSTITUCION = Set.of(
            USUARIOS_CREATE, USUARIOS_UPDATE, USUARIOS_DELETE, USUARIOS_READ,
            CONFIGURACION_CREATE, CONFIGURACION_UPDATE, CONFIGURACION_DELETE, CONFIGURACION_READ,
            GESTIONES_CREATE, GESTIONES_UPDATE, GESTIONES_DELETE, GESTIONES_READ,
            CURSOS_CREATE, CURSOS_UPDATE, CURSOS_DELETE, CURSOS_READ,
            PARALELOS_CREATE, PARALELOS_UPDATE, PARALELOS_DELETE, PARALELOS_READ,
            MATERIAS_CREATE, MATERIAS_UPDATE, MATERIAS_DELETE, MATERIAS_READ,
            AULAS_CREATE, AULAS_UPDATE, AULAS_DELETE, AULAS_READ,
            HORARIOS_CREATE, HORARIOS_UPDATE, HORARIOS_DELETE, HORARIOS_READ,
            DOCENTES_CREATE, DOCENTES_UPDATE, DOCENTES_DELETE, DOCENTES_READ,
            ESTUDIANTES_CREATE, ESTUDIANTES_UPDATE, ESTUDIANTES_DELETE, ESTUDIANTES_READ,
            TUTORES_CREATE, TUTORES_UPDATE, TUTORES_DELETE, TUTORES_READ,
            INSCRIPCIONES_CREATE, INSCRIPCIONES_UPDATE, INSCRIPCIONES_DELETE, INSCRIPCIONES_READ,
            ASIGNACIONES_CREATE, ASIGNACIONES_UPDATE, ASIGNACIONES_DELETE, ASIGNACIONES_READ,
            ROLES_CREATE, ROLES_UPDATE, ROLES_DELETE, ROLES_READ,
            MI_AREA_READ,
            AUDITORIA_READ,
            ASISTENCIA_CREATE, ASISTENCIA_UPDATE, ASISTENCIA_DELETE, ASISTENCIA_READ, ASISTENCIA_READ_ALL, ASISTENCIA_BACKDATE,
            CALIFICACIONES_CREATE, CALIFICACIONES_UPDATE, CALIFICACIONES_DELETE, CALIFICACIONES_READ, CALIFICACIONES_READ_ALL, CALIFICACIONES_OVERRIDE_CIERRE,
            REPORTES_CREATE, REPORTES_UPDATE, REPORTES_DELETE, REPORTES_READ, REPORTES_EXPORT, REPORTES_WRITE
    );

    public static final Set<String> DIRECTOR = Set.of(
            USUARIOS_CREATE, USUARIOS_UPDATE, USUARIOS_DELETE, USUARIOS_READ,
            CONFIGURACION_CREATE, CONFIGURACION_UPDATE, CONFIGURACION_DELETE, CONFIGURACION_READ,
            GESTIONES_CREATE, GESTIONES_UPDATE, GESTIONES_DELETE, GESTIONES_READ,
            CURSOS_CREATE, CURSOS_UPDATE, CURSOS_DELETE, CURSOS_READ,
            PARALELOS_CREATE, PARALELOS_UPDATE, PARALELOS_DELETE, PARALELOS_READ,
            MATERIAS_CREATE, MATERIAS_UPDATE, MATERIAS_DELETE, MATERIAS_READ,
            AULAS_CREATE, AULAS_UPDATE, AULAS_DELETE, AULAS_READ,
            HORARIOS_CREATE, HORARIOS_UPDATE, HORARIOS_DELETE, HORARIOS_READ,
            DOCENTES_CREATE, DOCENTES_UPDATE, DOCENTES_DELETE, DOCENTES_READ,
            ESTUDIANTES_CREATE, ESTUDIANTES_UPDATE, ESTUDIANTES_DELETE, ESTUDIANTES_READ,
            TUTORES_CREATE, TUTORES_UPDATE, TUTORES_DELETE, TUTORES_READ,
            INSCRIPCIONES_CREATE, INSCRIPCIONES_UPDATE, INSCRIPCIONES_DELETE, INSCRIPCIONES_READ,
            ASIGNACIONES_CREATE, ASIGNACIONES_UPDATE, ASIGNACIONES_DELETE, ASIGNACIONES_READ,
            ROLES_READ,
            MI_AREA_READ,
            AUDITORIA_READ,
            ASISTENCIA_CREATE, ASISTENCIA_UPDATE, ASISTENCIA_DELETE, ASISTENCIA_READ, ASISTENCIA_READ_ALL, ASISTENCIA_BACKDATE,
            CALIFICACIONES_CREATE, CALIFICACIONES_UPDATE, CALIFICACIONES_DELETE, CALIFICACIONES_READ, CALIFICACIONES_READ_ALL, CALIFICACIONES_OVERRIDE_CIERRE,
            REPORTES_CREATE, REPORTES_UPDATE, REPORTES_DELETE, REPORTES_READ, REPORTES_EXPORT, REPORTES_WRITE
    );

    public static final Set<String> SECRETARIO = Set.of(
            USUARIOS_READ,
            GESTIONES_READ,
            CURSOS_READ, PARALELOS_READ, MATERIAS_READ, AULAS_READ,
            DOCENTES_READ, ESTUDIANTES_READ, TUTORES_READ,
            INSCRIPCIONES_CREATE, INSCRIPCIONES_UPDATE, INSCRIPCIONES_DELETE, INSCRIPCIONES_READ,
            ASIGNACIONES_READ,
            ASISTENCIA_READ, ASISTENCIA_READ_ALL,
            CALIFICACIONES_READ, CALIFICACIONES_READ_ALL,
            REPORTES_READ, REPORTES_EXPORT
    );

    public static final Set<String> DOCENTE = Set.of(
            MI_AREA_READ,
            ASISTENCIA_READ,
            CURSOS_READ, PARALELOS_READ, MATERIAS_READ,
            INSCRIPCIONES_READ, ASIGNACIONES_READ,
            CALIFICACIONES_CREATE, CALIFICACIONES_UPDATE, CALIFICACIONES_READ,
            REPORTES_READ
    );

    public static final Set<String> ESTUDIANTE = Set.of();
    public static final Set<String> TUTOR = Set.of();

    public static final List<Definition> DEFINITIONS = List.of(
            new Definition(USUARIOS_CREATE, "Usuarios: crear", "USUARIOS", "CREATE", "Permite crear usuarios"),
            new Definition(USUARIOS_UPDATE, "Usuarios: editar", "USUARIOS", "UPDATE", "Permite editar usuarios existentes"),
            new Definition(USUARIOS_DELETE, "Usuarios: eliminar", "USUARIOS", "DELETE", "Permite desactivar usuarios"),
            new Definition(USUARIOS_READ, "Usuarios: ver", "USUARIOS", "READ", "Permite consultar usuarios"),

            new Definition(CONFIGURACION_CREATE, "Configuración: crear", "CONFIGURACION", "CREATE", "Permite crear configuraciones institucionales"),
            new Definition(CONFIGURACION_UPDATE, "Configuración: editar", "CONFIGURACION", "UPDATE", "Permite modificar configuraciones institucionales"),
            new Definition(CONFIGURACION_DELETE, "Configuración: eliminar", "CONFIGURACION", "DELETE", "Permite eliminar configuraciones institucionales"),
            new Definition(CONFIGURACION_READ, "Configuración: ver", "CONFIGURACION", "READ", "Permite consultar configuración institucional"),

            new Definition(GESTIONES_CREATE, "Gestiones: crear", "GESTIONES", "CREATE", "Permite crear gestiones académicas"),
            new Definition(GESTIONES_UPDATE, "Gestiones: editar", "GESTIONES", "UPDATE", "Permite modificar gestiones académicas"),
            new Definition(GESTIONES_DELETE, "Gestiones: eliminar", "GESTIONES", "DELETE", "Permite eliminar gestiones académicas"),
            new Definition(GESTIONES_READ, "Gestiones: ver", "GESTIONES", "READ", "Permite consultar gestiones académicas"),

            new Definition(CURSOS_CREATE, "Cursos: crear", "CURSOS", "CREATE", "Permite crear cursos"),
            new Definition(CURSOS_UPDATE, "Cursos: editar", "CURSOS", "UPDATE", "Permite modificar cursos"),
            new Definition(CURSOS_DELETE, "Cursos: eliminar", "CURSOS", "DELETE", "Permite eliminar cursos"),
            new Definition(CURSOS_READ, "Cursos: ver", "CURSOS", "READ", "Permite consultar cursos"),

            new Definition(PARALELOS_CREATE, "Paralelos: crear", "PARALELOS", "CREATE", "Permite crear paralelos"),
            new Definition(PARALELOS_UPDATE, "Paralelos: editar", "PARALELOS", "UPDATE", "Permite modificar paralelos"),
            new Definition(PARALELOS_DELETE, "Paralelos: eliminar", "PARALELOS", "DELETE", "Permite eliminar paralelos"),
            new Definition(PARALELOS_READ, "Paralelos: ver", "PARALELOS", "READ", "Permite consultar paralelos"),

            new Definition(MATERIAS_CREATE, "Materias: crear", "MATERIAS", "CREATE", "Permite crear materias"),
            new Definition(MATERIAS_UPDATE, "Materias: editar", "MATERIAS", "UPDATE", "Permite modificar materias"),
            new Definition(MATERIAS_DELETE, "Materias: eliminar", "MATERIAS", "DELETE", "Permite eliminar materias"),
            new Definition(MATERIAS_READ, "Materias: ver", "MATERIAS", "READ", "Permite consultar materias"),

            new Definition(AULAS_CREATE, "Aulas: crear", "AULAS", "CREATE", "Permite crear aulas"),
            new Definition(AULAS_UPDATE, "Aulas: editar", "AULAS", "UPDATE", "Permite modificar aulas"),
            new Definition(AULAS_DELETE, "Aulas: eliminar", "AULAS", "DELETE", "Permite eliminar aulas"),
            new Definition(AULAS_READ, "Aulas: ver", "AULAS", "READ", "Permite consultar aulas"),

            new Definition(HORARIOS_CREATE, "Horarios: crear", "HORARIOS", "CREATE", "Permite crear horarios de clase"),
            new Definition(HORARIOS_UPDATE, "Horarios: editar", "HORARIOS", "UPDATE", "Permite modificar horarios de clase"),
            new Definition(HORARIOS_DELETE, "Horarios: eliminar", "HORARIOS", "DELETE", "Permite eliminar horarios de clase"),
            new Definition(HORARIOS_READ, "Horarios: ver", "HORARIOS", "READ", "Permite consultar horarios de clase"),

            new Definition(DOCENTES_CREATE, "Docentes: crear", "DOCENTES", "CREATE", "Permite crear docentes"),
            new Definition(DOCENTES_UPDATE, "Docentes: editar", "DOCENTES", "UPDATE", "Permite modificar docentes"),
            new Definition(DOCENTES_DELETE, "Docentes: eliminar", "DOCENTES", "DELETE", "Permite eliminar docentes"),
            new Definition(DOCENTES_READ, "Docentes: ver", "DOCENTES", "READ", "Permite consultar docentes"),

            new Definition(ESTUDIANTES_CREATE, "Estudiantes: crear", "ESTUDIANTES", "CREATE", "Permite crear estudiantes"),
            new Definition(ESTUDIANTES_UPDATE, "Estudiantes: editar", "ESTUDIANTES", "UPDATE", "Permite modificar estudiantes"),
            new Definition(ESTUDIANTES_DELETE, "Estudiantes: eliminar", "ESTUDIANTES", "DELETE", "Permite eliminar estudiantes"),
            new Definition(ESTUDIANTES_READ, "Estudiantes: ver", "ESTUDIANTES", "READ", "Permite consultar estudiantes"),

            new Definition(TUTORES_CREATE, "Tutores: crear", "TUTORES", "CREATE", "Permite crear tutores"),
            new Definition(TUTORES_UPDATE, "Tutores: editar", "TUTORES", "UPDATE", "Permite modificar tutores"),
            new Definition(TUTORES_DELETE, "Tutores: eliminar", "TUTORES", "DELETE", "Permite eliminar tutores"),
            new Definition(TUTORES_READ, "Tutores: ver", "TUTORES", "READ", "Permite consultar tutores"),

            new Definition(INSCRIPCIONES_CREATE, "Inscripciones: crear", "INSCRIPCIONES", "CREATE", "Permite crear inscripciones"),
            new Definition(INSCRIPCIONES_UPDATE, "Inscripciones: editar", "INSCRIPCIONES", "UPDATE", "Permite modificar inscripciones"),
            new Definition(INSCRIPCIONES_DELETE, "Inscripciones: eliminar", "INSCRIPCIONES", "DELETE", "Permite eliminar inscripciones"),
            new Definition(INSCRIPCIONES_READ, "Inscripciones: ver", "INSCRIPCIONES", "READ", "Permite consultar inscripciones"),

            new Definition(ASIGNACIONES_CREATE, "Asignaciones: crear", "ASIGNACIONES", "CREATE", "Permite crear asignaciones docentes"),
            new Definition(ASIGNACIONES_UPDATE, "Asignaciones: editar", "ASIGNACIONES", "UPDATE", "Permite modificar asignaciones docentes"),
            new Definition(ASIGNACIONES_DELETE, "Asignaciones: eliminar", "ASIGNACIONES", "DELETE", "Permite eliminar asignaciones docentes"),
            new Definition(ASIGNACIONES_READ, "Asignaciones: ver", "ASIGNACIONES", "READ", "Permite consultar asignaciones docentes"),

            new Definition(ROLES_CREATE, "Roles: crear", "ROLES", "CREATE", "Permite crear roles institucionales"),
            new Definition(ROLES_UPDATE, "Roles: editar", "ROLES", "UPDATE", "Permite editar roles institucionales"),
            new Definition(ROLES_DELETE, "Roles: eliminar", "ROLES", "DELETE", "Permite eliminar roles institucionales"),
            new Definition(ROLES_READ, "Roles: ver", "ROLES", "READ", "Permite consultar roles y permisos"),

            new Definition(MI_AREA_READ, "Mi área: ver", "MI_AREA", "READ", "Permite acceder al área operativa del docente"),
            new Definition(AUDITORIA_READ, "Auditoría: ver", "AUDITORIA", "READ", "Permite consultar la bitácora de auditoría"),

            new Definition(ASISTENCIA_CREATE, "Asistencia: crear", "ASISTENCIA", "CREATE", "Permite crear registros de asistencia"),
            new Definition(ASISTENCIA_UPDATE, "Asistencia: editar", "ASISTENCIA", "UPDATE", "Permite modificar registros de asistencia"),
            new Definition(ASISTENCIA_DELETE, "Asistencia: eliminar", "ASISTENCIA", "DELETE", "Permite eliminar registros de asistencia"),
            new Definition(ASISTENCIA_READ, "Asistencia: ver", "ASISTENCIA", "READ", "Permite consultar registros y plantillas de asistencia"),
            new Definition(ASISTENCIA_READ_ALL, "Asistencia: ver institucional", "ASISTENCIA", "READ_ALL", "Permite consultar asistencias de todos los docentes de la institución"),
            new Definition(ASISTENCIA_BACKDATE, "Asistencia: fechas pasadas", "ASISTENCIA", "BACKDATE", "Permite registrar o modificar asistencia de fechas pasadas"),

            new Definition(CALIFICACIONES_CREATE, "Calificaciones: crear", "CALIFICACIONES", "CREATE", "Permite crear evaluaciones y calificaciones"),
            new Definition(CALIFICACIONES_UPDATE, "Calificaciones: editar", "CALIFICACIONES", "UPDATE", "Permite modificar evaluaciones y calificaciones"),
            new Definition(CALIFICACIONES_DELETE, "Calificaciones: eliminar", "CALIFICACIONES", "DELETE", "Permite eliminar evaluaciones y calificaciones"),
            new Definition(CALIFICACIONES_READ, "Calificaciones: ver", "CALIFICACIONES", "READ", "Permite consultar evaluaciones y calificaciones"),
            new Definition(CALIFICACIONES_READ_ALL, "Calificaciones: ver institucional", "CALIFICACIONES", "READ_ALL", "Permite consultar calificaciones de todos los docentes de la institución"),
            new Definition(CALIFICACIONES_OVERRIDE_CIERRE, "Calificaciones: cierre", "CALIFICACIONES", "OVERRIDE_CIERRE", "Permite modificar calificaciones en evaluaciones cerradas"),

            new Definition(REPORTES_CREATE, "Reportes: crear", "REPORTES", "CREATE", "Permite crear reportes institucionales"),
            new Definition(REPORTES_UPDATE, "Reportes: editar", "REPORTES", "UPDATE", "Permite modificar reportes institucionales"),
            new Definition(REPORTES_DELETE, "Reportes: eliminar", "REPORTES", "DELETE", "Permite eliminar reportes institucionales"),
            new Definition(REPORTES_READ, "Reportes: ver", "REPORTES", "READ", "Permite consultar reportes institucionales"),
            new Definition(REPORTES_EXPORT, "Reportes: exportar", "REPORTES", "EXPORT", "Permite exportar reportes institucionales"),
            new Definition(REPORTES_WRITE, "Reportes: configurar", "REPORTES", "WRITE", "Permite guardar filtros y configuraciones de reportes")
    );

    @Getter
    @RequiredArgsConstructor
    public static class Definition {
        private final String codigo;
        private final String nombre;
        private final String modulo;
        private final String accion;
        private final String descripcion;
    }
}
