-- =========================================================
-- MIGRACIÓN INCREMENTAL CONSOLIDADA
-- Sprint 2 (RBAC + Seguridad) + Sprint Especial SaaS + FCM
-- Sistema de Gestión Académica SaaS — Grupo 2 SI2
-- PostgreSQL — Schema: sia
-- Versión: 2.0
--
-- PRE-REQUISITO:
--   db-script.sql (Sprint 1) + scripts Sprint 2 aplicados
--   hasta sprint2-fixes-migration.sql (inclusive).
--
-- EJECUCIÓN (script único para producción):
--   psql -h <host> -U <user> -d <db> -f sprint-especial-saas-migration.sql
--
-- IDEMPOTENCIA:
--   Todos los CREATE usan IF NOT EXISTS.
--   Los ALTER TABLE usan ADD COLUMN IF NOT EXISTS.
--   Los INSERTs usan ON CONFLICT DO NOTHING.
-- =========================================================

SET search_path TO sia, public;

-- =========================================================
-- 0. RBAC — ROLES DINÁMICOS Y PERMISOS (Sprint 2)
-- =========================================================

ALTER TABLE rol
    ADD COLUMN IF NOT EXISTS id_institucion UUID NULL REFERENCES institucion(id) ON DELETE CASCADE;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_rol_nombre_institucion'
    ) THEN
        ALTER TABLE rol
            ADD CONSTRAINT uq_rol_nombre_institucion UNIQUE NULLS NOT DISTINCT (id_institucion, nombre);
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS permiso (
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

CREATE TABLE IF NOT EXISTS rol_permiso (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_rol UUID NOT NULL REFERENCES rol(id) ON DELETE CASCADE,
    id_permiso UUID NOT NULL REFERENCES permiso(id) ON DELETE CASCADE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_rol_permiso UNIQUE (id_rol, id_permiso)
);

UPDATE rol SET es_global = TRUE, id_institucion = NULL
WHERE codigo IN ('ADMIN_INSTITUCION', 'DIRECTOR', 'SECRETARIO', 'DOCENTE', 'ESTUDIANTE', 'TUTOR');

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
    ('MI_AREA_READ',         'Mi área: lectura',                'MI_AREA',          'READ',  'Permite acceder al área operativa del docente')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id, p.id FROM rol r
JOIN permiso p ON p.codigo IN (
    'USUARIOS_READ','USUARIOS_WRITE','CONFIGURACION_READ','CONFIGURACION_WRITE',
    'GESTION_READ','GESTION_WRITE','PERSONAS_READ','PERSONAS_WRITE',
    'OPERACION_READ','OPERACION_WRITE','ROLES_READ','ROLES_WRITE','MI_AREA_READ')
WHERE r.codigo = 'ADMIN_INSTITUCION'
ON CONFLICT (id_rol, id_permiso) DO NOTHING;

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id, p.id FROM rol r
JOIN permiso p ON p.codigo IN (
    'USUARIOS_READ','CONFIGURACION_READ','GESTION_READ','GESTION_WRITE',
    'PERSONAS_READ','PERSONAS_WRITE','OPERACION_READ','OPERACION_WRITE',
    'ROLES_READ','MI_AREA_READ')
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
-- 0B. BITÁCORA MEJORADA Y RECUPERACIÓN DE CONTRASEÑA (Sprint 2)
-- =========================================================

ALTER TABLE bitacora_auditoria
    ADD COLUMN IF NOT EXISTS metodo_http       VARCHAR(10),
    ADD COLUMN IF NOT EXISTS ruta_recurso      VARCHAR(255),
    ADD COLUMN IF NOT EXISTS nombre_funcion    VARCHAR(150),
    ADD COLUMN IF NOT EXISTS hash_integridad   VARCHAR(128);

CREATE TABLE IF NOT EXISTS password_recovery_challenge (
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

CREATE INDEX IF NOT EXISTS idx_password_recovery_usuario   ON password_recovery_challenge (id_usuario);
CREATE INDEX IF NOT EXISTS idx_password_recovery_expira_en ON password_recovery_challenge (expira_en DESC);

INSERT INTO permiso (codigo, nombre, modulo, accion, descripcion)
VALUES ('AUDITORIA_READ', 'Auditoría: lectura', 'AUDITORIA', 'READ', 'Permite consultar la bitácora de auditoría')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id, p.id FROM rol r
JOIN permiso p ON p.codigo = 'AUDITORIA_READ'
WHERE r.codigo IN ('ADMIN_INSTITUCION', 'DIRECTOR')
ON CONFLICT (id_rol, id_permiso) DO NOTHING;

-- =========================================================
-- A. PLANES DE SUSCRIPCIÓN Y MÓDULOS DEL SISTEMA
-- =========================================================

-- A.1 Planes (BASICO / PROFESIONAL / EMPRESARIAL)
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

-- A.2 Módulos que ofrece la plataforma
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

-- A.3 Relación plan ↔ módulo
CREATE TABLE IF NOT EXISTS plan_modulo (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    id_plan         UUID        NOT NULL REFERENCES plan_suscripcion(id) ON DELETE CASCADE,
    id_modulo       UUID        NOT NULL REFERENCES modulo_sistema(id) ON DELETE CASCADE,
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_plan_modulo UNIQUE (id_plan, id_modulo)
);

-- A.4 Suscripción de una institución a un plan
--     Solo una suscripción ACTIVA por institución (índice parcial).
--     Se permite historial: el campo estado cambia a VENCIDA/CANCELADA.
CREATE TABLE IF NOT EXISTS suscripcion_institucion (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion  UUID        NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_plan         UUID        NOT NULL REFERENCES plan_suscripcion(id),
    fecha_inicio    DATE        NOT NULL DEFAULT CURRENT_DATE,
    fecha_fin       DATE        NULL,
    estado          VARCHAR(20) NOT NULL DEFAULT 'ACTIVA'
                        CHECK (estado IN ('ACTIVA', 'VENCIDA', 'CANCELADA', 'SUSPENDIDA')),
    simulada        BOOLEAN     NOT NULL DEFAULT TRUE,  -- TRUE en versión académica
    observacion     TEXT,
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_suscripcion_fechas CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio)
);

-- Solo una suscripción activa por institución en todo momento
CREATE UNIQUE INDEX IF NOT EXISTS uq_suscripcion_activa_por_institucion
    ON suscripcion_institucion (id_institucion)
    WHERE estado = 'ACTIVA';

CREATE INDEX IF NOT EXISTS idx_suscripcion_institucion ON suscripcion_institucion (id_institucion);
CREATE INDEX IF NOT EXISTS idx_suscripcion_plan        ON suscripcion_institucion (id_plan);
CREATE INDEX IF NOT EXISTS idx_suscripcion_estado      ON suscripcion_institucion (estado);
CREATE INDEX IF NOT EXISTS idx_plan_suscripcion_estado ON plan_suscripcion (estado);

-- =========================================================
-- B. PRIVILEGIOS DE INTERFAZ DE USUARIO (campo / botón por rol)
-- =========================================================
-- Define visibilidad y edición de cada campo/botón por rol dentro de una institución.
-- visibilidad: VISIBLE | OCULTO
-- edicion:     EDITABLE | SOLO_LECTURA | OCULTO

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

-- =========================================================
-- C. SEGURIDAD — INTENTOS DE INICIO DE SESIÓN
-- =========================================================
-- Registra TODOS los intentos: exitosos y fallidos.
-- Permite detectar ataques de fuerza bruta, cuentas comprometidas.

CREATE TABLE IF NOT EXISTS intento_login (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    correo          CITEXT      NOT NULL,
    id_usuario      UUID        NULL REFERENCES usuario(id) ON DELETE SET NULL,
    id_institucion  UUID        NULL REFERENCES institucion(id) ON DELETE SET NULL,
    fecha_intento   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    exito           BOOLEAN     NOT NULL,
    ip              VARCHAR(50),
    agente_usuario  TEXT,
    -- Motivos de fallo tipificados para análisis
    motivo_fallo    VARCHAR(60) NULL
                        CHECK (motivo_fallo IN (
                            'CREDENCIALES_INVALIDAS',
                            'CUENTA_BLOQUEADA',
                            'CUENTA_INACTIVA',
                            'INSTITUCION_INACTIVA',
                            'TOKEN_EXPIRADO',
                            'OTRO'
                        )),
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT NOW()
    -- Sin actualizado_en: registro inmutable (auditoría)
);

CREATE INDEX IF NOT EXISTS idx_intento_login_correo   ON intento_login (correo);
CREATE INDEX IF NOT EXISTS idx_intento_login_fecha    ON intento_login (fecha_intento DESC);
CREATE INDEX IF NOT EXISTS idx_intento_login_usuario  ON intento_login (id_usuario)
    WHERE id_usuario IS NOT NULL;
-- Índice especializado para detección de fuerza bruta
CREATE INDEX IF NOT EXISTS idx_intento_login_fallidos ON intento_login (correo, fecha_intento DESC)
    WHERE exito = FALSE;

-- =========================================================
-- D. RESPALDO Y RESTAURACIÓN
-- =========================================================

-- D.1 Registro de ejecuciones de backup
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
    ruta_almacenamiento   VARCHAR(500),  -- S3 key / ruta lógica
    tamanio_bytes         BIGINT,
    observacion           TEXT,
    simulado              BOOLEAN     NOT NULL DEFAULT TRUE,  -- TRUE en versión académica
    creado_en             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- D.2 Registro de solicitudes de restauración (requieren aprobación)
CREATE TABLE IF NOT EXISTS registro_restauracion (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    id_respaldo      UUID        NOT NULL REFERENCES registro_respaldo(id) ON DELETE RESTRICT,
    id_institucion   UUID        NULL REFERENCES institucion(id) ON DELETE SET NULL,
    solicitado_por   UUID        NULL REFERENCES usuario(id) ON DELETE SET NULL,
    aprobado_por     UUID        NULL REFERENCES usuario(id) ON DELETE SET NULL,
    fecha_solicitud  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_ejecucion  TIMESTAMPTZ NULL,
    estado           VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE'
                         CHECK (estado IN ('PENDIENTE', 'APROBADO', 'EN_PROGRESO', 'COMPLETADO', 'RECHAZADO', 'FALLIDO')),
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

-- =========================================================
-- E. REPORTES CONFIGURABLES
-- =========================================================

-- E.1 Definición de reporte (plantilla reutilizable por institución)
CREATE TABLE IF NOT EXISTS reporte_configurable (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID        NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    codigo         VARCHAR(60) NOT NULL,
    nombre         VARCHAR(150) NOT NULL,
    descripcion    TEXT,
    -- Entidad base que se consulta: 'estudiante', 'asistencia', 'calificacion', etc.
    entidad_base   VARCHAR(60) NOT NULL,
    tipo_reporte   VARCHAR(20) NOT NULL DEFAULT 'ANALITICO'
                       CHECK (tipo_reporte IN ('ANALITICO', 'GERENCIAL', 'DINAMICO')),
    es_global      BOOLEAN     NOT NULL DEFAULT FALSE,  -- TRUE = disponible a toda institución
    creado_por     UUID        NULL REFERENCES usuario(id) ON DELETE SET NULL,
    estado         VARCHAR(15) NOT NULL DEFAULT 'ACTIVO'
                       CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    creado_en      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_reporte_configurable UNIQUE (id_institucion, codigo)
);

-- E.2 Campos disponibles de un reporte (configuración de columnas)
CREATE TABLE IF NOT EXISTS reporte_campo (
    id                     UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    id_reporte             UUID        NOT NULL REFERENCES reporte_configurable(id) ON DELETE CASCADE,
    nombre_campo           VARCHAR(100) NOT NULL,  -- Nombre interno (mapea a campo del dominio)
    etiqueta               VARCHAR(150) NOT NULL,  -- Nombre visible en UI
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

-- E.3 Bitácora de generaciones de reporte (auditoría de consultas)
CREATE TABLE IF NOT EXISTS bitacora_reporte (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion    UUID        NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_reporte        UUID        NOT NULL REFERENCES reporte_configurable(id) ON DELETE CASCADE,
    id_usuario        UUID        NULL REFERENCES usuario(id) ON DELETE SET NULL,
    fecha_generacion  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    filtros_aplicados JSONB,      -- Snapshot de los filtros usados
    cantidad_filas    INTEGER,
    formato           VARCHAR(10) NOT NULL DEFAULT 'JSON'
                          CHECK (formato IN ('JSON', 'PDF', 'EXCEL', 'CSV')),
    exito             BOOLEAN     NOT NULL DEFAULT TRUE,
    mensaje_error     TEXT,
    creado_en         TIMESTAMPTZ NOT NULL DEFAULT NOW()
    -- Sin actualizado_en: registro inmutable (auditoría)
);

CREATE INDEX IF NOT EXISTS idx_reporte_configurable_institucion ON reporte_configurable (id_institucion);
CREATE INDEX IF NOT EXISTS idx_reporte_configurable_tipo        ON reporte_configurable (tipo_reporte);
CREATE INDEX IF NOT EXISTS idx_reporte_configurable_estado      ON reporte_configurable (id_institucion, estado);
CREATE INDEX IF NOT EXISTS idx_reporte_campo_reporte            ON reporte_campo (id_reporte);
CREATE INDEX IF NOT EXISTS idx_bitacora_reporte_institucion     ON bitacora_reporte (id_institucion);
CREATE INDEX IF NOT EXISTS idx_bitacora_reporte_fecha           ON bitacora_reporte (fecha_generacion DESC);
CREATE INDEX IF NOT EXISTS idx_bitacora_reporte_usuario         ON bitacora_reporte (id_usuario);

-- =========================================================
-- F. TRIGGERS actualizado_en (tablas nuevas)
-- =========================================================
-- Requiere que fn_actualizar_actualizado_en() esté definida (viene del db-script.sql base)

CREATE OR REPLACE TRIGGER trg_plan_suscripcion_actualizado_en
    BEFORE UPDATE ON plan_suscripcion
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();

CREATE OR REPLACE TRIGGER trg_modulo_sistema_actualizado_en
    BEFORE UPDATE ON modulo_sistema
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();

CREATE OR REPLACE TRIGGER trg_plan_modulo_actualizado_en
    BEFORE UPDATE ON plan_modulo
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();

CREATE OR REPLACE TRIGGER trg_suscripcion_institucion_actualizado_en
    BEFORE UPDATE ON suscripcion_institucion
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();

CREATE OR REPLACE TRIGGER trg_privilegio_ui_actualizado_en
    BEFORE UPDATE ON privilegio_ui
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();

CREATE OR REPLACE TRIGGER trg_registro_respaldo_actualizado_en
    BEFORE UPDATE ON registro_respaldo
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();

CREATE OR REPLACE TRIGGER trg_registro_restauracion_actualizado_en
    BEFORE UPDATE ON registro_restauracion
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();

CREATE OR REPLACE TRIGGER trg_reporte_configurable_actualizado_en
    BEFORE UPDATE ON reporte_configurable
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();

CREATE OR REPLACE TRIGGER trg_reporte_campo_actualizado_en
    BEFORE UPDATE ON reporte_campo
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();

-- =========================================================
-- G. DATOS SEMILLA (SEED)
-- =========================================================

-- G.1 Planes del sistema
INSERT INTO plan_suscripcion (codigo, nombre, descripcion, max_usuarios, max_almacenamiento_mb, precio_mensual, estado)
VALUES
    ('BASICO',
     'Plan Básico',
     'Hasta 10 usuarios. Módulos de identidad, estructura académica y operación básica.',
     10, 512, 0.00, 'ACTIVO'),
    ('PROFESIONAL',
     'Plan Profesional',
     'Hasta 50 usuarios. Todos los módulos Sprint 1-2: asistencia, calificaciones y horarios.',
     50, 2048, 150.00, 'ACTIVO'),
    ('EMPRESARIAL',
     'Plan Empresarial',
     'Usuarios ilimitados. Incluye reportes avanzados, auditoría completa y gestión de respaldos.',
     999, 10240, 350.00, 'ACTIVO')
ON CONFLICT (codigo) DO NOTHING;

-- G.2 Módulos de la plataforma
INSERT INTO modulo_sistema (codigo, nombre, descripcion, ruta_frontend, orden_visual)
VALUES
    ('IDENTIDAD',    'Identidad y usuarios',   'Gestión de usuarios, roles y permisos',            '/usuarios',       1),
    ('ESTRUCTURA',   'Estructura académica',   'Cursos, paralelos, materias y gestión académica',  '/cursos',         2),
    ('OPERACION',    'Operación académica',    'Inscripciones, asignaciones docentes',             '/inscripciones',  3),
    ('ASISTENCIA',   'Asistencia',             'Registro y consulta de asistencia por sesión',     '/asistencia',     4),
    ('CALIFICACION', 'Calificaciones',         'Registro y consulta de calificaciones y notas',    '/calificaciones', 5),
    ('HORARIOS',     'Horarios',               'Programación semanal de clases por paralelo',      '/horarios',       6),
    ('REPORTES',     'Reportes',               'Reportes analíticos, gerenciales y dinámicos',     '/reportes',       7),
    ('AUDITORIA',    'Auditoría',              'Bitácora de operaciones y actividad de usuarios',  '/auditoria',      8),
    ('RESPALDOS',    'Respaldos',              'Gestión de copias de seguridad y restauraciones',  '/backups',        9)
ON CONFLICT (codigo) DO NOTHING;

-- G.3 Asociar módulos a planes
DO $$
DECLARE
    v_plan_basico       UUID;
    v_plan_pro          UUID;
    v_plan_emp          UUID;
    v_mod_id            UUID;
    v_codigo            TEXT;
    v_modulos_basico    TEXT[] := ARRAY['IDENTIDAD', 'ESTRUCTURA', 'OPERACION'];
    v_modulos_pro       TEXT[] := ARRAY['IDENTIDAD', 'ESTRUCTURA', 'OPERACION',
                                        'ASISTENCIA', 'CALIFICACION', 'HORARIOS', 'REPORTES'];
    v_modulos_emp       TEXT[] := ARRAY['IDENTIDAD', 'ESTRUCTURA', 'OPERACION',
                                        'ASISTENCIA', 'CALIFICACION', 'HORARIOS',
                                        'REPORTES', 'AUDITORIA', 'RESPALDOS'];
BEGIN
    SELECT id INTO v_plan_basico FROM plan_suscripcion WHERE codigo = 'BASICO';
    SELECT id INTO v_plan_pro    FROM plan_suscripcion WHERE codigo = 'PROFESIONAL';
    SELECT id INTO v_plan_emp    FROM plan_suscripcion WHERE codigo = 'EMPRESARIAL';

    FOREACH v_codigo IN ARRAY v_modulos_basico LOOP
        SELECT id INTO v_mod_id FROM modulo_sistema WHERE codigo = v_codigo;
        INSERT INTO plan_modulo (id_plan, id_modulo) VALUES (v_plan_basico, v_mod_id)
        ON CONFLICT DO NOTHING;
    END LOOP;

    FOREACH v_codigo IN ARRAY v_modulos_pro LOOP
        SELECT id INTO v_mod_id FROM modulo_sistema WHERE codigo = v_codigo;
        INSERT INTO plan_modulo (id_plan, id_modulo) VALUES (v_plan_pro, v_mod_id)
        ON CONFLICT DO NOTHING;
    END LOOP;

    FOREACH v_codigo IN ARRAY v_modulos_emp LOOP
        SELECT id INTO v_mod_id FROM modulo_sistema WHERE codigo = v_codigo;
        INSERT INTO plan_modulo (id_plan, id_modulo) VALUES (v_plan_emp, v_mod_id)
        ON CONFLICT DO NOTHING;
    END LOOP;
END $$;

-- G.4 Suscribir institución de demo al Plan Profesional (si existe)
DO $$
DECLARE
    v_inst_id   UUID;
    v_plan_id   UUID;
BEGIN
    SELECT id INTO v_inst_id FROM institucion ORDER BY creado_en LIMIT 1;
    SELECT id INTO v_plan_id FROM plan_suscripcion WHERE codigo = 'PROFESIONAL';

    IF v_inst_id IS NOT NULL AND v_plan_id IS NOT NULL THEN
        INSERT INTO suscripcion_institucion (id_institucion, id_plan, fecha_inicio, estado, simulada, observacion)
        VALUES (v_inst_id, v_plan_id, CURRENT_DATE, 'ACTIVA', TRUE, 'Suscripción demo creada en migración Sprint Especial')
        ON CONFLICT DO NOTHING;
    END IF;
END $$;

-- =========================================================
-- H. SOLICITUDES DE ONBOARDING SaaS
-- =========================================================
-- Un visitante llena el formulario de la landing page.
-- El SUPER_ADMIN revisa, aprueba, confirma pago y activa.
-- Estados: PENDIENTE_REVISION → APROBADA → PENDIENTE_PAGO → PAGADO → ACTIVA
--          PENDIENTE_REVISION → RECHAZADA

CREATE TABLE IF NOT EXISTS solicitud_onboarding (
    id                      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    -- Datos de la institución solicitante
    nombre_institucion      VARCHAR(200) NOT NULL,
    tipo_institucion        VARCHAR(20)  NOT NULL DEFAULT 'PRIVADO'
                                CHECK (tipo_institucion IN ('FISCAL', 'CONVENIO', 'PRIVADO')),
    telefono_institucion    VARCHAR(30),
    correo_institucion      CITEXT,
    direccion_institucion   VARCHAR(255),
    -- Datos del contacto (futuro ADMIN_INSTITUCION)
    nombres_contacto        VARCHAR(120) NOT NULL,
    apellidos_contacto      VARCHAR(120) NOT NULL,
    correo_contacto         CITEXT       NOT NULL,
    telefono_contacto       VARCHAR(30),
    -- Plan solicitado
    id_plan                 UUID         NOT NULL REFERENCES plan_suscripcion(id),
    -- Mensaje / descripción libre del solicitante
    mensaje                 TEXT,
    -- Estado del proceso
    estado                  VARCHAR(30)  NOT NULL DEFAULT 'PENDIENTE_REVISION'
                                CHECK (estado IN (
                                    'PENDIENTE_REVISION',
                                    'APROBADA',
                                    'PENDIENTE_PAGO',
                                    'PAGADO',
                                    'ACTIVA',
                                    'RECHAZADA'
                                )),
    -- Notas internas del SUPER_ADMIN
    notas_admin             TEXT,
    -- Referencia a entidades creadas al activar
    id_institucion_creada   UUID         NULL REFERENCES institucion(id) ON DELETE SET NULL,
    id_usuario_creado        UUID         NULL REFERENCES usuario(id) ON DELETE SET NULL,
    -- Auditoría
    creado_en               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    actualizado_en          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_solicitud_onboarding_estado  ON solicitud_onboarding (estado);
CREATE INDEX IF NOT EXISTS idx_solicitud_onboarding_correo  ON solicitud_onboarding (correo_contacto);
CREATE INDEX IF NOT EXISTS idx_solicitud_onboarding_plan    ON solicitud_onboarding (id_plan);
CREATE INDEX IF NOT EXISTS idx_solicitud_onboarding_inst    ON solicitud_onboarding (id_institucion_creada)
    WHERE id_institucion_creada IS NOT NULL;

CREATE OR REPLACE TRIGGER trg_solicitud_onboarding_actualizado_en
    BEFORE UPDATE ON solicitud_onboarding
    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_actualizado_en();

-- =========================================================
-- I. NOTIFICACIONES PUSH — FCM TOKEN EN USUARIO
-- =========================================================
-- Almacena el token FCM del dispositivo móvil por usuario.
-- Se actualiza cada vez que la app Flutter inicia sesión.

ALTER TABLE usuario
    ADD COLUMN IF NOT EXISTS fcm_token TEXT NULL;

COMMENT ON COLUMN usuario.fcm_token IS 'Token FCM del dispositivo móvil (Firebase Cloud Messaging). Nulo si el usuario no usa la app móvil.';

-- =========================================================
-- FIN DEL SCRIPT
-- =========================================================
-- Tablas creadas:
--   plan_suscripcion, modulo_sistema, plan_modulo, suscripcion_institucion
--   privilegio_ui
--   intento_login
--   registro_respaldo, registro_restauracion
--   reporte_configurable, reporte_campo, bitacora_reporte
--   solicitud_onboarding
--
-- Columnas alteradas:
--   usuario.fcm_token (TEXT NULL) — token push FCM para app móvil
--
-- Total: 12 tablas nuevas + 1 columna en tabla existente
-- =========================================================
