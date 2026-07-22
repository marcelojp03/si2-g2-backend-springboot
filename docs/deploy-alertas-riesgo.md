# Deploy de Alertas de Riesgo

Este procedimiento actualiza una instalacion existente en AWS RDS y App
Runner. No depende de Docker Compose ni ejecuta seeds al iniciar el backend.

## 1. Respaldo

Crear un snapshot manual de RDS antes de aplicar la migracion. La migracion
conserva los datos historicos, pero cierra alertas activas duplicadas para
cumplir la nueva regla de una alerta activa por estudiante y gestion.

## 2. Migracion RDS

Ejecutar desde la raiz del backend con un usuario que pueda alterar el schema
`sia`:

```powershell
$env:PGPASSWORD = "<password-rds>"
psql "host=<host-rds> port=5432 dbname=<database> user=<user> sslmode=require" `
  -f scripts/db/deploy-alertas-riesgo.sql
Remove-Item Env:PGPASSWORD
```

El script usa `ON_ERROR_STOP`, por lo que `psql` termina ante el primer error.
Es idempotente y agrupa las migraciones de compatibilidad dimensional,
alertas, vigencia de datos y seguimientos.

## 3. Backend

Construir y publicar la imagen:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/deploy/deploy-to-ecr.ps1
```

Actualizar App Runner con la imagen publicada y esperar que el servicio quede
en estado `RUNNING`. Verificar al menos el login y `GET /actuator/health` si el
entorno expone Actuator.

## 4. Frontend

Publicar la rama `develop` del frontend con su procedimiento habitual. El
frontend y el backend deben desplegarse desde revisiones compatibles de la
misma entrega.

## 5. Datos demo opcionales

No ejecutar este paso en una institucion con datos academicos reales. Los
datos locales de PostgreSQL no viajan dentro de la imagen ni del repositorio.

Para reproducir los datos de demostracion desde codigo, el backend genera de
forma idempotente periodos P1/P2/P3, evaluaciones, notas variadas por
estudiante, dimensiones SABER/HACER/SER/AUTO y asistencias. Luego ejecuta el
primer analisis institucional:

```powershell
$superPassword = Read-Host "Password del super admin" -AsSecureString
$directorPassword = Read-Host "Password del director" -AsSecureString

& scripts/deploy/post-deploy-alertas-riesgo.ps1 `
  -ApiBase "https://<servicio>.awsapprunner.com" `
  -SuperAdminEmail "<super-admin>" `
  -SuperAdminPassword $superPassword `
  -DirectorEmail "<director>" `
  -DirectorPassword $directorPassword `
  -InstitutionCode "CSM-001" `
  -ConfirmDemoData
```

Las contrasenas se reciben como valores seguros y no se guardan en el
repositorio ni en el historial. Para una institucion real, omitir este paso y ejecutar el analisis
desde la pantalla cuando ya existan periodos, evaluaciones y calificaciones.

## 6. Verificacion

1. Iniciar sesion como director y abrir Alertas de Riesgo.
2. Confirmar que aparecen los paralelos autorizados.
3. Ejecutar el analisis para P1, P2, P3 y toda la gestion.
4. Abrir el detalle de un estudiante y comprobar materias y dimensiones.
5. Iniciar sesion como docente y confirmar que solo aparecen sus asignaciones.
6. Cambiar una alerta por `ABIERTA`, `EN_SEGUIMIENTO`, `ATENDIDA` y `CERRADA`.
7. Confirmar que el historial conserva cada transicion.

## Rollback

Si la migracion o el smoke test falla antes de generar datos demo, restaurar
el snapshot de RDS y volver a seleccionar la imagen anterior en App Runner.
Si ya se generaron datos demo, restaurar el snapshot es el unico rollback que
garantiza recuperar exactamente el estado anterior.
