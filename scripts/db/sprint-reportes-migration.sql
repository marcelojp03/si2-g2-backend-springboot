-- =========================================================
-- MODULO REPORTES
-- PostgreSQL - ejecutar sobre schema sia
-- =========================================================

SET search_path TO sia, public;

CREATE TABLE IF NOT EXISTS reporte_filtro_favorito (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_usuario UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    codigo_reporte VARCHAR(80) NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    filtros_json JSONB NOT NULL,
    presentacion_json JSONB,
    favorito BOOLEAN NOT NULL DEFAULT FALSE,
    ultimo_usado BOOLEAN NOT NULL DEFAULT FALSE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS reporte_ejecucion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_usuario UUID NULL REFERENCES usuario(id) ON DELETE SET NULL,
    codigo_reporte VARCHAR(80) NOT NULL,
    tipo_salida VARCHAR(20) NOT NULL CHECK (tipo_salida IN ('HTML','PDF','XLSX','CSV')),
    estado VARCHAR(20) NOT NULL CHECK (estado IN ('PENDIENTE','PROCESANDO','COMPLETADO','ERROR')),
    filtros_json JSONB NOT NULL,
    total_registros INTEGER,
    archivo_url TEXT,
    error_mensaje TEXT,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    finalizado_en TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS reporte_dashboard_widget (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES institucion(id) ON DELETE CASCADE,
    id_usuario UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    codigo_reporte VARCHAR(80) NOT NULL,
    titulo VARCHAR(150) NOT NULL,
    tipo_widget VARCHAR(30) NOT NULL CHECK (tipo_widget IN ('TABLA','GRAFICO','KPI')),
    filtros_json JSONB NOT NULL,
    presentacion_json JSONB,
    posicion_x INTEGER NOT NULL DEFAULT 0,
    posicion_y INTEGER NOT NULL DEFAULT 0,
    ancho INTEGER NOT NULL DEFAULT 4,
    alto INTEGER NOT NULL DEFAULT 3,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_reporte_favorito_usuario
    ON reporte_filtro_favorito(id_institucion, id_usuario, codigo_reporte);

CREATE INDEX IF NOT EXISTS idx_reporte_ejecucion_usuario_estado
    ON reporte_ejecucion(id_institucion, id_usuario, estado, creado_en DESC);

CREATE INDEX IF NOT EXISTS idx_reporte_widget_usuario
    ON reporte_dashboard_widget(id_institucion, id_usuario, activo);
