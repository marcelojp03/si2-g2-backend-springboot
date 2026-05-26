# AGENTS.md

Guia operativa para agentes de codigo en este backend Spring Boot.

## Alcance
- Proyecto: backend academico multi-tenant (schema compartido, aislamiento por `id_institucion`).
- Stack: Spring Boot 3.5, Java 23, Maven, PostgreSQL, JWT, AWS S3.
- Estructura modular por paquetes en `src/main/java/com/uagrm/si2g2`.

### Módulos implementados

**Sprint 1:** `auth`, `institucion`, `usuario`, `academico`, `curso`, `materia`, `docente`, `estudiante`, `tutor`, `inscripcion`, `asignacion`, `storage`, `auditoria`

**Sprint 2:** `aula`, `horario`, `asistencia`, `calificacion`, `dashboard`

**Sprint Especial:** `saas/plan`, `saas/suscripcion`, `saas/solicitud` (en progreso — paquete raíz `saas/`)

Cada módulo sigue la estructura interna `api/` · `application/` · `domain/` · `dto/`.

## Convenciones Sprint Especial (SaaS)
- `TenantContext.getOrThrow()` en lugar de `TenantContext.get()` en todos los services de negocio (HU-SE-01).
- `@PreAuthorize("hasRole('SUPER_ADMIN')")` en todas las mutaciones de planes (`saas/plan`).
- Al suscribir institución: validar `max_usuarios` antes de crear usuario en `UsuarioService.crear()`.
- Endpoints de privilegios UI (`/api/privilegios-ui`) devuelven mapa plano `{ modulo: { entidad: { campo: { visible, editable } } } }`.
- Registrar en `bitacora_auditoria` toda operación de suscripción, planes, respaldos y permisos.

## Comandos de trabajo
- Compilar rapido: `mvn compile -q`
- Ejecutar backend: `mvn spring-boot:run`
- Ejecutar tests: `mvn test`
- Build paquete: `mvn package -DskipTests`
- Deploy imagen a ECR: `powershell -ExecutionPolicy Bypass -File scripts/deploy/deploy-to-ecr.ps1`

## Requisitos de entorno
- Java: usar JDK 23 para Maven (no JDK 11). Verificar con `mvn -v`.
- Config principal: `src/main/resources/application.yml`
- Variables locales: `.env` (se carga por `spring.config.import`). Plantilla: `.env.example`.
- DB: `ddl-auto: none`; el schema se mantiene con SQL manual en `scripts/db/db-script.sql`.

## Convenciones del proyecto
- Responses API envueltas en `ApiResponse<T>`.
- Validaciones con Bean Validation en DTOs + `@Valid` en controllers.
- Errores centralizados en `common/exception/GlobalExceptionHandler`.
- Seguridad por roles con `@PreAuthorize` y JWT stateless.
- Multi-tenant: siempre filtrar/validar por `id_institucion` en capa de servicio/repositorio.
- No introducir migradores automaticos (Flyway/Liquibase) sin solicitud explicita.

## Archivos clave
- Checklist funcional Sprint 1: [docs/checklist sprint 1.md](docs/checklist%20sprint%201.md)
- Checklist funcional Sprint 2: [docs/checklist sprint 2.md](docs/checklist%20sprint%202.md)
- Checklist Sprint Especial: [docs/checklist sprint especial.md](docs/checklist%20sprint%20especial.md)
- Script SQL base + seed: [scripts/db/db-script.sql](scripts/db/db-script.sql)
- Migraciones Sprint 2: `scripts/db/sprint-aulas-migration.sql`, `sprint2-horarios-migration.sql`, `sprint2-asistencia-migration.sql`, `sprint2-calificaciones-migration.sql`
- Migración Sprint Especial: [scripts/db/sprint-especial-saas-migration.sql](scripts/db/sprint-especial-saas-migration.sql)
- Script deploy ECR: [scripts/deploy/deploy-to-ecr.ps1](scripts/deploy/deploy-to-ecr.ps1)
- Config runtime: [src/main/resources/application.yml](src/main/resources/application.yml)

## Pitfalls frecuentes
- Si `mvn spring-boot:run` falla por class version, revisar JDK de Maven (`mvn -v`) y corregir `JAVA_HOME`.
- Error 500 en login puede ser JWT mal configurado (`JWT_SECRET` no base64 valido o demasiado corto).
- No asumir acceso publico en S3: el backend usa URLs pre-firmadas para lectura de archivos.

## Limites al editar
- Minimizar cambios: no reformatear modulos no relacionados.
- Mantener nombres/contratos de endpoints existentes salvo pedido explicito.
- Si hay cambios inesperados no solicitados en archivos relacionados, pausar y confirmar antes de continuar.
