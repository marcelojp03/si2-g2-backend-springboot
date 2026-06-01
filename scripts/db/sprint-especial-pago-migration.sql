-- ════════════════════════════════════════════════════════════════════════════
-- Sprint Especial SaaS — Pago de onboarding (pasarela Vpay)
-- Tabla: sia.pago_suscripcion
--
-- Registra el pago del plan asociado a una solicitud de onboarding.
-- El pago es PRE-institución: se vincula a solicitud_onboarding, no a id_institucion.
-- Flujo: APROBADA → generar QR (PENDIENTE) → Vpay PAG → PAGADO → activar institución.
--
-- Ejecutar manualmente en RDS/PostgreSQL (no hay Flyway/Liquibase).
-- ════════════════════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS sia.pago_suscripcion (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    id_solicitud        UUID            NOT NULL,
    id_plan             UUID            NOT NULL,
    monto               NUMERIC(10, 2)  NOT NULL,
    moneda              VARCHAR(5)      NOT NULL DEFAULT 'BOB',
    metodo_pago         VARCHAR(30)     NOT NULL DEFAULT 'QR',
    proveedor           VARCHAR(50)     NOT NULL DEFAULT 'VPAY',
    token_pago          UUID            NOT NULL DEFAULT gen_random_uuid(), -- token público del link de pago
    referencia_externa  VARCHAR(150),                       -- id del QR devuelto por Vpay
    qr_base64           TEXT,                               -- imagen del QR en base64 (PNG)
    estado              VARCHAR(20)     NOT NULL DEFAULT 'PENDIENTE'
                          CHECK (estado IN ('PENDIENTE', 'PAGADO', 'CANCELADO', 'EXPIRADO')),
    glosa               VARCHAR(150),
    fecha_expiracion    DATE,
    pagado_en           TIMESTAMPTZ,
    creado_en           TIMESTAMPTZ     NOT NULL DEFAULT now(),
    actualizado_en      TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT fk_pago_solicitud
        FOREIGN KEY (id_solicitud) REFERENCES sia.solicitud_onboarding (id) ON DELETE CASCADE,
    CONSTRAINT fk_pago_plan
        FOREIGN KEY (id_plan) REFERENCES sia.plan_suscripcion (id)
);

CREATE INDEX        IF NOT EXISTS idx_pago_suscripcion_solicitud   ON sia.pago_suscripcion (id_solicitud);
CREATE INDEX        IF NOT EXISTS idx_pago_suscripcion_estado       ON sia.pago_suscripcion (estado);
CREATE INDEX        IF NOT EXISTS idx_pago_suscripcion_referencia   ON sia.pago_suscripcion (referencia_externa);
CREATE UNIQUE INDEX IF NOT EXISTS idx_pago_suscripcion_token_pago   ON sia.pago_suscripcion (token_pago);

-- Si ya ejecutaste la migración sin token_pago, aplica este parche:
-- ALTER TABLE sia.pago_suscripcion ADD COLUMN IF NOT EXISTS token_pago UUID NOT NULL DEFAULT gen_random_uuid();
-- CREATE UNIQUE INDEX IF NOT EXISTS idx_pago_suscripcion_token_pago ON sia.pago_suscripcion (token_pago);
