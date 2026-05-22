# AGENTS.md

Guia operativa para agentes de codigo en este backend Spring Boot.

## Alcance
- Proyecto: backend academico multi-tenant (schema compartido, aislamiento por `id_institucion`).
- Stack: Spring Boot 3.5, Java 23, Maven, PostgreSQL, JWT, AWS S3.
- Estructura modular por paquetes en `src/main/java/com/uagrm/si2g2` (auth, institucion, curso, estudiante, tutor, inscripcion, storage, etc.).

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
- Checklist funcional y alcance Sprint 1: [docs/checklist sprint 1.md](docs/checklist%20sprint%201.md)
- Script SQL base + seed: [scripts/db/db-script.sql](scripts/db/db-script.sql)
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
