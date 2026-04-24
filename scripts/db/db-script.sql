-- =========================================================
-- SPRINT 1 - BASE DE DATOS (VERSION 3.1)
-- Sistema de Gestión Académica SaaS
-- PostgreSQL
-- Schema: sia (Sistema de Información Académico)
-- =========================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "citext";

-- Crear schema SIA y establecerlo como activo
CREATE SCHEMA IF NOT EXISTS sia;
SET search_path TO sia, public;

-- =========================================================
-- FUNCIÓN GENERAL PARA actualizado_en
-- =========================================================

CREATE OR REPLACE FUNCTION fn_actualizar_actualizado_en()
RETURNS TRIGGER AS $$
BEGIN
    NEW.actualizado_en = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =========================================================
-- 1. INSTITUCIÓN Y CONFIGURACIÓN
-- =========================================================
-- TABLAS BASE: INSTITUCIÓN / SEGURIDAD / PERMISOS

CREATE TABLE institucion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(200) NOT NULL,
    tipo_institucion VARCHAR(20) NOT NULL CHECK (tipo_institucion IN ('FISCAL', 'CONVENIO', 'PRIVADO')),
    telefono VARCHAR(30),
    correo CITEXT,
    direccion VARCHAR(255),
    -- logo_url eliminado: usar archivo_referencia con tipo_referencia = 'LOGO'
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
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion VARCHAR(255),
    es_global BOOLEAN NOT NULL DEFAULT FALSE,
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
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
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE usuario_rol (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_usuario UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    id_rol UUID NOT NULL REFERENCES rol(id),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_usuario_rol UNIQUE (id_usuario, id_rol)
);

-- Solo un rol activo por usuario (multirol disponible en el futuro)
CREATE UNIQUE INDEX uq_usuario_rol_activo_unico
    ON usuario_rol (id_usuario)
    WHERE activo = TRUE;

-- =========================================================
-- 3. ESTRUCTURA ACADÉMICA
-- =========================================================
-- TABLAS ACADÉMICAS BASE

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

-- Solo una gestión activa por institución
CREATE UNIQUE INDEX uq_gestion_activa_por_institucion
    ON gestion_academica (id_institucion)
    WHERE activa = TRUE;

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
-- PERSONAS ACADÉMICAS
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

-- Solo un tutor principal activo por estudiante
CREATE UNIQUE INDEX uq_estudiante_tutor_principal
    ON estudiante_tutor (id_institucion, id_estudiante)
    WHERE es_principal = TRUE AND estado = 'ACTIVO';

-- =========================================================
-- 5. OPERACIÓN ACADÉMICA DEL SPRINT 1
-- =========================================================
-- OPERACIÓN ACADÉMICA DEL SPRINT 1

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

-- Índice para evitar duplicados activos del mismo estudiante en la misma gestión
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
-- BITÁCORA / CAJA NEGRA
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
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_bitacora_institucion ON bitacora_auditoria (id_institucion);
CREATE INDEX idx_bitacora_usuario ON bitacora_auditoria (id_usuario);
CREATE INDEX idx_bitacora_fecha ON bitacora_auditoria (fecha_evento DESC);
CREATE INDEX idx_bitacora_modulo ON bitacora_auditoria (nombre_modulo);

-- =========================================================
-- ÍNDICES DE APOYO
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
-- SUBSISTEMA DE ARCHIVOS / S3
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
        categoria IN ('IMAGEN', 'DOCUMENTO', 'EVIDENCIA', 'ADJUNTO', 'OTRO')
    ),
    visibilidad VARCHAR(20) NOT NULL DEFAULT 'PRIVADO' CHECK (
        visibilidad IN ('PRIVADO', 'PUBLICO', 'FIRMADO')
    ),
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVO' CHECK (
        estado IN ('ACTIVO', 'ELIMINADO')
    ),

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
        tipo_referencia IN ('LOGO', 'FOTO_PERFIL', 'EVIDENCIA', 'DOCUMENTO', 'ADJUNTO')
    ),

    es_principal BOOLEAN NOT NULL DEFAULT FALSE,
    orden_visual INTEGER,
    observacion VARCHAR(255),
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVO' CHECK (
        estado IN ('ACTIVO', 'INACTIVO')
    ),

    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_archivo_referencia_archivo_institucion
        FOREIGN KEY (id_archivo, id_institucion) REFERENCES sia.archivo (id, id_institucion) ON DELETE CASCADE
);

-- Índices de archivo
CREATE INDEX idx_archivo_institucion ON sia.archivo(id_institucion);
CREATE INDEX idx_archivo_usuario_subio ON sia.archivo(id_usuario_subio);
CREATE INDEX idx_archivo_categoria ON sia.archivo(categoria);
CREATE INDEX idx_archivo_estado ON sia.archivo(estado);
CREATE INDEX idx_archivo_key_s3 ON sia.archivo(key_s3);

-- Índices de archivo_referencia
CREATE INDEX idx_archivo_referencia_institucion ON sia.archivo_referencia(id_institucion);
CREATE INDEX idx_archivo_referencia_archivo ON sia.archivo_referencia(id_archivo);
CREATE INDEX idx_archivo_referencia_entidad ON sia.archivo_referencia(modulo, entidad, id_entidad);
CREATE INDEX idx_archivo_referencia_tipo ON sia.archivo_referencia(tipo_referencia);

-- Un mismo archivo no se repite activo sobre la misma entidad con el mismo tipo
CREATE UNIQUE INDEX uq_archivo_referencia_activa
    ON sia.archivo_referencia(id_institucion, id_archivo, modulo, entidad, id_entidad, tipo_referencia)
    WHERE estado = 'ACTIVO';

-- Solo un archivo principal activo por tipo y entidad
CREATE UNIQUE INDEX uq_archivo_referencia_principal
    ON sia.archivo_referencia(id_institucion, modulo, entidad, id_entidad, tipo_referencia)
    WHERE es_principal = TRUE AND estado = 'ACTIVO';

-- =========================================================
-- 8. TRIGGERS DE actualizado_en
-- =========================================================

CREATE TRIGGER trg_institucion_actualizado_en
    BEFORE UPDATE ON institucion
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();

CREATE TRIGGER trg_configuracion_institucion_actualizado_en
    BEFORE UPDATE ON configuracion_institucion
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();

CREATE TRIGGER trg_rol_actualizado_en
    BEFORE UPDATE ON rol
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();

CREATE TRIGGER trg_usuario_actualizado_en
    BEFORE UPDATE ON usuario
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();

CREATE TRIGGER trg_usuario_rol_actualizado_en
    BEFORE UPDATE ON usuario_rol
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();

CREATE TRIGGER trg_gestion_academica_actualizado_en
    BEFORE UPDATE ON gestion_academica
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();

CREATE TRIGGER trg_curso_actualizado_en
    BEFORE UPDATE ON curso
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();

CREATE TRIGGER trg_paralelo_actualizado_en
    BEFORE UPDATE ON paralelo
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();

CREATE TRIGGER trg_materia_actualizado_en
    BEFORE UPDATE ON materia
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();

CREATE TRIGGER trg_curso_materia_actualizado_en
    BEFORE UPDATE ON curso_materia
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();

CREATE TRIGGER trg_docente_actualizado_en
    BEFORE UPDATE ON docente
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();

CREATE TRIGGER trg_estudiante_actualizado_en
    BEFORE UPDATE ON estudiante
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();

CREATE TRIGGER trg_tutor_actualizado_en
    BEFORE UPDATE ON tutor
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();

CREATE TRIGGER trg_estudiante_tutor_actualizado_en
    BEFORE UPDATE ON estudiante_tutor
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();

CREATE TRIGGER trg_inscripcion_actualizado_en
    BEFORE UPDATE ON inscripcion
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();

CREATE TRIGGER trg_asignacion_docente_actualizado_en
    BEFORE UPDATE ON asignacion_docente
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();

CREATE TRIGGER trg_archivo_actualizado_en
    BEFORE UPDATE ON sia.archivo
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();

CREATE TRIGGER trg_archivo_referencia_actualizado_en
    BEFORE UPDATE ON sia.archivo_referencia
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();

-- =========================================================
-- 9. DATOS INICIALES
-- =========================================================

INSERT INTO rol (codigo, nombre, descripcion, es_global) VALUES
('SUPER_ADMIN',       'Super Administrador',          'Administrador global del sistema',             TRUE),
('ADMIN_INSTITUCION', 'Administrador de Institución', 'Administrador principal de la institución',   FALSE),
('DIRECTOR',          'Director',                     'Dirección institucional',                      FALSE),
('SECRETARIO',        'Secretario',                   'Gestión operativa académica y administrativa', FALSE),
('DOCENTE',           'Docente',                      'Docente del sistema',                          FALSE),
('ESTUDIANTE',        'Estudiante',                   'Estudiante del sistema',                       FALSE),
('TUTOR',             'Tutor',                        'Tutor/apoderado de estudiante',                FALSE);

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
-- v3.0  Sprint 1 inicial: institución, usuarios, roles, estructura académica,
--       personas (docente/estudiante/tutor), inscripción, asignación docente.
-- v3.1  Subsistema de archivos S3: tablas archivo y archivo_referencia.
--       Eliminado logo_url de institución. Roles ESTUDIANTE y TUTOR agregados.
-- v3.2  API: URLs pre-firmadas (presigned) para archivos S3 (sin cambio de BD).
--       API: DELETE /api/instituciones/{id}/configuraciones/{clave} (sin cambio de BD).
-- =========================================================