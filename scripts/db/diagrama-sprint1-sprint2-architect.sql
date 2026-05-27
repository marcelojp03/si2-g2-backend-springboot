-- =========================================================
-- DIAGRAMA BASE DE DATOS - SPRINT 1 Y SPRINT 2
-- Sistema de Gestion Academica SaaS
-- Uso: pegar/importar en Architect para generar el diagrama ER.
--
-- Nota:
-- Este script esta pensado para diagramacion, no para ejecutar migraciones.
-- Por eso no incluye datos iniciales, triggers, funciones ni indices.
-- =========================================================

-- =========================================================
-- SPRINT 1: INSTITUCION, SEGURIDAD Y CONFIGURACION
-- =========================================================

CREATE TABLE institucion (
    id UUID PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(200) NOT NULL,
    tipo_institucion VARCHAR(20) NOT NULL,
    telefono VARCHAR(30),
    correo VARCHAR(255),
    direccion VARCHAR(255),
    estado VARCHAR(15) NOT NULL,
    creado_en TIMESTAMP NOT NULL,
    actualizado_en TIMESTAMP NOT NULL
);

CREATE TABLE configuracion_institucion (
    id UUID PRIMARY KEY,
    id_institucion UUID NOT NULL,
    clave VARCHAR(100) NOT NULL,
    valor TEXT NOT NULL,
    tipo_valor VARCHAR(30) NOT NULL,
    descripcion VARCHAR(255),
    creado_en TIMESTAMP NOT NULL,
    actualizado_en TIMESTAMP NOT NULL,
    CONSTRAINT uq_configuracion_institucion UNIQUE (id_institucion, clave),
    CONSTRAINT fk_configuracion_institucion
        FOREIGN KEY (id_institucion) REFERENCES institucion(id)
);

CREATE TABLE rol (
    id UUID PRIMARY KEY,
    codigo VARCHAR(50) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    id_institucion UUID,
    descripcion VARCHAR(255),
    es_global BOOLEAN NOT NULL,
    estado VARCHAR(15) NOT NULL,
    creado_en TIMESTAMP NOT NULL,
    actualizado_en TIMESTAMP NOT NULL,
    CONSTRAINT fk_rol_institucion
        FOREIGN KEY (id_institucion) REFERENCES institucion(id)
);

CREATE TABLE permiso (
    id UUID PRIMARY KEY,
    codigo VARCHAR(60) NOT NULL UNIQUE,
    nombre VARCHAR(120) NOT NULL,
    modulo VARCHAR(60) NOT NULL,
    accion VARCHAR(30) NOT NULL,
    descripcion VARCHAR(255),
    estado VARCHAR(15) NOT NULL,
    creado_en TIMESTAMP NOT NULL,
    actualizado_en TIMESTAMP NOT NULL
);

CREATE TABLE rol_permiso (
    id UUID PRIMARY KEY,
    id_rol UUID NOT NULL,
    id_permiso UUID NOT NULL,
    creado_en TIMESTAMP NOT NULL,
    actualizado_en TIMESTAMP NOT NULL,
    CONSTRAINT uq_rol_permiso UNIQUE (id_rol, id_permiso),
    CONSTRAINT fk_rol_permiso_rol
        FOREIGN KEY (id_rol) REFERENCES rol(id),
    CONSTRAINT fk_rol_permiso_permiso
        FOREIGN KEY (id_permiso) REFERENCES permiso(id)
);

CREATE TABLE usuario (
    id UUID PRIMARY KEY,
    id_institucion UUID,
    correo VARCHAR(255) NOT NULL UNIQUE,
    hash_contrasena TEXT NOT NULL,
    nombres VARCHAR(120) NOT NULL,
    apellidos VARCHAR(120) NOT NULL,
    telefono VARCHAR(30),
    requiere_cambio_contrasena BOOLEAN NOT NULL,
    estado VARCHAR(15) NOT NULL,
    ultimo_acceso TIMESTAMP,
    creado_en TIMESTAMP NOT NULL,
    actualizado_en TIMESTAMP NOT NULL,
    CONSTRAINT fk_usuario_institucion
        FOREIGN KEY (id_institucion) REFERENCES institucion(id)
);

CREATE TABLE usuario_rol (
    id UUID PRIMARY KEY,
    id_usuario UUID NOT NULL,
    id_rol UUID NOT NULL,
    activo BOOLEAN NOT NULL,
    creado_en TIMESTAMP NOT NULL,
    actualizado_en TIMESTAMP NOT NULL,
    CONSTRAINT uq_usuario_rol UNIQUE (id_usuario, id_rol),
    CONSTRAINT fk_usuario_rol_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario(id),
    CONSTRAINT fk_usuario_rol_rol
        FOREIGN KEY (id_rol) REFERENCES rol(id)
);

-- =========================================================
-- SPRINT 1: ESTRUCTURA ACADEMICA
-- =========================================================

CREATE TABLE gestion_academica (
    id UUID PRIMARY KEY,
    id_institucion UUID NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    activa BOOLEAN NOT NULL,
    estado VARCHAR(15) NOT NULL,
    creado_en TIMESTAMP NOT NULL,
    actualizado_en TIMESTAMP NOT NULL,
    CONSTRAINT uq_gestion_academica UNIQUE (id_institucion, nombre),
    CONSTRAINT fk_gestion_institucion
        FOREIGN KEY (id_institucion) REFERENCES institucion(id)
);

CREATE TABLE curso (
    id UUID PRIMARY KEY,
    id_institucion UUID NOT NULL,
    codigo VARCHAR(30),
    nombre VARCHAR(100) NOT NULL,
    nivel VARCHAR(50),
    orden_visual INTEGER,
    estado VARCHAR(15) NOT NULL,
    creado_en TIMESTAMP NOT NULL,
    actualizado_en TIMESTAMP NOT NULL,
    CONSTRAINT uq_curso_nombre UNIQUE (id_institucion, nombre),
    CONSTRAINT fk_curso_institucion
        FOREIGN KEY (id_institucion) REFERENCES institucion(id)
);

CREATE TABLE paralelo (
    id UUID PRIMARY KEY,
    id_institucion UUID NOT NULL,
    id_curso UUID NOT NULL,
    id_gestion_academica UUID NOT NULL,
    nombre VARCHAR(20) NOT NULL,
    capacidad INTEGER,
    estado VARCHAR(15) NOT NULL,
    creado_en TIMESTAMP NOT NULL,
    actualizado_en TIMESTAMP NOT NULL,
    CONSTRAINT uq_paralelo UNIQUE (id_institucion, id_curso, id_gestion_academica, nombre),
    CONSTRAINT fk_paralelo_institucion
        FOREIGN KEY (id_institucion) REFERENCES institucion(id),
    CONSTRAINT fk_paralelo_curso
        FOREIGN KEY (id_curso) REFERENCES curso(id),
    CONSTRAINT fk_paralelo_gestion
        FOREIGN KEY (id_gestion_academica) REFERENCES gestion_academica(id)
);

CREATE TABLE aula (
    id UUID PRIMARY KEY,
    id_institucion UUID NOT NULL,
    codigo VARCHAR(30) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    capacidad INTEGER NOT NULL,
    ubicacion VARCHAR(180),
    recursos VARCHAR(500),
    estado VARCHAR(15) NOT NULL,
    creado_en TIMESTAMP NOT NULL,
    actualizado_en TIMESTAMP NOT NULL,
    CONSTRAINT uq_aula_codigo UNIQUE (id_institucion, codigo),
    CONSTRAINT uq_aula_nombre UNIQUE (id_institucion, nombre),
    CONSTRAINT fk_aula_institucion
        FOREIGN KEY (id_institucion) REFERENCES institucion(id)
);

CREATE TABLE materia (
    id UUID PRIMARY KEY,
    id_institucion UUID NOT NULL,
    codigo VARCHAR(30) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    area VARCHAR(100),
    carga_horaria INTEGER,
    estado VARCHAR(15) NOT NULL,
    creado_en TIMESTAMP NOT NULL,
    actualizado_en TIMESTAMP NOT NULL,
    CONSTRAINT uq_materia_codigo UNIQUE (id_institucion, codigo),
    CONSTRAINT uq_materia_nombre UNIQUE (id_institucion, nombre),
    CONSTRAINT fk_materia_institucion
        FOREIGN KEY (id_institucion) REFERENCES institucion(id)
);

CREATE TABLE curso_materia (
    id UUID PRIMARY KEY,
    id_institucion UUID NOT NULL,
    id_curso UUID NOT NULL,
    id_materia UUID NOT NULL,
    id_gestion_academica UUID NOT NULL,
    carga_horaria INTEGER,
    estado VARCHAR(15) NOT NULL,
    creado_en TIMESTAMP NOT NULL,
    actualizado_en TIMESTAMP NOT NULL,
    CONSTRAINT uq_curso_materia UNIQUE (id_institucion, id_curso, id_materia, id_gestion_academica),
    CONSTRAINT fk_curso_materia_institucion
        FOREIGN KEY (id_institucion) REFERENCES institucion(id),
    CONSTRAINT fk_curso_materia_curso
        FOREIGN KEY (id_curso) REFERENCES curso(id),
    CONSTRAINT fk_curso_materia_materia
        FOREIGN KEY (id_materia) REFERENCES materia(id),
    CONSTRAINT fk_curso_materia_gestion
        FOREIGN KEY (id_gestion_academica) REFERENCES gestion_academica(id)
);

-- =========================================================
-- SPRINT 1: PERSONAS
-- =========================================================

CREATE TABLE docente (
    id UUID PRIMARY KEY,
    id_institucion UUID NOT NULL,
    id_usuario UUID,
    codigo VARCHAR(30),
    documento_identidad VARCHAR(30) NOT NULL,
    nombres VARCHAR(120) NOT NULL,
    apellidos VARCHAR(120) NOT NULL,
    telefono VARCHAR(30),
    correo VARCHAR(255),
    especialidad VARCHAR(120),
    estado VARCHAR(15) NOT NULL,
    creado_en TIMESTAMP NOT NULL,
    actualizado_en TIMESTAMP NOT NULL,
    CONSTRAINT uq_docente_documento UNIQUE (id_institucion, documento_identidad),
    CONSTRAINT fk_docente_institucion
        FOREIGN KEY (id_institucion) REFERENCES institucion(id),
    CONSTRAINT fk_docente_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario(id)
);

CREATE TABLE estudiante (
    id UUID PRIMARY KEY,
    id_institucion UUID NOT NULL,
    id_usuario UUID,
    codigo_estudiante VARCHAR(30),
    documento_identidad VARCHAR(30),
    nombres VARCHAR(120) NOT NULL,
    apellidos VARCHAR(120) NOT NULL,
    fecha_nacimiento DATE,
    sexo VARCHAR(15),
    direccion VARCHAR(255),
    telefono VARCHAR(30),
    correo VARCHAR(255),
    estado VARCHAR(15) NOT NULL,
    creado_en TIMESTAMP NOT NULL,
    actualizado_en TIMESTAMP NOT NULL,
    CONSTRAINT uq_estudiante_documento UNIQUE (id_institucion, documento_identidad),
    CONSTRAINT fk_estudiante_institucion
        FOREIGN KEY (id_institucion) REFERENCES institucion(id),
    CONSTRAINT fk_estudiante_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario(id)
);

CREATE TABLE tutor (
    id UUID PRIMARY KEY,
    id_institucion UUID NOT NULL,
    id_usuario UUID,
    documento_identidad VARCHAR(30),
    nombres VARCHAR(120) NOT NULL,
    apellidos VARCHAR(120) NOT NULL,
    telefono VARCHAR(30),
    correo VARCHAR(255),
    direccion VARCHAR(255),
    estado VARCHAR(15) NOT NULL,
    creado_en TIMESTAMP NOT NULL,
    actualizado_en TIMESTAMP NOT NULL,
    CONSTRAINT uq_tutor_documento UNIQUE (id_institucion, documento_identidad),
    CONSTRAINT fk_tutor_institucion
        FOREIGN KEY (id_institucion) REFERENCES institucion(id),
    CONSTRAINT fk_tutor_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario(id)
);

CREATE TABLE estudiante_tutor (
    id UUID PRIMARY KEY,
    id_institucion UUID NOT NULL,
    id_estudiante UUID NOT NULL,
    id_tutor UUID NOT NULL,
    parentesco VARCHAR(50) NOT NULL,
    es_principal BOOLEAN NOT NULL,
    estado VARCHAR(15) NOT NULL,
    creado_en TIMESTAMP NOT NULL,
    actualizado_en TIMESTAMP NOT NULL,
    CONSTRAINT uq_estudiante_tutor UNIQUE (id_estudiante, id_tutor),
    CONSTRAINT fk_estudiante_tutor_institucion
        FOREIGN KEY (id_institucion) REFERENCES institucion(id),
    CONSTRAINT fk_estudiante_tutor_estudiante
        FOREIGN KEY (id_estudiante) REFERENCES estudiante(id),
    CONSTRAINT fk_estudiante_tutor_tutor
        FOREIGN KEY (id_tutor) REFERENCES tutor(id)
);

-- =========================================================
-- SPRINT 1: OPERACION ACADEMICA
-- =========================================================

CREATE TABLE inscripcion (
    id UUID PRIMARY KEY,
    id_institucion UUID NOT NULL,
    id_estudiante UUID NOT NULL,
    id_gestion_academica UUID NOT NULL,
    id_paralelo UUID NOT NULL,
    fecha_inscripcion DATE NOT NULL,
    estado VARCHAR(15) NOT NULL,
    observacion VARCHAR(255),
    creado_en TIMESTAMP NOT NULL,
    actualizado_en TIMESTAMP NOT NULL,
    CONSTRAINT uq_inscripcion_estudiante_gestion UNIQUE (id_estudiante, id_gestion_academica),
    CONSTRAINT fk_inscripcion_institucion
        FOREIGN KEY (id_institucion) REFERENCES institucion(id),
    CONSTRAINT fk_inscripcion_estudiante
        FOREIGN KEY (id_estudiante) REFERENCES estudiante(id),
    CONSTRAINT fk_inscripcion_gestion
        FOREIGN KEY (id_gestion_academica) REFERENCES gestion_academica(id),
    CONSTRAINT fk_inscripcion_paralelo
        FOREIGN KEY (id_paralelo) REFERENCES paralelo(id)
);

CREATE TABLE asignacion_docente (
    id UUID PRIMARY KEY,
    id_institucion UUID NOT NULL,
    id_docente UUID NOT NULL,
    id_materia UUID NOT NULL,
    id_paralelo UUID NOT NULL,
    id_gestion_academica UUID NOT NULL,
    carga_horaria INTEGER,
    estado VARCHAR(15) NOT NULL,
    creado_en TIMESTAMP NOT NULL,
    actualizado_en TIMESTAMP NOT NULL,
    CONSTRAINT uq_asignacion_docente UNIQUE (id_docente, id_materia, id_paralelo, id_gestion_academica),
    CONSTRAINT fk_asignacion_institucion
        FOREIGN KEY (id_institucion) REFERENCES institucion(id),
    CONSTRAINT fk_asignacion_docente_docente
        FOREIGN KEY (id_docente) REFERENCES docente(id),
    CONSTRAINT fk_asignacion_docente_materia
        FOREIGN KEY (id_materia) REFERENCES materia(id),
    CONSTRAINT fk_asignacion_docente_paralelo
        FOREIGN KEY (id_paralelo) REFERENCES paralelo(id),
    CONSTRAINT fk_asignacion_docente_gestion
        FOREIGN KEY (id_gestion_academica) REFERENCES gestion_academica(id)
);

CREATE TABLE bitacora_auditoria (
    id UUID PRIMARY KEY,
    id_institucion UUID,
    id_usuario UUID,
    fecha_evento TIMESTAMP NOT NULL,
    direccion_ip VARCHAR(50),
    plataforma_cliente VARCHAR(30),
    agente_usuario TEXT,
    nombre_modulo VARCHAR(100) NOT NULL,
    nombre_entidad VARCHAR(100),
    id_entidad VARCHAR(100),
    tipo_operacion VARCHAR(30) NOT NULL,
    datos_antes TEXT,
    datos_despues TEXT,
    exito BOOLEAN NOT NULL,
    mensaje VARCHAR(255),
    creado_en TIMESTAMP NOT NULL,
    CONSTRAINT fk_bitacora_institucion
        FOREIGN KEY (id_institucion) REFERENCES institucion(id),
    CONSTRAINT fk_bitacora_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario(id)
);

-- =========================================================
-- SPRINT 1: ARCHIVOS
-- =========================================================

CREATE TABLE archivo (
    id UUID PRIMARY KEY,
    id_institucion UUID NOT NULL,
    id_usuario_subio UUID,
    nombre_original VARCHAR(255) NOT NULL,
    nombre_archivo VARCHAR(255) NOT NULL,
    extension VARCHAR(20),
    mime_type VARCHAR(100) NOT NULL,
    tamano_bytes BIGINT NOT NULL,
    bucket_s3 VARCHAR(150) NOT NULL,
    region_s3 VARCHAR(50),
    key_s3 TEXT NOT NULL,
    etag VARCHAR(100),
    checksum_sha256 VARCHAR(128),
    categoria VARCHAR(30) NOT NULL,
    visibilidad VARCHAR(20) NOT NULL,
    estado VARCHAR(15) NOT NULL,
    creado_en TIMESTAMP NOT NULL,
    actualizado_en TIMESTAMP NOT NULL,
    CONSTRAINT fk_archivo_institucion
        FOREIGN KEY (id_institucion) REFERENCES institucion(id),
    CONSTRAINT fk_archivo_usuario
        FOREIGN KEY (id_usuario_subio) REFERENCES usuario(id)
);

CREATE TABLE archivo_referencia (
    id UUID PRIMARY KEY,
    id_institucion UUID NOT NULL,
    id_archivo UUID NOT NULL,
    modulo VARCHAR(50) NOT NULL,
    entidad VARCHAR(50) NOT NULL,
    id_entidad UUID NOT NULL,
    tipo_referencia VARCHAR(30) NOT NULL,
    es_principal BOOLEAN NOT NULL,
    orden_visual INTEGER,
    observacion VARCHAR(255),
    estado VARCHAR(15) NOT NULL,
    creado_en TIMESTAMP NOT NULL,
    actualizado_en TIMESTAMP NOT NULL,
    CONSTRAINT fk_archivo_referencia_institucion
        FOREIGN KEY (id_institucion) REFERENCES institucion(id),
    CONSTRAINT fk_archivo_referencia_archivo
        FOREIGN KEY (id_archivo) REFERENCES archivo(id)
);

-- =========================================================
-- SPRINT 2: ASISTENCIA
-- =========================================================

CREATE TABLE asistencia_registro (
    id UUID PRIMARY KEY,
    id_institucion UUID NOT NULL,
    id_asignacion_docente UUID NOT NULL,
    registrado_por UUID,
    fecha DATE NOT NULL,
    estado VARCHAR(15) NOT NULL,
    creado_en TIMESTAMP NOT NULL,
    actualizado_en TIMESTAMP NOT NULL,
    CONSTRAINT uq_asistencia_registro_asignacion_fecha UNIQUE (id_asignacion_docente, fecha),
    CONSTRAINT fk_asistencia_registro_institucion
        FOREIGN KEY (id_institucion) REFERENCES institucion(id),
    CONSTRAINT fk_asistencia_registro_asignacion
        FOREIGN KEY (id_asignacion_docente) REFERENCES asignacion_docente(id),
    CONSTRAINT fk_asistencia_registro_usuario
        FOREIGN KEY (registrado_por) REFERENCES usuario(id)
);

CREATE TABLE asistencia_detalle (
    id UUID PRIMARY KEY,
    id_asistencia_registro UUID NOT NULL,
    id_inscripcion UUID NOT NULL,
    estado_asistencia VARCHAR(15) NOT NULL,
    creado_en TIMESTAMP NOT NULL,
    actualizado_en TIMESTAMP NOT NULL,
    CONSTRAINT uq_asistencia_detalle_registro_inscripcion UNIQUE (id_asistencia_registro, id_inscripcion),
    CONSTRAINT fk_asistencia_detalle_registro
        FOREIGN KEY (id_asistencia_registro) REFERENCES asistencia_registro(id),
    CONSTRAINT fk_asistencia_detalle_inscripcion
        FOREIGN KEY (id_inscripcion) REFERENCES inscripcion(id)
);

-- =========================================================
-- SPRINT 2: CALIFICACIONES
-- =========================================================

CREATE TABLE evaluacion (
    id UUID PRIMARY KEY,
    id_institucion UUID NOT NULL,
    id_asignacion_docente UUID NOT NULL,
    creado_por UUID,
    periodo INTEGER NOT NULL,
    tipo VARCHAR(40) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    ponderacion NUMERIC(5,2) NOT NULL,
    escala VARCHAR(15) NOT NULL,
    estado VARCHAR(15) NOT NULL,
    creado_en TIMESTAMP NOT NULL,
    actualizado_en TIMESTAMP NOT NULL,
    CONSTRAINT uq_evaluacion_asignacion_periodo_nombre UNIQUE (id_asignacion_docente, periodo, nombre),
    CONSTRAINT fk_evaluacion_institucion
        FOREIGN KEY (id_institucion) REFERENCES institucion(id),
    CONSTRAINT fk_evaluacion_asignacion
        FOREIGN KEY (id_asignacion_docente) REFERENCES asignacion_docente(id),
    CONSTRAINT fk_evaluacion_usuario
        FOREIGN KEY (creado_por) REFERENCES usuario(id)
);

CREATE TABLE calificacion (
    id UUID PRIMARY KEY,
    id_institucion UUID NOT NULL,
    id_evaluacion UUID NOT NULL,
    id_inscripcion UUID NOT NULL,
    registrado_por UUID,
    nota_numerica NUMERIC(8,2),
    nota_literal VARCHAR(5),
    creado_en TIMESTAMP NOT NULL,
    actualizado_en TIMESTAMP NOT NULL,
    CONSTRAINT uq_calificacion_evaluacion_inscripcion UNIQUE (id_evaluacion, id_inscripcion),
    CONSTRAINT fk_calificacion_institucion
        FOREIGN KEY (id_institucion) REFERENCES institucion(id),
    CONSTRAINT fk_calificacion_evaluacion
        FOREIGN KEY (id_evaluacion) REFERENCES evaluacion(id),
    CONSTRAINT fk_calificacion_inscripcion
        FOREIGN KEY (id_inscripcion) REFERENCES inscripcion(id),
    CONSTRAINT fk_calificacion_usuario
        FOREIGN KEY (registrado_por) REFERENCES usuario(id)
);

CREATE TABLE calificacion_cambio (
    id UUID PRIMARY KEY,
    id_institucion UUID NOT NULL,
    id_calificacion UUID NOT NULL,
    id_usuario UUID,
    valor_anterior VARCHAR(30),
    valor_nuevo VARCHAR(30) NOT NULL,
    razon VARCHAR(255) NOT NULL,
    fecha_cambio TIMESTAMP NOT NULL,
    CONSTRAINT fk_calificacion_cambio_institucion
        FOREIGN KEY (id_institucion) REFERENCES institucion(id),
    CONSTRAINT fk_calificacion_cambio_calificacion
        FOREIGN KEY (id_calificacion) REFERENCES calificacion(id),
    CONSTRAINT fk_calificacion_cambio_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario(id)
);
