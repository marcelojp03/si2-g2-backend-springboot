-- =========================================================
-- SISTEMA DE GESTIÓN ACADÉMICA SaaS — SCRIPT CONSOLIDADO
-- Sprint 1 + Sprint 2 (incl. HU-S2-18 trimestral) + Sprint Especial
-- PostgreSQL · Schema: sia
-- =========================================================
-- Script único para crear el esquema completo desde cero.
-- Para bases de datos existentes usar los scripts incrementales
-- correspondientes (sprint-especial-saas-migration.sql, etc.)
-- =========================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "citext";

CREATE SCHEMA IF NOT EXISTS sia;
SET search_path TO sia, public;

-- =========================================================
-- FUNCIÓN GENERAL DE actualizado_en
-- =========================================================

CREATE OR REPLACE FUNCTION fn_actualizar_actualizado_en()
RETURNS TRIGGER AS $$
BEGIN
    NEW.actualizado_en = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =========================================================
-- 1. INSTITUCIÓN / SEGURIDAD / PERMISOS
-- =========================================================

CREATE TABLE institucion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(200) NOT NULL,
    tipo_institucion VARCHAR(20) NOT NULL CHECK (tipo_institucion IN ('FISCAL', 'CONVENIO', 'PRIVADO')),
    telefono VARCHAR(30),
    correo CITEXT,
    direccion VARCHAR(255),
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE configuracion_institucion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    clave VARCHAR(100) NOT NULL,
    valor TEXT NOT NULL,
    tipo_valor VARCHAR(30) NOT NULL DEFAULT 'TEXTO'
        CHECK (tipo_valor IN ('TEXTO', 'NUMERO', 'BOOLEANO', 'JSON')),
    descripcion VARCHAR(255),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_configuracion_institucion UNIQUE (id_institucion, clave)
);

CREATE TABLE rol (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo VARCHAR(50) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    id_institucion UUID NULL REFERENCES institucion(id) ON DELETE CASCADE,
    descripcion VARCHAR(255),
    es_global BOOLEAN NOT NULL DEFAULT FALSE,
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_rol_nombre_institucion UNIQUE NULLS NOT DISTINCT (id_institucion, nombre)
);

CREATE TABLE permiso (
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

CREATE TABLE rol_permiso (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_rol UUID NOT NULL REFERENCES rol(id) ON DELETE CASCADE,
    id_permiso UUID NOT NULL REFERENCES permiso(id) ON DELETE CASCADE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_rol_permiso UNIQUE (id_rol, id_permiso)
);

CREATE TABLE usuario (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NULL REFERENCES institucion(id) ON DELETE CASCADE,
    correo CITEXT NOT NULL UNIQUE,
    hash_contrasena TEXT NOT NULL,
    nombres VARCHAR(120) NOT NULL,
    apellidos VARCHAR(120) NOT NULL,
    telefono VARCHAR(30),
    requiere_cambio_contrasena BOOLEAN NOT NULL DEFAULT FALSE,
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'INACTIVO', 'BLOQUEADO')),
    ultimo_acceso TIMESTAMPTZ NULL,
    fcm_token TEXT NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON COLUMN usuario.fcm_token IS 'Token FCM del dispositivo móvil (Firebase Cloud Messaging). Nulo si el usuario no usa la app móvil.';

CREATE TABLE usuario_rol (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_usuario UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    id_rol UUID NOT NULL REFERENCES rol(id),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_usuario_rol UNIQUE (id_usuario, id_rol)
);

CREATE UNIQUE INDEX uq_usuario_rol_activo_unico
    ON usuario_rol (id_usuario) WHERE activo = TRUE;

-- =========================================================
-- 2. ESTRUCTURA ACADÉMICA
-- =========================================================

CREATE TABLE gestion_academica (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    nombre VARCHAR(100) NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    activa BOOLEAN NOT NULL DEFAULT FALSE,
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVA' CHECK (estado IN ('ACTIVA', 'CERRADA', 'ANULADA')),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_gestion_academica UNIQUE (id_institucion, nombre),
    CONSTRAINT uq_gestion_id_institucion UNIQUE (id, id_institucion),
    CONSTRAINT ck_gestion_fechas CHECK (fecha_fin >= fecha_inicio)
);

CREATE UNIQUE INDEX uq_gestion_activa_por_institucion
    ON gestion_academica (id_institucion) WHERE activa = TRUE;

CREATE TABLE curso (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    codigo VARCHAR(30),
    nombre VARCHAR(100) NOT NULL,
    nivel VARCHAR(50),
    orden_visual INTEGER,
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_curso_nombre UNIQUE (id_institucion, nombre),
    CONSTRAINT uq_curso_codigo UNIQUE NULLS NOT DISTINCT (id_institucion, codigo),
    CONSTRAINT uq_curso_id_institucion UNIQUE (id, id_institucion)
);

CREATE TABLE paralelo (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_curso UUID NOT NULL,
    id_gestion_academica UUID NOT NULL,
    nombre VARCHAR(20) NOT NULL,
    capacidad INTEGER,
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_paralelo UNIQUE (id_institucion, id_curso, id_gestion_academica, nombre),
    CONSTRAINT uq_paralelo_id_institucion UNIQUE (id, id_institucion),
    CONSTRAINT ck_paralelo_capacidad CHECK (capacidad IS NULL OR capacidad > 0),
    CONSTRAINT fk_paralelo_curso_institucion
        FOREIGN KEY (id_curso, id_institucion) REFERENCES curso (id, id_institucion) ON DELETE CASCADE,
    CONSTRAINT fk_paralelo_gestion_institucion
        FOREIGN KEY (id_gestion_academica, id_institucion) REFERENCES gestion_academica (id, id_institucion) ON DELETE CASCADE
);

CREATE TABLE aula (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    codigo VARCHAR(30) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    capacidad INTEGER NOT NULL,
    ubicacion VARCHAR(180),
    recursos VARCHAR(500),
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_aula_codigo UNIQUE (id_institucion, codigo),
    CONSTRAINT uq_aula_nombre UNIQUE (id_institucion, nombre),
    CONSTRAINT uq_aula_id_institucion UNIQUE (id, id_institucion),
    CONSTRAINT ck_aula_capacidad CHECK (capacidad > 0)
);

CREATE INDEX idx_aula_institucion_estado ON aula (id_institucion, estado);
CREATE INDEX idx_aula_capacidad ON aula (id_institucion, capacidad);

CREATE TABLE materia (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    codigo VARCHAR(30) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    area VARCHAR(100),
    carga_horaria INTEGER,
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_materia_codigo UNIQUE (id_institucion, codigo),
    CONSTRAINT uq_materia_nombre UNIQUE (id_institucion, nombre),
    CONSTRAINT uq_materia_id_institucion UNIQUE (id, id_institucion),
    CONSTRAINT ck_materia_carga CHECK (carga_horaria IS NULL OR carga_horaria > 0)
);

CREATE TABLE curso_materia (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_curso UUID NOT NULL,
    id_materia UUID NOT NULL,
    id_gestion_academica UUID NOT NULL,
    carga_horaria INTEGER,
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_curso_materia UNIQUE (id_institucion, id_curso, id_materia, id_gestion_academica),
    CONSTRAINT ck_curso_materia_carga CHECK (carga_horaria IS NULL OR carga_horaria > 0),
    CONSTRAINT fk_curso_materia_curso_institucion
        FOREIGN KEY (id_curso, id_institucion) REFERENCES curso (id, id_institucion) ON DELETE CASCADE,
    CONSTRAINT fk_curso_materia_materia_institucion
        FOREIGN KEY (id_materia, id_institucion) REFERENCES materia (id, id_institucion) ON DELETE CASCADE,
    CONSTRAINT fk_curso_materia_gestion_institucion
        FOREIGN KEY (id_gestion_academica, id_institucion) REFERENCES gestion_academica (id, id_institucion) ON DELETE CASCADE
);

-- =========================================================
-- 3. PERSONAS ACADÉMICAS
-- =========================================================

CREATE TABLE docente (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_usuario UUID NULL REFERENCES usuario(id) ON DELETE SET NULL,
    codigo VARCHAR(30),
    documento_identidad VARCHAR(30) NOT NULL,
    nombres VARCHAR(120) NOT NULL,
    apellidos VARCHAR(120) NOT NULL,
    telefono VARCHAR(30),
    correo CITEXT,
    especialidad VARCHAR(120),
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_docente_documento UNIQUE (id_institucion, documento_identidad),
    CONSTRAINT uq_docente_codigo UNIQUE (id_institucion, codigo),
    CONSTRAINT uq_docente_correo UNIQUE (id_institucion, correo),
    CONSTRAINT uq_docente_usuario UNIQUE (id_usuario),
    CONSTRAINT uq_docente_id_institucion UNIQUE (id, id_institucion)
);

CREATE TABLE estudiante (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_usuario UUID NULL REFERENCES usuario(id) ON DELETE SET NULL,
    codigo_estudiante VARCHAR(30),
    documento_identidad VARCHAR(30),
    nombres VARCHAR(120) NOT NULL,
    apellidos VARCHAR(120) NOT NULL,
    fecha_nacimiento DATE,
    sexo VARCHAR(15) CHECK (sexo IN ('MASCULINO', 'FEMENINO', 'OTRO')),
    direccion VARCHAR(255),
    telefono VARCHAR(30),
    correo CITEXT,
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'INACTIVO', 'EGRESADO', 'RETIRADO')),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_estudiante_codigo UNIQUE (id_institucion, codigo_estudiante),
    CONSTRAINT uq_estudiante_documento UNIQUE (id_institucion, documento_identidad),
    CONSTRAINT uq_estudiante_correo UNIQUE (id_institucion, correo),
    CONSTRAINT uq_estudiante_usuario UNIQUE (id_usuario),
    CONSTRAINT uq_estudiante_id_institucion UNIQUE (id, id_institucion)
);

CREATE TABLE tutor (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_usuario UUID NULL REFERENCES usuario(id) ON DELETE SET NULL,
    documento_identidad VARCHAR(30),
    nombres VARCHAR(120) NOT NULL,
    apellidos VARCHAR(120) NOT NULL,
    telefono VARCHAR(30),
    correo CITEXT,
    direccion VARCHAR(255),
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_tutor_documento UNIQUE (id_institucion, documento_identidad),
    CONSTRAINT uq_tutor_correo UNIQUE (id_institucion, correo),
    CONSTRAINT uq_tutor_usuario UNIQUE (id_usuario),
    CONSTRAINT uq_tutor_id_institucion UNIQUE (id, id_institucion)
);

CREATE TABLE estudiante_tutor (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_estudiante UUID NOT NULL,
    id_tutor UUID NOT NULL,
    parentesco VARCHAR(50) NOT NULL,
    es_principal BOOLEAN NOT NULL DEFAULT FALSE,
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_estudiante_tutor UNIQUE (id_institucion, id_estudiante, id_tutor),
    CONSTRAINT fk_est_tutor_estudiante_institucion
        FOREIGN KEY (id_estudiante, id_institucion) REFERENCES estudiante (id, id_institucion) ON DELETE CASCADE,
    CONSTRAINT fk_est_tutor_tutor_institucion
        FOREIGN KEY (id_tutor, id_institucion) REFERENCES tutor (id, id_institucion) ON DELETE CASCADE
);

CREATE UNIQUE INDEX uq_estudiante_tutor_principal
    ON estudiante_tutor (id_institucion, id_estudiante)
    WHERE es_principal = TRUE AND estado = 'ACTIVO';

-- =========================================================
-- 4. OPERACIÓN ACADÉMICA — Sprint 1
-- =========================================================

CREATE TABLE inscripcion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_estudiante UUID NOT NULL,
    id_gestion_academica UUID NOT NULL,
    id_paralelo UUID NOT NULL,
    fecha_inscripcion DATE NOT NULL DEFAULT CURRENT_DATE,
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVA' CHECK (estado IN ('ACTIVA', 'RETIRADA', 'CONCLUIDA', 'ANULADA')),
    observacion VARCHAR(255),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_inscripcion_estudiante_institucion
        FOREIGN KEY (id_estudiante, id_institucion) REFERENCES estudiante (id, id_institucion) ON DELETE CASCADE,
    CONSTRAINT fk_inscripcion_gestion_institucion
        FOREIGN KEY (id_gestion_academica, id_institucion) REFERENCES gestion_academica (id, id_institucion) ON DELETE CASCADE,
    CONSTRAINT fk_inscripcion_paralelo_institucion
        FOREIGN KEY (id_paralelo, id_institucion) REFERENCES paralelo (id, id_institucion) ON DELETE CASCADE
);

CREATE UNIQUE INDEX uq_inscripcion_activa_estudiante_gestion
    ON inscripcion (id_institucion, id_estudiante, id_gestion_academica)
    WHERE estado = 'ACTIVA';

CREATE TABLE asignacion_docente (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_docente UUID NOT NULL,
    id_materia UUID NOT NULL,
    id_paralelo UUID NOT NULL,
    id_gestion_academica UUID NOT NULL,
    carga_horaria INTEGER,
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVA' CHECK (estado IN ('ACTIVA', 'INACTIVA')),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_asignacion_docente UNIQUE (id_institucion, id_docente, id_materia, id_paralelo, id_gestion_academica),
    CONSTRAINT uq_asignacion_id_institucion UNIQUE (id, id_institucion),
    CONSTRAINT ck_asignacion_carga CHECK (carga_horaria IS NULL OR carga_horaria > 0),
    CONSTRAINT fk_asignacion_docente_institucion
        FOREIGN KEY (id_docente, id_institucion) REFERENCES docente (id, id_institucion) ON DELETE CASCADE,
    CONSTRAINT fk_asignacion_materia_institucion
        FOREIGN KEY (id_materia, id_institucion) REFERENCES materia (id, id_institucion) ON DELETE CASCADE,
    CONSTRAINT fk_asignacion_paralelo_institucion
        FOREIGN KEY (id_paralelo, id_institucion) REFERENCES paralelo (id, id_institucion) ON DELETE CASCADE,
    CONSTRAINT fk_asignacion_gestion_institucion
        FOREIGN KEY (id_gestion_academica, id_institucion) REFERENCES gestion_academica (id, id_institucion) ON DELETE CASCADE
);

-- =========================================================
-- 5. BITÁCORA / CAJA NEGRA (incluye columnas Sprint 2)
-- =========================================================

CREATE TABLE bitacora_auditoria (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NULL REFERENCES institucion(id),
    id_usuario UUID NULL REFERENCES usuario(id),
    fecha_evento TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    direccion_ip VARCHAR(50),
    plataforma_cliente VARCHAR(30),
    agente_usuario TEXT,
    nombre_modulo VARCHAR(100) NOT NULL,
    nombre_entidad VARCHAR(100),
    id_entidad VARCHAR(100),
    tipo_operacion VARCHAR(30) NOT NULL,
    datos_antes JSONB,
    datos_despues JSONB,
    exito BOOLEAN NOT NULL DEFAULT TRUE,
    mensaje VARCHAR(255),
    metodo_http VARCHAR(10),
    ruta_recurso VARCHAR(255),
    nombre_funcion VARCHAR(150),
    hash_integridad VARCHAR(128),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_bitacora_institucion ON bitacora_auditoria (id_institucion);
CREATE INDEX idx_bitacora_usuario ON bitacora_auditoria (id_usuario);
CREATE INDEX idx_bitacora_fecha ON bitacora_auditoria (fecha_evento DESC);
CREATE INDEX idx_bitacora_modulo ON bitacora_auditoria (nombre_modulo);

-- =========================================================
-- 6. ÍNDICES DE APOYO
-- =========================================================

CREATE INDEX idx_usuario_institucion ON usuario (id_institucion);
CREATE INDEX idx_gestion_institucion ON gestion_academica (id_institucion);
CREATE INDEX idx_curso_institucion ON curso (id_institucion);
CREATE INDEX idx_paralelo_institucion ON paralelo (id_institucion);
CREATE INDEX idx_materia_institucion ON materia (id_institucion);
CREATE INDEX idx_docente_institucion ON docente (id_institucion);
CREATE INDEX idx_estudiante_institucion ON estudiante (id_institucion);
CREATE INDEX idx_tutor_institucion ON tutor (id_institucion);
CREATE INDEX idx_inscripcion_institucion ON inscripcion (id_institucion);
CREATE INDEX idx_asignacion_institucion ON asignacion_docente (id_institucion);

-- =========================================================
-- 7. SUBSISTEMA DE ARCHIVOS / S3
-- =========================================================

CREATE TABLE sia.archivo (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES sia.institucion(id) ON DELETE CASCADE,
    id_usuario_subio UUID NULL REFERENCES sia.usuario(id) ON DELETE SET NULL,
    nombre_original VARCHAR(255) NOT NULL,
    nombre_archivo VARCHAR(255) NOT NULL,
    extension VARCHAR(20),
    mime_type VARCHAR(100) NOT NULL,
    tamano_bytes BIGINT NOT NULL CHECK (tamano_bytes >= 0),
    bucket_s3 VARCHAR(150) NOT NULL,
    region_s3 VARCHAR(50),
    key_s3 TEXT NOT NULL,
    etag VARCHAR(100),
    checksum_sha256 VARCHAR(128),
    categoria VARCHAR(30) NOT NULL CHECK (
        categoria IN ('IMAGEN', 'DOCUMENTO', 'EVIDENCIA', 'ADJUNTO', 'OTRO')),
    visibilidad VARCHAR(20) NOT NULL DEFAULT 'PRIVADO' CHECK (
        visibilidad IN ('PRIVADO', 'PUBLICO', 'FIRMADO')),
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVO' CHECK (
        estado IN ('ACTIVO', 'ELIMINADO')),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_archivo_id_institucion UNIQUE (id, id_institucion)
);

CREATE TABLE sia.archivo_referencia (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES sia.institucion(id) ON DELETE CASCADE,
    id_archivo UUID NOT NULL,
    modulo VARCHAR(50) NOT NULL,
    entidad VARCHAR(50) NOT NULL,
    id_entidad UUID NOT NULL,
    tipo_referencia VARCHAR(30) NOT NULL CHECK (
        tipo_referencia IN ('LOGO', 'FOTO_PERFIL', 'EVIDENCIA', 'DOCUMENTO', 'ADJUNTO')),
    es_principal BOOLEAN NOT NULL DEFAULT FALSE,
    orden_visual INTEGER,
    observacion VARCHAR(255),
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVO' CHECK (
        estado IN ('ACTIVO', 'INACTIVO')),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_archivo_referencia_archivo_institucion
        FOREIGN KEY (id_archivo, id_institucion) REFERENCES sia.archivo (id, id_institucion) ON DELETE CASCADE
);

CREATE INDEX idx_archivo_institucion ON sia.archivo(id_institucion);
CREATE INDEX idx_archivo_usuario_subio ON sia.archivo(id_usuario_subio);
CREATE INDEX idx_archivo_categoria ON sia.archivo(categoria);
CREATE INDEX idx_archivo_estado ON sia.archivo(estado);
CREATE INDEX idx_archivo_key_s3 ON sia.archivo(key_s3);

CREATE INDEX idx_archivo_referencia_institucion ON sia.archivo_referencia(id_institucion);
CREATE INDEX idx_archivo_referencia_archivo ON sia.archivo_referencia(id_archivo);
CREATE INDEX idx_archivo_referencia_entidad ON sia.archivo_referencia(modulo, entidad, id_entidad);
CREATE INDEX idx_archivo_referencia_tipo ON sia.archivo_referencia(tipo_referencia);

CREATE UNIQUE INDEX uq_archivo_referencia_activa
    ON sia.archivo_referencia(id_institucion, id_archivo, modulo, entidad, id_entidad, tipo_referencia)
    WHERE estado = 'ACTIVO';

CREATE UNIQUE INDEX uq_archivo_referencia_principal
    ON sia.archivo_referencia(id_institucion, modulo, entidad, id_entidad, tipo_referencia)
    WHERE es_principal = TRUE AND estado = 'ACTIVO';

-- =========================================================
-- 8. ASISTENCIA — Sprint 2
-- =========================================================

CREATE TABLE asistencia_registro (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_asignacion_docente UUID NOT NULL REFERENCES asignacion_docente(id),
    registrado_por UUID NULL REFERENCES usuario(id) ON DELETE SET NULL,
    fecha DATE NOT NULL,
    estado VARCHAR(15) NOT NULL DEFAULT 'REGISTRADA'
        CHECK (estado IN ('REGISTRADA', 'MODIFICADA', 'ANULADA')),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_asistencia_registro_asignacion_fecha UNIQUE (id_asignacion_docente, fecha)
);

CREATE TABLE asistencia_detalle (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_asistencia_registro UUID NOT NULL REFERENCES asistencia_registro(id) ON DELETE CASCADE,
    id_inscripcion UUID NOT NULL REFERENCES inscripcion(id),
    estado_asistencia VARCHAR(15) NOT NULL
        CHECK (estado_asistencia IN ('PRESENTE', 'AUSENTE', 'TARDANZA', 'JUSTIFICADO')),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_asistencia_detalle_registro_inscripcion UNIQUE (id_asistencia_registro, id_inscripcion)
);

CREATE INDEX idx_asistencia_registro_institucion_fecha ON asistencia_registro (id_institucion, fecha DESC);
CREATE INDEX idx_asistencia_registro_asignacion ON asistencia_registro (id_asignacion_docente);
CREATE INDEX idx_asistencia_registro_registrado_por ON asistencia_registro (registrado_por);
CREATE INDEX idx_asistencia_detalle_institucion ON asistencia_detalle (id_institucion);
CREATE INDEX idx_asistencia_detalle_registro ON asistencia_detalle (id_asistencia_registro);
CREATE INDEX idx_asistencia_detalle_inscripcion ON asistencia_detalle (id_inscripcion);
CREATE INDEX idx_asistencia_detalle_estado ON asistencia_detalle (estado_asistencia);

-- =========================================================
-- 9. CALIFICACIONES — Sprint 2
-- =========================================================

CREATE TABLE docente_materia (
    id_docente UUID NOT NULL REFERENCES docente(id) ON DELETE CASCADE,
    id_materia UUID NOT NULL REFERENCES materia(id) ON DELETE CASCADE,
    PRIMARY KEY (id_docente, id_materia)
);

CREATE INDEX idx_docente_materia_materia ON docente_materia (id_materia);

CREATE TABLE evaluacion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_materia UUID NOT NULL REFERENCES materia(id),
    id_asignacion_docente UUID NULL REFERENCES asignacion_docente(id),
    creado_por UUID NULL REFERENCES usuario(id) ON DELETE SET NULL,
    periodo INTEGER NOT NULL CHECK (periodo >= 1),
    tipo VARCHAR(40) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    ponderacion NUMERIC(5,2) NOT NULL CHECK (ponderacion > 0 AND ponderacion <= 100),
    escala VARCHAR(15) NOT NULL DEFAULT 'NUMERICA' CHECK (escala IN ('NUMERICA', 'LITERAL')),
    estado VARCHAR(15) NOT NULL DEFAULT 'ABIERTA' CHECK (estado IN ('ABIERTA', 'CERRADA', 'ANULADA')),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_evaluacion_materia_periodo_nombre UNIQUE (id_institucion, id_materia, periodo, nombre)
);

CREATE TABLE evaluacion_materia (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_materia UUID NOT NULL REFERENCES materia(id),
    creado_por UUID NULL REFERENCES usuario(id) ON DELETE SET NULL,
    periodo INTEGER NOT NULL CHECK (periodo >= 1),
    tipo VARCHAR(40) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    ponderacion NUMERIC(5,2) NOT NULL CHECK (ponderacion > 0 AND ponderacion <= 100),
    escala VARCHAR(15) NOT NULL DEFAULT 'NUMERICA' CHECK (escala IN ('NUMERICA', 'LITERAL')),
    estado VARCHAR(15) NOT NULL DEFAULT 'ABIERTA' CHECK (estado IN ('ABIERTA', 'CERRADA', 'ANULADA')),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_eval_materia_periodo_nombre UNIQUE (id_institucion, id_materia, periodo, nombre)
);

CREATE TABLE calificacion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_evaluacion UUID NOT NULL REFERENCES evaluacion(id) ON DELETE CASCADE,
    id_inscripcion UUID NOT NULL REFERENCES inscripcion(id),
    registrado_por UUID NULL REFERENCES usuario(id) ON DELETE SET NULL,
    nota_numerica NUMERIC(8,2) NULL,
    nota_literal VARCHAR(5) NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_calificacion_evaluacion_inscripcion UNIQUE (id_evaluacion, id_inscripcion),
    CONSTRAINT ck_calificacion_valor_unico CHECK (
        (nota_numerica IS NOT NULL AND nota_literal IS NULL) OR
        (nota_numerica IS NULL AND nota_literal IS NOT NULL))
);

CREATE TABLE calificacion_cambio (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_calificacion UUID NOT NULL REFERENCES calificacion(id) ON DELETE CASCADE,
    id_usuario UUID NULL REFERENCES usuario(id) ON DELETE SET NULL,
    valor_anterior VARCHAR(30) NULL,
    valor_nuevo VARCHAR(30) NOT NULL,
    razon VARCHAR(255) NOT NULL,
    fecha_cambio TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_evaluacion_institucion_materia ON evaluacion (id_institucion, id_materia);
CREATE INDEX idx_evaluacion_institucion_materia_periodo ON evaluacion (id_institucion, id_materia, periodo);
CREATE INDEX idx_evaluacion_estado ON evaluacion (estado);
CREATE INDEX idx_eval_materia_inst_materia ON evaluacion_materia (id_institucion, id_materia);
CREATE INDEX idx_calificacion_institucion_evaluacion ON calificacion (id_institucion, id_evaluacion);
CREATE INDEX idx_calificacion_inscripcion ON calificacion (id_inscripcion);
CREATE INDEX idx_calificacion_cambio_calificacion ON calificacion_cambio (id_calificacion, fecha_cambio DESC);
CREATE INDEX idx_calificacion_cambio_usuario ON calificacion_cambio (id_usuario, fecha_cambio DESC);

-- =========================================================
-- 10. HORARIOS — Sprint 2 (con FK compuesta y trigger anti-solapamiento)
-- =========================================================

CREATE TABLE sia.horario_clase (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL,
    id_asignacion_docente UUID NOT NULL,
    id_aula UUID NOT NULL,
    dia_semana VARCHAR(15) NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVO',
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_horario_institucion FOREIGN KEY (id_institucion) REFERENCES sia.institucion(id),
    CONSTRAINT fk_horario_asignacion_institucion
        FOREIGN KEY (id_asignacion_docente, id_institucion)
        REFERENCES sia.asignacion_docente(id, id_institucion),
    CONSTRAINT fk_horario_aula_institucion
        FOREIGN KEY (id_aula, id_institucion) REFERENCES sia.aula(id, id_institucion),
    CONSTRAINT ck_horario_dia_semana CHECK (dia_semana IN ('LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO')),
    CONSTRAINT ck_horario_estado CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    CONSTRAINT ck_horario_horas CHECK (hora_inicio < hora_fin)
);

CREATE INDEX idx_horario_clase_id_institucion ON sia.horario_clase(id_institucion);
CREATE INDEX idx_horario_clase_id_asignacion_docente ON sia.horario_clase(id_asignacion_docente);
CREATE INDEX idx_horario_clase_id_aula ON sia.horario_clase(id_aula);
CREATE INDEX idx_horario_clase_dia_semana ON sia.horario_clase(dia_semana);

CREATE OR REPLACE FUNCTION sia.fn_validar_solapamiento_horario()
RETURNS TRIGGER AS $$
DECLARE
    v_id_docente UUID;
BEGIN
    IF NEW.estado <> 'ACTIVO' THEN
        RETURN NEW;
    END IF;

    IF EXISTS (
        SELECT 1 FROM sia.horario_clase h
        WHERE h.id <> NEW.id
          AND h.id_institucion = NEW.id_institucion
          AND h.id_aula = NEW.id_aula
          AND h.dia_semana = NEW.dia_semana
          AND h.estado = 'ACTIVO'
          AND h.hora_inicio < NEW.hora_fin
          AND h.hora_fin    > NEW.hora_inicio
    ) THEN
        RAISE EXCEPTION 'Solapamiento de horario en el aula seleccionada (mismo día y franja horaria)';
    END IF;

    SELECT a.id_docente INTO v_id_docente
    FROM sia.asignacion_docente a
    WHERE a.id = NEW.id_asignacion_docente;

    IF v_id_docente IS NOT NULL AND EXISTS (
        SELECT 1 FROM sia.horario_clase h
        JOIN sia.asignacion_docente a2 ON a2.id = h.id_asignacion_docente
        WHERE h.id <> NEW.id
          AND h.id_institucion = NEW.id_institucion
          AND a2.id_docente   = v_id_docente
          AND h.dia_semana    = NEW.dia_semana
          AND h.estado        = 'ACTIVO'
          AND h.hora_inicio   < NEW.hora_fin
          AND h.hora_fin      > NEW.hora_inicio
    ) THEN
        RAISE EXCEPTION 'El docente ya tiene asignada otra clase en el mismo día y franja horaria';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validar_solapamiento_horario
    BEFORE INSERT OR UPDATE ON sia.horario_clase
    FOR EACH ROW EXECUTE FUNCTION sia.fn_validar_solapamiento_horario();

-- =========================================================
-- 11. SEGURIDAD — recuperación de contraseña
-- =========================================================

CREATE TABLE password_recovery_challenge (
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

CREATE INDEX idx_password_recovery_usuario ON password_recovery_challenge (id_usuario);
CREATE INDEX idx_password_recovery_expira_en ON password_recovery_challenge (expira_en DESC);

-- =========================================================
-- 12. CALIFICACIONES TRIMESTRALES — Sprint 2 (HU-S2-18)
--     Modelo Bolivia: SER / SABER / HACER / AUTOEVALUACIÓN
-- =========================================================

CREATE TABLE periodo_trimestral (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_gestion_academica UUID NOT NULL REFERENCES gestion_academica(id) ON DELETE CASCADE,
    numero_trimestre INTEGER NOT NULL CHECK (numero_trimestre BETWEEN 1 AND 3),
    estado VARCHAR(20) NOT NULL DEFAULT 'ABIERTO' CHECK (estado IN ('ABIERTO','EN_CIERRE','CERRADO','REABIERTO')),
    fecha_cierre TIMESTAMPTZ NULL,
    justificacion_cierre VARCHAR(500) NULL,
    id_usuario_cierre UUID NULL REFERENCES usuario(id) ON DELETE SET NULL,
    fecha_reapertura TIMESTAMPTZ NULL,
    justificacion_reapertura VARCHAR(500) NULL,
    id_usuario_reapertura UUID NULL REFERENCES usuario(id) ON DELETE SET NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_periodo_trimestral_gestion_numero UNIQUE (id_institucion, id_gestion_academica, numero_trimestre)
);

CREATE TABLE actividad_evaluativa (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_periodo_trimestral UUID NOT NULL REFERENCES periodo_trimestral(id) ON DELETE CASCADE,
    id_gestion_academica UUID NOT NULL REFERENCES gestion_academica(id) ON DELETE CASCADE,
    id_curso UUID NOT NULL REFERENCES curso(id) ON DELETE CASCADE,
    id_paralelo UUID NOT NULL REFERENCES paralelo(id) ON DELETE CASCADE,
    id_materia UUID NOT NULL REFERENCES materia(id) ON DELETE CASCADE,
    id_docente UUID NOT NULL REFERENCES docente(id) ON DELETE CASCADE,
    nombre_actividad VARCHAR(150) NOT NULL,
    tipo_actividad VARCHAR(30) NOT NULL,
    dimension VARCHAR(15) NOT NULL CHECK (dimension IN ('SABER','HACER')),
    puntaje_maximo INTEGER NOT NULL CHECK (puntaje_maximo IN (40,45)),
    fecha_actividad TIMESTAMPTZ NOT NULL,
    descripcion VARCHAR(1000) NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'BORRADOR' CHECK (estado IN ('BORRADOR','PUBLICADA','CERRADA')),
    publicado_en TIMESTAMPTZ NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_actividad_evaluativa_periodo_nombre UNIQUE (id_institucion, id_periodo_trimestral, nombre_actividad)
);

CREATE TABLE calificacion_actividad (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_actividad UUID NOT NULL REFERENCES actividad_evaluativa(id) ON DELETE CASCADE,
    id_estudiante UUID NOT NULL REFERENCES estudiante(id) ON DELETE CASCADE,
    nota_obtenida NUMERIC(5,2) NULL,
    observacion VARCHAR(500) NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE','REGISTRADA','PUBLICADA','MODIFICADA')),
    id_usuario_registro UUID NULL REFERENCES usuario(id) ON DELETE SET NULL,
    id_usuario_modificacion UUID NULL REFERENCES usuario(id) ON DELETE SET NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_calificacion_actividad_estudiante UNIQUE (id_actividad, id_estudiante)
);

CREATE TABLE calificacion_ser (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_trimestre UUID NOT NULL REFERENCES periodo_trimestral(id) ON DELETE CASCADE,
    id_gestion_academica UUID NOT NULL REFERENCES gestion_academica(id) ON DELETE CASCADE,
    id_curso UUID NOT NULL REFERENCES curso(id) ON DELETE CASCADE,
    id_paralelo UUID NOT NULL REFERENCES paralelo(id) ON DELETE CASCADE,
    id_materia UUID NOT NULL REFERENCES materia(id) ON DELETE CASCADE,
    id_docente UUID NOT NULL REFERENCES docente(id) ON DELETE CASCADE,
    id_estudiante UUID NOT NULL REFERENCES estudiante(id) ON DELETE CASCADE,
    nota_ser NUMERIC(5,2) NOT NULL CHECK (nota_ser BETWEEN 0 AND 10),
    observacion VARCHAR(500) NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'REGISTRADA' CHECK (estado IN ('PENDIENTE','REGISTRADA','PUBLICADA','MODIFICADA')),
    id_usuario_registro UUID NULL REFERENCES usuario(id) ON DELETE SET NULL,
    id_usuario_modificacion UUID NULL REFERENCES usuario(id) ON DELETE SET NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_calificacion_ser UNIQUE (id_estudiante, id_materia, id_trimestre, id_gestion_academica)
);

CREATE TABLE autoevaluacion_trimestral (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_trimestre UUID NOT NULL REFERENCES periodo_trimestral(id) ON DELETE CASCADE,
    id_gestion_academica UUID NOT NULL REFERENCES gestion_academica(id) ON DELETE CASCADE,
    id_materia UUID NOT NULL REFERENCES materia(id) ON DELETE CASCADE,
    id_estudiante UUID NOT NULL REFERENCES estudiante(id) ON DELETE CASCADE,
    nota_autoevaluacion NUMERIC(5,2) NOT NULL CHECK (nota_autoevaluacion BETWEEN 0 AND 5),
    comentario VARCHAR(1000) NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE','REGISTRADA','PUBLICADA')),
    id_usuario_registro UUID NULL REFERENCES usuario(id) ON DELETE SET NULL,
    id_usuario_modificacion UUID NULL REFERENCES usuario(id) ON DELETE SET NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_autoevaluacion_trimestral UNIQUE (id_estudiante, id_materia, id_trimestre, id_gestion_academica)
);

CREATE INDEX idx_periodo_trimestral_institucion_gestion
    ON periodo_trimestral (id_institucion, id_gestion_academica, numero_trimestre);
CREATE INDEX idx_actividad_evaluativa_periodo_dimension
    ON actividad_evaluativa (id_institucion, id_periodo_trimestral, dimension, estado);
CREATE INDEX idx_calificacion_actividad_actividad
    ON calificacion_actividad (id_actividad, id_estudiante);
CREATE INDEX idx_calificacion_ser_periodo
    ON calificacion_ser (id_institucion, id_gestion_academica, id_trimestre, id_materia);
CREATE INDEX idx_autoevaluacion_trimestral_periodo
    ON autoevaluacion_trimestral (id_institucion, id_gestion_academica, id_trimestre, id_materia);

-- =========================================================
-- 13. TRIGGERS DE actualizado_en
-- =========================================================

CREATE TRIGGER trg_institucion_actualizado_en BEFORE UPDATE ON institucion FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
CREATE TRIGGER trg_configuracion_institucion_actualizado_en BEFORE UPDATE ON configuracion_institucion FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
CREATE TRIGGER trg_rol_actualizado_en BEFORE UPDATE ON rol FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
CREATE TRIGGER trg_permiso_actualizado_en BEFORE UPDATE ON permiso FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
CREATE TRIGGER trg_rol_permiso_actualizado_en BEFORE UPDATE ON rol_permiso FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
CREATE TRIGGER trg_usuario_actualizado_en BEFORE UPDATE ON usuario FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
CREATE TRIGGER trg_usuario_rol_actualizado_en BEFORE UPDATE ON usuario_rol FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
CREATE TRIGGER trg_gestion_academica_actualizado_en BEFORE UPDATE ON gestion_academica FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
CREATE TRIGGER trg_curso_actualizado_en BEFORE UPDATE ON curso FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
CREATE TRIGGER trg_paralelo_actualizado_en BEFORE UPDATE ON paralelo FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
CREATE TRIGGER trg_aula_actualizado_en BEFORE UPDATE ON aula FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
CREATE TRIGGER trg_materia_actualizado_en BEFORE UPDATE ON materia FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
CREATE TRIGGER trg_curso_materia_actualizado_en BEFORE UPDATE ON curso_materia FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
CREATE TRIGGER trg_docente_actualizado_en BEFORE UPDATE ON docente FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
CREATE TRIGGER trg_estudiante_actualizado_en BEFORE UPDATE ON estudiante FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
CREATE TRIGGER trg_tutor_actualizado_en BEFORE UPDATE ON tutor FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
CREATE TRIGGER trg_estudiante_tutor_actualizado_en BEFORE UPDATE ON estudiante_tutor FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
CREATE TRIGGER trg_inscripcion_actualizado_en BEFORE UPDATE ON inscripcion FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
CREATE TRIGGER trg_asignacion_docente_actualizado_en BEFORE UPDATE ON asignacion_docente FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
CREATE TRIGGER trg_archivo_actualizado_en BEFORE UPDATE ON sia.archivo FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
CREATE TRIGGER trg_archivo_referencia_actualizado_en BEFORE UPDATE ON sia.archivo_referencia FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
CREATE TRIGGER trg_asistencia_registro_actualizado_en BEFORE UPDATE ON asistencia_registro FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
CREATE TRIGGER trg_asistencia_detalle_actualizado_en BEFORE UPDATE ON asistencia_detalle FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
CREATE TRIGGER trg_evaluacion_actualizado_en BEFORE UPDATE ON evaluacion FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
CREATE TRIGGER trg_evaluacion_materia_actualizado_en BEFORE UPDATE ON evaluacion_materia FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
CREATE TRIGGER trg_calificacion_actualizado_en BEFORE UPDATE ON calificacion FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
CREATE TRIGGER trg_horario_clase_actualizado_en BEFORE UPDATE ON sia.horario_clase FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
CREATE TRIGGER trg_periodo_trimestral_actualizado_en BEFORE UPDATE ON periodo_trimestral FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
CREATE TRIGGER trg_actividad_evaluativa_actualizado_en BEFORE UPDATE ON actividad_evaluativa FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
CREATE TRIGGER trg_calificacion_actividad_actualizado_en BEFORE UPDATE ON calificacion_actividad FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
CREATE TRIGGER trg_calificacion_ser_actualizado_en BEFORE UPDATE ON calificacion_ser FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
CREATE TRIGGER trg_autoevaluacion_trimestral_actualizado_en BEFORE UPDATE ON autoevaluacion_trimestral FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();

-- =========================================================
-- 14. DATOS INICIALES — roles, permisos
-- =========================================================

INSERT INTO permiso (codigo, nombre, modulo, accion, descripcion)
VALUES
    ('USUARIOS_READ',        'Usuarios: lectura',               'USUARIOS',         'READ',  'Permite consultar usuarios'),
    ('USUARIOS_WRITE',       'Usuarios: escritura',             'USUARIOS',         'WRITE', 'Permite crear, editar y desactivar usuarios'),
    ('CONFIGURACION_READ',   'Configuración: lectura',          'CONFIGURACION',    'READ',  'Permite consultar configuración institucional'),
    ('CONFIGURACION_WRITE',  'Configuración: escritura',        'CONFIGURACION',    'WRITE', 'Permite modificar configuración institucional'),
    ('GESTION_READ',         'Gestión académica: lectura',      'GESTION_ACADEMICA','READ',  'Permite consultar estructura académica'),
    ('GESTION_WRITE',        'Gestión académica: escritura',    'GESTION_ACADEMICA','WRITE', 'Permite modificar estructura académica'),
    ('PERSONAS_READ',        'Personas: lectura',               'PERSONAS',         'READ',  'Permite consultar docentes, estudiantes y tutores'),
    ('PERSONAS_WRITE',       'Personas: escritura',             'PERSONAS',         'WRITE', 'Permite modificar docentes, estudiantes y tutores'),
    ('OPERACION_READ',       'Operación: lectura',              'OPERACION',        'READ',  'Permite consultar inscripciones y asignaciones'),
    ('OPERACION_WRITE',      'Operación: escritura',            'OPERACION',        'WRITE', 'Permite modificar inscripciones y asignaciones'),
    ('ROLES_READ',           'Roles: lectura',                  'ROLES',            'READ',  'Permite consultar roles y permisos'),
    ('ROLES_WRITE',          'Roles: escritura',                'ROLES',            'WRITE', 'Permite crear y editar roles institucionales'),
    ('MI_AREA_READ',         'Mi área: lectura',                'MI_AREA',          'READ',  'Permite acceder al área operativa del docente'),
    ('AUDITORIA_READ',       'Auditoría: lectura',              'AUDITORIA',        'READ',  'Permite consultar la bitácora de auditoría')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id, p.id FROM rol r
JOIN permiso p ON p.codigo IN (
    'USUARIOS_READ','USUARIOS_WRITE','CONFIGURACION_READ','CONFIGURACION_WRITE',
    'GESTION_READ','GESTION_WRITE','PERSONAS_READ','PERSONAS_WRITE',
    'OPERACION_READ','OPERACION_WRITE','ROLES_READ','ROLES_WRITE','MI_AREA_READ','AUDITORIA_READ')
WHERE r.codigo = 'ADMIN_INSTITUCION'
ON CONFLICT (id_rol, id_permiso) DO NOTHING;

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id, p.id FROM rol r
JOIN permiso p ON p.codigo IN (
    'USUARIOS_READ','CONFIGURACION_READ','GESTION_READ','GESTION_WRITE',
    'PERSONAS_READ','PERSONAS_WRITE','OPERACION_READ','OPERACION_WRITE',
    'ROLES_READ','MI_AREA_READ','AUDITORIA_READ')
WHERE r.codigo = 'DIRECTOR'
ON CONFLICT (id_rol, id_permiso) DO NOTHING;

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id, p.id FROM rol r
JOIN permiso p ON p.codigo IN (
    'USUARIOS_READ','GESTION_READ','GESTION_WRITE',
    'PERSONAS_READ','PERSONAS_WRITE','OPERACION_READ','OPERACION_WRITE')
WHERE r.codigo = 'SECRETARIO'
ON CONFLICT (id_rol, id_permiso) DO NOTHING;

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id, p.id FROM rol r
JOIN permiso p ON p.codigo IN ('OPERACION_READ','MI_AREA_READ')
WHERE r.codigo = 'DOCENTE'
ON CONFLICT (id_rol, id_permiso) DO NOTHING;

-- =========================================================
-- 15. SPRINT ESPECIAL — PLANES, SaaS Y MÓDULOS
-- =========================================================

CREATE TABLE IF NOT EXISTS plan_suscripcion (
    id                     UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo                 VARCHAR(30)  NOT NULL UNIQUE,
    nombre                 VARCHAR(100) NOT NULL,
    descripcion            TEXT,
    max_usuarios           INTEGER      NOT NULL DEFAULT 10 CHECK (max_usuarios > 0),
    max_almacenamiento_mb  INTEGER      NOT NULL DEFAULT 512 CHECK (max_almacenamiento_mb > 0),
    precio_mensual         NUMERIC(10,2) NOT NULL DEFAULT 0.00 CHECK (precio_mensual >= 0),
    estado                 VARCHAR(15)  NOT NULL DEFAULT 'ACTIVO'
                               CHECK (estado IN ('ACTIVO', 'INACTIVO', 'DEPRECADO')),
    creado_en              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    actualizado_en         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS modulo_sistema (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo          VARCHAR(50) NOT NULL UNIQUE,
    nombre          VARCHAR(120) NOT NULL,
    descripcion     TEXT,
    icono           VARCHAR(60),
    ruta_frontend   VARCHAR(120),
    orden_visual    INTEGER     NOT NULL DEFAULT 0,
    estado          VARCHAR(15) NOT NULL DEFAULT 'ACTIVO'
                        CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS plan_modulo (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    id_plan         UUID        NOT NULL REFERENCES plan_suscripcion(id) ON DELETE CASCADE,
    id_modulo       UUID        NOT NULL REFERENCES modulo_sistema(id) ON DELETE CASCADE,
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_plan_modulo UNIQUE (id_plan, id_modulo)
);

CREATE TABLE IF NOT EXISTS suscripcion_institucion (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion  UUID        NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_plan         UUID        NOT NULL REFERENCES plan_suscripcion(id),
    fecha_inicio    DATE        NOT NULL DEFAULT CURRENT_DATE,
    fecha_fin       DATE        NULL,
    estado          VARCHAR(20) NOT NULL DEFAULT 'ACTIVA'
                        CHECK (estado IN ('ACTIVA', 'VENCIDA', 'CANCELADA', 'SUSPENDIDA')),
    simulada        BOOLEAN     NOT NULL DEFAULT TRUE,
    observacion     TEXT,
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_suscripcion_fechas CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_suscripcion_activa_por_institucion
    ON suscripcion_institucion (id_institucion)
    WHERE estado = 'ACTIVA';

CREATE INDEX IF NOT EXISTS idx_suscripcion_institucion ON suscripcion_institucion (id_institucion);
CREATE INDEX IF NOT EXISTS idx_suscripcion_plan        ON suscripcion_institucion (id_plan);
CREATE INDEX IF NOT EXISTS idx_suscripcion_estado      ON suscripcion_institucion (estado);
CREATE INDEX IF NOT EXISTS idx_plan_suscripcion_estado ON plan_suscripcion (estado);

CREATE TABLE IF NOT EXISTS privilegio_ui (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion  UUID        NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_rol          UUID        NOT NULL REFERENCES rol(id) ON DELETE CASCADE,
    modulo          VARCHAR(60) NOT NULL,
    entidad         VARCHAR(60) NOT NULL,
    campo           VARCHAR(100) NOT NULL,
    visibilidad     VARCHAR(20) NOT NULL DEFAULT 'VISIBLE'
                        CHECK (visibilidad IN ('VISIBLE', 'OCULTO')),
    edicion         VARCHAR(20) NOT NULL DEFAULT 'EDITABLE'
                        CHECK (edicion IN ('EDITABLE', 'SOLO_LECTURA', 'OCULTO')),
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_privilegio_ui UNIQUE (id_institucion, id_rol, modulo, entidad, campo)
);

CREATE INDEX IF NOT EXISTS idx_privilegio_ui_rol    ON privilegio_ui (id_institucion, id_rol);
CREATE INDEX IF NOT EXISTS idx_privilegio_ui_modulo ON privilegio_ui (id_institucion, modulo, entidad);

CREATE TABLE IF NOT EXISTS intento_login (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    correo          CITEXT      NOT NULL,
    id_usuario      UUID        NULL REFERENCES usuario(id) ON DELETE SET NULL,
    id_institucion  UUID        NULL REFERENCES institucion(id) ON DELETE SET NULL,
    fecha_intento   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    exito           BOOLEAN     NOT NULL,
    ip              VARCHAR(50),
    agente_usuario  TEXT,
    motivo_fallo    VARCHAR(60) NULL
                        CHECK (motivo_fallo IN (
                            'CREDENCIALES_INVALIDAS','CUENTA_BLOQUEADA','CUENTA_INACTIVA',
                            'INSTITUCION_INACTIVA','TOKEN_EXPIRADO','OTRO'
                        )),
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_intento_login_correo   ON intento_login (correo);
CREATE INDEX IF NOT EXISTS idx_intento_login_fecha    ON intento_login (fecha_intento DESC);
CREATE INDEX IF NOT EXISTS idx_intento_login_usuario  ON intento_login (id_usuario) WHERE id_usuario IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_intento_login_fallidos ON intento_login (correo, fecha_intento DESC) WHERE exito = FALSE;

CREATE TABLE IF NOT EXISTS registro_respaldo (
    id                    UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion        UUID        NULL REFERENCES institucion(id) ON DELETE SET NULL,
    tipo_respaldo         VARCHAR(20) NOT NULL DEFAULT 'COMPLETO'
                              CHECK (tipo_respaldo IN ('COMPLETO', 'INCREMENTAL', 'POR_TENANT', 'ARCHIVOS')),
    estado                VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE'
                              CHECK (estado IN ('PENDIENTE', 'EN_PROGRESO', 'COMPLETADO', 'FALLIDO')),
    iniciado_por          UUID        NULL REFERENCES usuario(id) ON DELETE SET NULL,
    fecha_inicio          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_fin             TIMESTAMPTZ NULL,
    ruta_almacenamiento   VARCHAR(500),
    tamanio_bytes         BIGINT,
    observacion           TEXT,
    simulado              BOOLEAN     NOT NULL DEFAULT TRUE,
    creado_en             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS registro_restauracion (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    id_respaldo      UUID        NOT NULL REFERENCES registro_respaldo(id) ON DELETE RESTRICT,
    id_institucion   UUID        NULL REFERENCES institucion(id) ON DELETE SET NULL,
    solicitado_por   UUID        NULL REFERENCES usuario(id) ON DELETE SET NULL,
    aprobado_por     UUID        NULL REFERENCES usuario(id) ON DELETE SET NULL,
    fecha_solicitud  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_ejecucion  TIMESTAMPTZ NULL,
    estado           VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE'
                         CHECK (estado IN ('PENDIENTE','APROBADO','EN_PROGRESO','COMPLETADO','RECHAZADO','FALLIDO')),
    motivo           TEXT        NOT NULL,
    observacion      TEXT,
    simulado         BOOLEAN     NOT NULL DEFAULT TRUE,
    creado_en        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_respaldo_institucion     ON registro_respaldo (id_institucion);
CREATE INDEX IF NOT EXISTS idx_respaldo_estado          ON registro_respaldo (estado);
CREATE INDEX IF NOT EXISTS idx_respaldo_fecha           ON registro_respaldo (fecha_inicio DESC);
CREATE INDEX IF NOT EXISTS idx_restauracion_respaldo    ON registro_restauracion (id_respaldo);
CREATE INDEX IF NOT EXISTS idx_restauracion_estado      ON registro_restauracion (estado);
CREATE INDEX IF NOT EXISTS idx_restauracion_institucion ON registro_restauracion (id_institucion);

CREATE TABLE IF NOT EXISTS reporte_configurable (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID        NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    codigo         VARCHAR(60) NOT NULL,
    nombre         VARCHAR(150) NOT NULL,
    descripcion    TEXT,
    entidad_base   VARCHAR(60) NOT NULL,
    tipo_reporte   VARCHAR(20) NOT NULL DEFAULT 'ANALITICO'
                       CHECK (tipo_reporte IN ('ANALITICO', 'GERENCIAL', 'DINAMICO')),
    es_global      BOOLEAN     NOT NULL DEFAULT FALSE,
    creado_por     UUID        NULL REFERENCES usuario(id) ON DELETE SET NULL,
    estado         VARCHAR(15) NOT NULL DEFAULT 'ACTIVO'
                       CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    creado_en      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_reporte_configurable UNIQUE (id_institucion, codigo)
);

CREATE TABLE IF NOT EXISTS reporte_campo (
    id                     UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    id_reporte             UUID        NOT NULL REFERENCES reporte_configurable(id) ON DELETE CASCADE,
    nombre_campo           VARCHAR(100) NOT NULL,
    etiqueta               VARCHAR(150) NOT NULL,
    tipo_dato              VARCHAR(20)  NOT NULL DEFAULT 'TEXTO'
                               CHECK (tipo_dato IN ('TEXTO', 'NUMERO', 'FECHA', 'BOOLEANO', 'ENUM')),
    es_filtro              BOOLEAN     NOT NULL DEFAULT FALSE,
    es_visible_por_defecto BOOLEAN     NOT NULL DEFAULT TRUE,
    es_ordenable           BOOLEAN     NOT NULL DEFAULT FALSE,
    es_agrupable           BOOLEAN     NOT NULL DEFAULT FALSE,
    orden_visual           INTEGER     NOT NULL DEFAULT 0,
    creado_en              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_reporte_campo UNIQUE (id_reporte, nombre_campo)
);

CREATE TABLE IF NOT EXISTS bitacora_reporte (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion    UUID        NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_reporte        UUID        NOT NULL REFERENCES reporte_configurable(id) ON DELETE CASCADE,
    id_usuario        UUID        NULL REFERENCES usuario(id) ON DELETE SET NULL,
    fecha_generacion  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    filtros_aplicados JSONB,
    cantidad_filas    INTEGER,
    formato           VARCHAR(10) NOT NULL DEFAULT 'JSON'
                          CHECK (formato IN ('JSON', 'PDF', 'EXCEL', 'CSV')),
    exito             BOOLEAN     NOT NULL DEFAULT TRUE,
    mensaje_error     TEXT,
    creado_en         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_reporte_configurable_institucion ON reporte_configurable (id_institucion);
CREATE INDEX IF NOT EXISTS idx_reporte_configurable_tipo        ON reporte_configurable (tipo_reporte);
CREATE INDEX IF NOT EXISTS idx_reporte_configurable_estado      ON reporte_configurable (id_institucion, estado);
CREATE INDEX IF NOT EXISTS idx_reporte_campo_reporte            ON reporte_campo (id_reporte);
CREATE INDEX IF NOT EXISTS idx_bitacora_reporte_institucion     ON bitacora_reporte (id_institucion);
CREATE INDEX IF NOT EXISTS idx_bitacora_reporte_fecha           ON bitacora_reporte (fecha_generacion DESC);
CREATE INDEX IF NOT EXISTS idx_bitacora_reporte_usuario         ON bitacora_reporte (id_usuario);

CREATE TABLE IF NOT EXISTS solicitud_onboarding (
    id                      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre_institucion      VARCHAR(200) NOT NULL,
    tipo_institucion        VARCHAR(20)  NOT NULL DEFAULT 'PRIVADO'
                                CHECK (tipo_institucion IN ('FISCAL', 'CONVENIO', 'PRIVADO')),
    telefono_institucion    VARCHAR(30),
    correo_institucion      CITEXT,
    direccion_institucion   VARCHAR(255),
    nombres_contacto        VARCHAR(120) NOT NULL,
    apellidos_contacto      VARCHAR(120) NOT NULL,
    correo_contacto         CITEXT       NOT NULL,
    telefono_contacto       VARCHAR(30),
    id_plan                 UUID         NOT NULL REFERENCES plan_suscripcion(id),
    mensaje                 TEXT,
    estado                  VARCHAR(30)  NOT NULL DEFAULT 'PENDIENTE_REVISION'
                                CHECK (estado IN (
                                    'PENDIENTE_REVISION','APROBADA','PENDIENTE_PAGO',
                                    'PAGADO','ACTIVA','RECHAZADA'
                                )),
    notas_admin             TEXT,
    id_institucion_creada   UUID         NULL REFERENCES institucion(id) ON DELETE SET NULL,
    id_usuario_creado       UUID         NULL REFERENCES usuario(id) ON DELETE SET NULL,
    creado_en               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    actualizado_en          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_solicitud_onboarding_estado  ON solicitud_onboarding (estado);
CREATE INDEX IF NOT EXISTS idx_solicitud_onboarding_correo  ON solicitud_onboarding (correo_contacto);
CREATE INDEX IF NOT EXISTS idx_solicitud_onboarding_plan    ON solicitud_onboarding (id_plan);
CREATE INDEX IF NOT EXISTS idx_solicitud_onboarding_inst    ON solicitud_onboarding (id_institucion_creada)
    WHERE id_institucion_creada IS NOT NULL;

CREATE OR REPLACE TRIGGER trg_plan_suscripcion_actualizado_en
    BEFORE UPDATE ON plan_suscripcion FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
CREATE OR REPLACE TRIGGER trg_modulo_sistema_actualizado_en
    BEFORE UPDATE ON modulo_sistema FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
CREATE OR REPLACE TRIGGER trg_plan_modulo_actualizado_en
    BEFORE UPDATE ON plan_modulo FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
CREATE OR REPLACE TRIGGER trg_suscripcion_institucion_actualizado_en
    BEFORE UPDATE ON suscripcion_institucion FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
CREATE OR REPLACE TRIGGER trg_privilegio_ui_actualizado_en
    BEFORE UPDATE ON privilegio_ui FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
CREATE OR REPLACE TRIGGER trg_registro_respaldo_actualizado_en
    BEFORE UPDATE ON registro_respaldo FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
CREATE OR REPLACE TRIGGER trg_registro_restauracion_actualizado_en
    BEFORE UPDATE ON registro_restauracion FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
CREATE OR REPLACE TRIGGER trg_reporte_configurable_actualizado_en
    BEFORE UPDATE ON reporte_configurable FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
CREATE OR REPLACE TRIGGER trg_reporte_campo_actualizado_en
    BEFORE UPDATE ON reporte_campo FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();
CREATE OR REPLACE TRIGGER trg_solicitud_onboarding_actualizado_en
    BEFORE UPDATE ON solicitud_onboarding FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();

-- =========================================================
-- 16. DATOS SEMILLA — Planes y módulos del sistema
-- =========================================================

INSERT INTO plan_suscripcion (codigo, nombre, descripcion, max_usuarios, max_almacenamiento_mb, precio_mensual, estado)
VALUES
    ('BASICO',       'Plan Básico',       'Hasta 10 usuarios. Módulos de identidad, estructura académica y operación básica.', 10, 512, 0.00, 'ACTIVO'),
    ('PROFESIONAL',  'Plan Profesional',  'Hasta 50 usuarios. Todos los módulos Sprint 1-2: asistencia, calificaciones y horarios.', 50, 2048, 150.00, 'ACTIVO'),
    ('EMPRESARIAL',  'Plan Empresarial',  'Usuarios ilimitados. Incluye reportes avanzados, auditoría completa y gestión de respaldos.', 999, 10240, 350.00, 'ACTIVO')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO modulo_sistema (codigo, nombre, descripcion, ruta_frontend, orden_visual)
VALUES
    ('IDENTIDAD',    'Identidad y usuarios',   'Gestión de usuarios, roles y permisos',           '/usuarios',       1),
    ('ESTRUCTURA',   'Estructura académica',   'Cursos, paralelos, materias y gestión académica', '/cursos',         2),
    ('OPERACION',    'Operación académica',    'Inscripciones, asignaciones docentes',            '/inscripciones',  3),
    ('ASISTENCIA',   'Asistencia',             'Registro y consulta de asistencia por sesión',    '/asistencia',     4),
    ('CALIFICACION', 'Calificaciones',         'Registro y consulta de calificaciones y notas',   '/calificaciones', 5),
    ('HORARIOS',     'Horarios',               'Programación semanal de clases por paralelo',     '/horarios',       6),
    ('REPORTES',     'Reportes',               'Reportes analíticos, gerenciales y dinámicos',    '/reportes',       7),
    ('AUDITORIA',    'Auditoría',              'Bitácora de operaciones y actividad de usuarios', '/auditoria',      8),
    ('RESPALDOS',    'Respaldos',              'Gestión de copias de seguridad y restauraciones', '/backups',        9)
ON CONFLICT (codigo) DO NOTHING;

DO $$
DECLARE
    v_plan_basico UUID; v_plan_pro UUID; v_plan_emp UUID;
    v_mod_id UUID; v_codigo TEXT;
    v_modulos_basico TEXT[] := ARRAY['IDENTIDAD', 'ESTRUCTURA', 'OPERACION'];
    v_modulos_pro    TEXT[] := ARRAY['IDENTIDAD', 'ESTRUCTURA', 'OPERACION', 'ASISTENCIA', 'CALIFICACION', 'HORARIOS', 'REPORTES'];
    v_modulos_emp    TEXT[] := ARRAY['IDENTIDAD', 'ESTRUCTURA', 'OPERACION', 'ASISTENCIA', 'CALIFICACION', 'HORARIOS', 'REPORTES', 'AUDITORIA', 'RESPALDOS'];
BEGIN
    SELECT id INTO v_plan_basico FROM plan_suscripcion WHERE codigo = 'BASICO';
    SELECT id INTO v_plan_pro    FROM plan_suscripcion WHERE codigo = 'PROFESIONAL';
    SELECT id INTO v_plan_emp    FROM plan_suscripcion WHERE codigo = 'EMPRESARIAL';
    FOREACH v_codigo IN ARRAY v_modulos_basico LOOP
        SELECT id INTO v_mod_id FROM modulo_sistema WHERE codigo = v_codigo;
        INSERT INTO plan_modulo (id_plan, id_modulo) VALUES (v_plan_basico, v_mod_id) ON CONFLICT DO NOTHING;
    END LOOP;
    FOREACH v_codigo IN ARRAY v_modulos_pro LOOP
        SELECT id INTO v_mod_id FROM modulo_sistema WHERE codigo = v_codigo;
        INSERT INTO plan_modulo (id_plan, id_modulo) VALUES (v_plan_pro, v_mod_id) ON CONFLICT DO NOTHING;
    END LOOP;
    FOREACH v_codigo IN ARRAY v_modulos_emp LOOP
        SELECT id INTO v_mod_id FROM modulo_sistema WHERE codigo = v_codigo;
        INSERT INTO plan_modulo (id_plan, id_modulo) VALUES (v_plan_emp, v_mod_id) ON CONFLICT DO NOTHING;
    END LOOP;
END $$;

DO $$
DECLARE v_inst_id UUID; v_plan_id UUID;
BEGIN
    SELECT id INTO v_inst_id FROM institucion ORDER BY creado_en LIMIT 1;
    SELECT id INTO v_plan_id FROM plan_suscripcion WHERE codigo = 'PROFESIONAL';
    IF v_inst_id IS NOT NULL AND v_plan_id IS NOT NULL THEN
        INSERT INTO suscripcion_institucion (id_institucion, id_plan, fecha_inicio, estado, simulada, observacion)
        VALUES (v_inst_id, v_plan_id, CURRENT_DATE, 'ACTIVA', TRUE, 'Suscripción demo creada en script inicial')
        ON CONFLICT DO NOTHING;
    END IF;
END $$;

-- =========================================================
-- FIN DEL SCRIPT
-- =========================================================

INSERT INTO rol (codigo, nombre, descripcion, es_global) VALUES
('SUPER_ADMIN',       'Super Administrador',          'Administrador global del sistema',             TRUE),
('ADMIN_INSTITUCION', 'Administrador de Institución', 'Administrador principal de la institución',   TRUE),
('DIRECTOR',          'Director',                     'Dirección institucional',                      TRUE),
('SECRETARIO',        'Secretario',                   'Gestión operativa académica y administrativa', TRUE),
('DOCENTE',           'Docente',                      'Docente del sistema',                          TRUE),
('ESTUDIANTE',        'Estudiante',                   'Estudiante del sistema',                       TRUE),
('TUTOR',             'Tutor',                        'Tutor/apoderado de estudiante',                TRUE);

INSERT INTO permiso (codigo, nombre, modulo, accion, descripcion) VALUES
('USUARIOS_READ',       'Usuarios: lectura',           'USUARIOS',          'READ',  'Permite consultar usuarios'),
('USUARIOS_WRITE',      'Usuarios: escritura',          'USUARIOS',          'WRITE', 'Permite crear, editar y desactivar usuarios'),
('CONFIGURACION_READ',  'Configuración: lectura',       'CONFIGURACION',     'READ',  'Permite consultar configuración institucional'),
('CONFIGURACION_WRITE', 'Configuración: escritura',     'CONFIGURACION',     'WRITE', 'Permite modificar configuración institucional'),
('GESTION_READ',        'Gestión académica: lectura',   'GESTION_ACADEMICA', 'READ',  'Permite consultar estructura académica'),
('GESTION_WRITE',       'Gestión académica: escritura', 'GESTION_ACADEMICA', 'WRITE', 'Permite modificar estructura académica'),
('PERSONAS_READ',       'Personas: lectura',            'PERSONAS',          'READ',  'Permite consultar docentes, estudiantes y tutores'),
('PERSONAS_WRITE',      'Personas: escritura',          'PERSONAS',          'WRITE', 'Permite modificar docentes, estudiantes y tutores'),
('OPERACION_READ',      'Operación: lectura',           'OPERACION',         'READ',  'Permite consultar inscripciones y asignaciones'),
('OPERACION_WRITE',     'Operación: escritura',         'OPERACION',         'WRITE', 'Permite modificar inscripciones y asignaciones'),
('ROLES_READ',          'Roles: lectura',               'ROLES',             'READ',  'Permite consultar roles y permisos'),
('ROLES_WRITE',         'Roles: escritura',             'ROLES',             'WRITE', 'Permite crear y editar roles institucionales'),
('MI_AREA_READ',        'Mi área: lectura',             'MI_AREA',           'READ',  'Permite acceder al área operativa del docente'),
('AUDITORIA_READ',      'Auditoría: lectura',           'AUDITORIA',         'READ',  'Permite consultar la bitácora de auditoría');

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id, p.id FROM rol r
JOIN permiso p ON p.codigo IN (
    'USUARIOS_READ','USUARIOS_WRITE','CONFIGURACION_READ','CONFIGURACION_WRITE',
    'GESTION_READ','GESTION_WRITE','PERSONAS_READ','PERSONAS_WRITE',
    'OPERACION_READ','OPERACION_WRITE','ROLES_READ','ROLES_WRITE','MI_AREA_READ','AUDITORIA_READ')
WHERE r.codigo = 'ADMIN_INSTITUCION';

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id, p.id FROM rol r
JOIN permiso p ON p.codigo IN (
    'USUARIOS_READ','CONFIGURACION_READ','GESTION_READ','GESTION_WRITE',
    'PERSONAS_READ','PERSONAS_WRITE','OPERACION_READ','OPERACION_WRITE',
    'ROLES_READ','MI_AREA_READ','AUDITORIA_READ')
WHERE r.codigo = 'DIRECTOR';

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id, p.id FROM rol r
JOIN permiso p ON p.codigo IN (
    'USUARIOS_READ','GESTION_READ','GESTION_WRITE',
    'PERSONAS_READ','PERSONAS_WRITE','OPERACION_READ','OPERACION_WRITE')
WHERE r.codigo = 'SECRETARIO';

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id, p.id FROM rol r
JOIN permiso p ON p.codigo IN ('OPERACION_READ','MI_AREA_READ')
WHERE r.codigo = 'DOCENTE';

INSERT INTO institucion (codigo, nombre, tipo_institucion, telefono, correo, direccion)
VALUES ('UEM-001', 'Unidad Educativa Modelo', 'PRIVADO', '70000000', 'contacto@uemodelo.edu.bo', 'Santa Cruz - Bolivia');

INSERT INTO configuracion_institucion (id_institucion, clave, valor, tipo_valor, descripcion)
SELECT id, 'ESCALA_CALIFICACION', '100', 'NUMERO', 'Escala máxima de calificación'
FROM institucion WHERE codigo = 'UEM-001';

INSERT INTO configuracion_institucion (id_institucion, clave, valor, tipo_valor, descripcion)
SELECT id, 'NOTA_MINIMA_APROBACION', '51', 'NUMERO', 'Nota mínima de aprobación'
FROM institucion WHERE codigo = 'UEM-001';

INSERT INTO configuracion_institucion (id_institucion, clave, valor, tipo_valor, descripcion)
SELECT id, 'NOMBRE_PERIODO_ACADEMICO', 'GESTIÓN', 'TEXTO', 'Nombre visible del periodo académico'
FROM institucion WHERE codigo = 'UEM-001';

-- =========================================================
-- HISTORIAL DE CAMBIOS
-- =========================================================
-- v3.0  Sprint 1 inicial.
-- v3.1  Subsistema de archivos S3.
-- v3.2  API: URLs pre-firmadas + DELETE configuración.
-- v4.0  Sprint 2 consolidado: aulas, RBAC fino, asistencia, calificaciones,
--       horarios (con trigger anti-solapamiento), seguridad/recuperación,
--       auditoría enriquecida. FKs compuestas multi-tenant aplicadas.
-- =========================================================
