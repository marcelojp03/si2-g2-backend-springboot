-- =========================================================
-- SPRINT 1 - BASE DE DATOS (VERSION 2 CORREGIDA)
-- Sistema de Gestión Académica SaaS
-- PostgreSQL
-- Schema: sia (Sistema de Información Académico)
-- =========================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Crear schema SIA y establecerlo como activo
CREATE SCHEMA IF NOT EXISTS sia;
SET search_path TO sia;

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
    correo VARCHAR(150),
    direccion VARCHAR(255),
    logo_url TEXT,
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    creado_en TIMESTAMP NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE configuracion_institucion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    clave VARCHAR(100) NOT NULL,
    valor TEXT NOT NULL,
    tipo_valor VARCHAR(30) NOT NULL DEFAULT 'TEXTO'
        CHECK (tipo_valor IN ('TEXTO', 'NUMERO', 'BOOLEANO', 'JSON')),
    descripcion VARCHAR(255),
    creado_en TIMESTAMP NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_configuracion_institucion UNIQUE (id_institucion, clave)
);

CREATE TABLE rol (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo VARCHAR(50) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion VARCHAR(255),
    es_global BOOLEAN NOT NULL DEFAULT FALSE,
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    creado_en TIMESTAMP NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE usuario (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NULL REFERENCES institucion(id) ON DELETE CASCADE,
    correo VARCHAR(150) NOT NULL UNIQUE,
    hash_contrasena TEXT NOT NULL,
    nombres VARCHAR(120) NOT NULL,
    apellidos VARCHAR(120) NOT NULL,
    telefono VARCHAR(30),
    requiere_cambio_contrasena BOOLEAN NOT NULL DEFAULT FALSE,
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'INACTIVO', 'BLOQUEADO')),
    ultimo_acceso TIMESTAMP NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE usuario_rol (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_usuario UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    id_rol UUID NOT NULL REFERENCES rol(id),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_usuario_rol UNIQUE (id_usuario, id_rol)
);

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
    creado_en TIMESTAMP NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_gestion_academica UNIQUE (id_institucion, nombre),
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
    creado_en TIMESTAMP NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_curso_nombre UNIQUE (id_institucion, nombre),
    CONSTRAINT uq_curso_codigo UNIQUE NULLS NOT DISTINCT (id_institucion, codigo)
);

CREATE TABLE paralelo (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_curso UUID NOT NULL REFERENCES curso(id) ON DELETE CASCADE,
    id_gestion_academica UUID NOT NULL REFERENCES gestion_academica(id) ON DELETE CASCADE,
    nombre VARCHAR(20) NOT NULL,
    capacidad INTEGER,
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    creado_en TIMESTAMP NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_paralelo UNIQUE (id_institucion, id_curso, id_gestion_academica, nombre),
    CONSTRAINT ck_paralelo_capacidad CHECK (capacidad IS NULL OR capacidad > 0)
);

CREATE TABLE materia (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    codigo VARCHAR(30) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    area VARCHAR(100),
    carga_horaria INTEGER,
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    creado_en TIMESTAMP NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_materia_codigo UNIQUE (id_institucion, codigo),
    CONSTRAINT uq_materia_nombre UNIQUE (id_institucion, nombre),
    CONSTRAINT ck_materia_carga CHECK (carga_horaria IS NULL OR carga_horaria > 0)
);

CREATE TABLE curso_materia (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_curso UUID NOT NULL REFERENCES curso(id) ON DELETE CASCADE,
    id_materia UUID NOT NULL REFERENCES materia(id) ON DELETE CASCADE,
    id_gestion_academica UUID NOT NULL REFERENCES gestion_academica(id) ON DELETE CASCADE,
    carga_horaria INTEGER,
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    creado_en TIMESTAMP NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_curso_materia UNIQUE (id_institucion, id_curso, id_materia, id_gestion_academica),
    CONSTRAINT ck_curso_materia_carga CHECK (carga_horaria IS NULL OR carga_horaria > 0)
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
    correo VARCHAR(150),
    especialidad VARCHAR(120),
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    creado_en TIMESTAMP NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_docente_documento UNIQUE (id_institucion, documento_identidad),
    CONSTRAINT uq_docente_codigo UNIQUE (id_institucion, codigo),
    CONSTRAINT uq_docente_correo UNIQUE (id_institucion, correo),
    CONSTRAINT uq_docente_usuario UNIQUE (id_usuario)
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
    correo VARCHAR(150),
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'INACTIVO', 'EGRESADO', 'RETIRADO')),
    creado_en TIMESTAMP NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_estudiante_codigo UNIQUE (id_institucion, codigo_estudiante),
    CONSTRAINT uq_estudiante_documento UNIQUE (id_institucion, documento_identidad),
    CONSTRAINT uq_estudiante_correo UNIQUE (id_institucion, correo),
    CONSTRAINT uq_estudiante_usuario UNIQUE (id_usuario)
);

CREATE TABLE tutor (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_usuario UUID NULL REFERENCES usuario(id) ON DELETE SET NULL,
    documento_identidad VARCHAR(30),
    nombres VARCHAR(120) NOT NULL,
    apellidos VARCHAR(120) NOT NULL,
    telefono VARCHAR(30),
    correo VARCHAR(150),
    direccion VARCHAR(255),
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    creado_en TIMESTAMP NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_tutor_documento UNIQUE (id_institucion, documento_identidad),
    CONSTRAINT uq_tutor_correo UNIQUE (id_institucion, correo),
    CONSTRAINT uq_tutor_usuario UNIQUE (id_usuario)
);

CREATE TABLE estudiante_tutor (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_estudiante UUID NOT NULL REFERENCES estudiante(id) ON DELETE CASCADE,
    id_tutor UUID NOT NULL REFERENCES tutor(id) ON DELETE CASCADE,
    parentesco VARCHAR(50) NOT NULL,
    es_principal BOOLEAN NOT NULL DEFAULT FALSE,
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    creado_en TIMESTAMP NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_estudiante_tutor UNIQUE (id_institucion, id_estudiante, id_tutor)
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
    id_estudiante UUID NOT NULL REFERENCES estudiante(id) ON DELETE CASCADE,
    id_gestion_academica UUID NOT NULL REFERENCES gestion_academica(id) ON DELETE CASCADE,
    id_paralelo UUID NOT NULL REFERENCES paralelo(id) ON DELETE CASCADE,
    fecha_inscripcion DATE NOT NULL DEFAULT CURRENT_DATE,
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVA' CHECK (estado IN ('ACTIVA', 'RETIRADA', 'CONCLUIDA', 'ANULADA')),
    observacion VARCHAR(255),
    creado_en TIMESTAMP NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Índice para evitar duplicados activos del mismo estudiante en la misma gestión
CREATE UNIQUE INDEX uq_inscripcion_activa_estudiante_gestion
    ON inscripcion (id_institucion, id_estudiante, id_gestion_academica)
    WHERE estado = 'ACTIVA';

CREATE TABLE asignacion_docente (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_docente UUID NOT NULL REFERENCES docente(id) ON DELETE CASCADE,
    id_materia UUID NOT NULL REFERENCES materia(id) ON DELETE CASCADE,
    id_paralelo UUID NOT NULL REFERENCES paralelo(id) ON DELETE CASCADE,
    id_gestion_academica UUID NOT NULL REFERENCES gestion_academica(id) ON DELETE CASCADE,
    carga_horaria INTEGER,
    estado VARCHAR(15) NOT NULL DEFAULT 'ACTIVA' CHECK (estado IN ('ACTIVA', 'INACTIVA')),
    creado_en TIMESTAMP NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_asignacion_docente UNIQUE (id_institucion, id_docente, id_materia, id_paralelo, id_gestion_academica),
    CONSTRAINT ck_asignacion_carga CHECK (carga_horaria IS NULL OR carga_horaria > 0)
);

-- =========================================================
-- BITÁCORA / CAJA NEGRA
-- =========================================================

CREATE TABLE bitacora_auditoria (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NULL REFERENCES institucion(id),
    id_usuario UUID NULL REFERENCES usuario(id),
    fecha_evento TIMESTAMP NOT NULL DEFAULT NOW(),
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
    creado_en TIMESTAMP NOT NULL DEFAULT NOW()
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

-- =========================================================
-- 9. DATOS INICIALES
-- =========================================================

INSERT INTO rol (codigo, nombre, descripcion, es_global) VALUES
('SUPER_ADMIN',       'Super Administrador',           'Administrador global del sistema',          TRUE),
('ADMIN_INSTITUCION', 'Administrador de Institución',  'Administrador principal de la institución', FALSE),
('DIRECTOR',          'Director',                      'Dirección institucional',                   FALSE),
('DOCENTE',           'Docente',                       'Docente del sistema',                       FALSE),
('ESTUDIANTE',        'Estudiante',                    'Estudiante del sistema',                    FALSE),
('TUTOR',             'Tutor',                         'Padre, madre o tutor',                      FALSE);

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