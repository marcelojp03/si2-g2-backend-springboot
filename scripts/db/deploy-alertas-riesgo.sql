\set ON_ERROR_STOP on

-- Migracion maestra para una RDS existente. Ejecutar antes de desplegar la
-- imagen que contiene el modulo de alertas de riesgo.
\ir sync-riesgo-calificaciones-dimensiones.sql
\ir hu-nueva-19-riesgo-academico-migration.sql
\ir hu-nueva-19-riesgo-academico-correcciones.sql
