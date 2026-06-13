-- ============================================================
-- MÓDULO PAGOS/CUOTAS — Planes de pago, cuotas por estudiante, pagos
-- ============================================================

ALTER TABLE sia.inscripcion ADD COLUMN IF NOT EXISTS id_plan_pago UUID REFERENCES sia.plan_pago(id);

CREATE TABLE IF NOT EXISTS sia.plan_pago (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES sia.institucion(id) ON DELETE CASCADE,
    nombre VARCHAR(120) NOT NULL,
    tipo_periodo VARCHAR(20) NOT NULL CHECK (tipo_periodo IN ('MENSUAL', 'TRIMESTRAL', 'SEMESTRAL', 'ANUAL')),
    monto NUMERIC(10,2) NOT NULL CHECK (monto > 0),
    moneda VARCHAR(3) NOT NULL DEFAULT 'BOB',
    cantidad_cuotas INTEGER NOT NULL CHECK (cantidad_cuotas >= 1),
    dia_vencimiento INTEGER NOT NULL DEFAULT 10 CHECK (dia_vencimiento BETWEEN 1 AND 28),
    descripcion TEXT,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_plan_pago_institucion_nombre UNIQUE (id_institucion, nombre)
);

CREATE TABLE IF NOT EXISTS sia.cuota_estudiante (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES sia.institucion(id) ON DELETE CASCADE,
    id_estudiante UUID NOT NULL REFERENCES sia.estudiante(id) ON DELETE CASCADE,
    id_plan_pago UUID NOT NULL REFERENCES sia.plan_pago(id) ON DELETE RESTRICT,
    id_gestion_academica UUID NOT NULL REFERENCES sia.gestion_academica(id) ON DELETE CASCADE,
    numero_cuota INTEGER NOT NULL CHECK (numero_cuota >= 1),
    monto NUMERIC(10,2) NOT NULL CHECK (monto > 0),
    fecha_vencimiento DATE NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE', 'PAGADA', 'VENCIDA', 'ANULADA')),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_cuota_estudiante UNIQUE (id_institucion, id_estudiante, id_plan_pago, numero_cuota)
);

CREATE INDEX IF NOT EXISTS idx_cuota_estudiante_estado
    ON sia.cuota_estudiante (id_estudiante, estado);

CREATE TABLE IF NOT EXISTS sia.pago (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_institucion UUID NOT NULL REFERENCES sia.institucion(id) ON DELETE CASCADE,
    id_cuota UUID NOT NULL REFERENCES sia.cuota_estudiante(id) ON DELETE CASCADE,
    id_usuario_paga UUID NOT NULL REFERENCES sia.usuario(id) ON DELETE CASCADE,
    monto NUMERIC(10,2) NOT NULL CHECK (monto > 0),
    moneda VARCHAR(3) NOT NULL DEFAULT 'BOB',
    metodo_pago VARCHAR(30) NOT NULL DEFAULT 'QR' CHECK (metodo_pago IN ('QR', 'TRANSFERENCIA', 'TIGO_MONEY', 'EFECTIVO', 'TARJETA')),
    proveedor VARCHAR(30),
    referencia_externa VARCHAR(200),
    token_pago UUID,
    qr_base64 TEXT,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE', 'COMPLETADO', 'FALLIDO', 'REEMBOLSADO')),
    pagado_en TIMESTAMPTZ,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pago_cuota ON sia.pago (id_cuota);
CREATE INDEX IF NOT EXISTS idx_pago_usuario ON sia.pago (id_usuario_paga);
