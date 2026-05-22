package com.uagrm.si2g2.auth.application;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

public final class PermissionCatalog {

    private PermissionCatalog() {}

    public static final String USUARIOS_READ = "USUARIOS_READ";
    public static final String USUARIOS_WRITE = "USUARIOS_WRITE";
    public static final String CONFIGURACION_READ = "CONFIGURACION_READ";
    public static final String CONFIGURACION_WRITE = "CONFIGURACION_WRITE";
    public static final String GESTION_READ = "GESTION_READ";
    public static final String GESTION_WRITE = "GESTION_WRITE";
    public static final String PERSONAS_READ = "PERSONAS_READ";
    public static final String PERSONAS_WRITE = "PERSONAS_WRITE";
    public static final String OPERACION_READ = "OPERACION_READ";
    public static final String OPERACION_WRITE = "OPERACION_WRITE";
    public static final String ROLES_READ = "ROLES_READ";
    public static final String ROLES_WRITE = "ROLES_WRITE";
    public static final String MI_AREA_READ = "MI_AREA_READ";
    public static final String AUDITORIA_READ = "AUDITORIA_READ";

    public static final String ASISTENCIA_READ = "ASISTENCIA_READ";
    public static final String ASISTENCIA_WRITE = "ASISTENCIA_WRITE";
    public static final String ASISTENCIA_READ_ALL = "ASISTENCIA_READ_ALL";
    public static final String ASISTENCIA_BACKDATE = "ASISTENCIA_BACKDATE";
    public static final String CALIFICACIONES_READ = "CALIFICACIONES_READ";
    public static final String CALIFICACIONES_WRITE = "CALIFICACIONES_WRITE";
    public static final String CALIFICACIONES_READ_ALL = "CALIFICACIONES_READ_ALL";
    public static final String CALIFICACIONES_OVERRIDE_CIERRE = "CALIFICACIONES_OVERRIDE_CIERRE";

    public static final Set<String> ADMIN_INSTITUCION = Set.of(
            USUARIOS_READ, USUARIOS_WRITE,
            CONFIGURACION_READ, CONFIGURACION_WRITE,
            GESTION_READ, GESTION_WRITE,
            PERSONAS_READ, PERSONAS_WRITE,
            OPERACION_READ, OPERACION_WRITE,
            ROLES_READ, ROLES_WRITE,
            MI_AREA_READ,
            AUDITORIA_READ,
            ASISTENCIA_READ, ASISTENCIA_WRITE, ASISTENCIA_READ_ALL, ASISTENCIA_BACKDATE,
            CALIFICACIONES_READ, CALIFICACIONES_WRITE, CALIFICACIONES_READ_ALL, CALIFICACIONES_OVERRIDE_CIERRE
    );

    public static final Set<String> DIRECTOR = Set.of(
            USUARIOS_READ,
            CONFIGURACION_READ,
            GESTION_READ, GESTION_WRITE,
            PERSONAS_READ, PERSONAS_WRITE,
            OPERACION_READ, OPERACION_WRITE,
            ROLES_READ,
            MI_AREA_READ,
            AUDITORIA_READ,
            ASISTENCIA_READ, ASISTENCIA_READ_ALL,
            CALIFICACIONES_READ, CALIFICACIONES_READ_ALL
    );

    public static final Set<String> SECRETARIO = Set.of(
            USUARIOS_READ,
            GESTION_READ, GESTION_WRITE,
            PERSONAS_READ, PERSONAS_WRITE,
            OPERACION_READ, OPERACION_WRITE,
            ASISTENCIA_READ, ASISTENCIA_READ_ALL,
            CALIFICACIONES_READ, CALIFICACIONES_READ_ALL
    );

    public static final Set<String> DOCENTE = Set.of(
            OPERACION_READ,
            MI_AREA_READ,
            ASISTENCIA_READ,
            CALIFICACIONES_READ, CALIFICACIONES_WRITE
    );

    public static final Set<String> ESTUDIANTE = Set.of();
    public static final Set<String> TUTOR = Set.of();

    public static final List<Definition> DEFINITIONS = List.of(
            new Definition(USUARIOS_READ, "Usuarios: lectura", "USUARIOS", "READ", "Permite consultar usuarios"),
            new Definition(USUARIOS_WRITE, "Usuarios: escritura", "USUARIOS", "WRITE", "Permite crear, editar y desactivar usuarios"),
            new Definition(CONFIGURACION_READ, "Configuración: lectura", "CONFIGURACION", "READ", "Permite consultar configuración institucional"),
            new Definition(CONFIGURACION_WRITE, "Configuración: escritura", "CONFIGURACION", "WRITE", "Permite modificar configuración institucional"),
            new Definition(GESTION_READ, "Gestión académica: lectura", "GESTION_ACADEMICA", "READ", "Permite consultar estructura académica"),
            new Definition(GESTION_WRITE, "Gestión académica: escritura", "GESTION_ACADEMICA", "WRITE", "Permite modificar estructura académica"),
            new Definition(PERSONAS_READ, "Personas: lectura", "PERSONAS", "READ", "Permite consultar docentes, estudiantes y tutores"),
            new Definition(PERSONAS_WRITE, "Personas: escritura", "PERSONAS", "WRITE", "Permite modificar docentes, estudiantes y tutores"),
            new Definition(OPERACION_READ, "Operación: lectura", "OPERACION", "READ", "Permite consultar inscripciones y asignaciones"),
            new Definition(OPERACION_WRITE, "Operación: escritura", "OPERACION", "WRITE", "Permite modificar inscripciones y asignaciones"),
            new Definition(ROLES_READ, "Roles: lectura", "ROLES", "READ", "Permite consultar roles y permisos"),
            new Definition(ROLES_WRITE, "Roles: escritura", "ROLES", "WRITE", "Permite crear y editar roles institucionales"),
            new Definition(MI_AREA_READ, "Mi área: lectura", "MI_AREA", "READ", "Permite acceder al área operativa del docente"),
            new Definition(AUDITORIA_READ, "Auditoría: lectura", "AUDITORIA", "READ", "Permite consultar la bitácora de auditoría"),

            new Definition(ASISTENCIA_READ, "Asistencia: lectura", "ASISTENCIA", "READ", "Permite consultar registros y plantillas de asistencia"),
            new Definition(ASISTENCIA_WRITE, "Asistencia: escritura", "ASISTENCIA", "WRITE", "Permite registrar y modificar asistencia institucional"),
            new Definition(ASISTENCIA_READ_ALL, "Asistencia: lectura institucional", "ASISTENCIA", "READ_ALL", "Permite consultar asistencias de todos los docentes de la institución"),
            new Definition(ASISTENCIA_BACKDATE, "Asistencia: fechas pasadas", "ASISTENCIA", "BACKDATE", "Permite registrar o modificar asistencia de fechas pasadas"),
            new Definition(CALIFICACIONES_READ, "Calificaciones: lectura", "CALIFICACIONES", "READ", "Permite consultar evaluaciones y calificaciones"),
            new Definition(CALIFICACIONES_WRITE, "Calificaciones: escritura", "CALIFICACIONES", "WRITE", "Permite registrar y modificar evaluaciones y calificaciones"),
            new Definition(CALIFICACIONES_READ_ALL, "Calificaciones: lectura institucional", "CALIFICACIONES", "READ_ALL", "Permite consultar calificaciones de todos los docentes de la institucion"),
            new Definition(CALIFICACIONES_OVERRIDE_CIERRE, "Calificaciones: cierre", "CALIFICACIONES", "OVERRIDE_CIERRE", "Permite modificar calificaciones en evaluaciones cerradas")
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
