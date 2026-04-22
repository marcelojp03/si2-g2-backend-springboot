# Checklist Backend — SI2 Grupo 2

> Stack: Spring Boot 3.5.0 · Java 23 · PostgreSQL (RDS) · Schema `sia` · JWT · Maven  
> Multitenancy: Shared DB, aislamiento por `id_institucion`  

---

## SPRINT 1 — Autenticación y Base de Datos

### Infraestructura del proyecto
- [x] Proyecto Spring Boot 3.5.0 creado (Java 23, Maven, jar)
- [x] `application.yml` configurado (datasource, JPA `ddl-auto: none`, JWT, puerto 2026)
- [x] Dependencias: Spring Security, Spring Data JPA, PostgreSQL driver, Lombok, jjwt 0.12.6

### Seguridad / JWT
- [x] `JwtService` — generación y validación de tokens
- [x] `JwtAuthFilter` — extracción del Bearer token, validación, escritura en SecurityContext
- [x] `SecurityConfig` — CSRF deshabilitado, `/api/auth/**` público, stateless, filtro JWT
- [x] `ApplicationConfig` — `UserDetailsService`, `AuthenticationProvider`, `PasswordEncoder` (BCrypt)

### Seguridad / Autorización por rol
- [ ] Restringir endpoints sensibles por rol (`hasRole` / `hasAnyRole`) en backend
- [ ] Definir matriz mínima de acceso por rol (`SUPER_ADMIN`, `ADMIN_INSTITUCION`, `DIRECTOR`, `DOCENTE`)
- [ ] Verificar que `id_institucion` del JWT no sea manipulable desde el request

### Módulo `auth/`
- [x] `Usuario.java` — entidad JPA, implementa `UserDetails`, login por `correo`, multitenancy por `id_institucion`
- [x] `Rol.java` — entidad JPA para tabla `rol`
- [x] `UsuarioRepository` — `findByCorreo`
- [x] `RolRepository` — `findByCodigo`
- [x] `AuthService` — `register()` y `login()`, JWT claims con `id_institucion` + `roles`
- [x] `AuthController` — `POST /api/auth/register`, `POST /api/auth/login`
- [x] `LoginRequest`, `RegisterRequest`, `AuthResponse` DTOs

### Base de datos
- [x] `scripts/db/db-script.sql` — schema completo versión 2 corregida
  - [x] Extensión `pgcrypto`, función `fn_actualizar_actualizado_en()`
  - [x] Tablas: `institucion`, `configuracion_institucion`, `rol`, `usuario`, `usuario_rol`
  - [x] Tablas: `gestion_academica`, `curso`, `paralelo`, `materia`, `curso_materia`
  - [x] Tablas: `docente`, `estudiante`, `tutor`, `estudiante_tutor`
  - [x] Tablas: `inscripcion`, `asignacion_docente`
  - [x] 16 triggers `BEFORE UPDATE` para `actualizado_en`
  - [x] Índices parciales: gestión activa única por institución, tutor principal único por estudiante
  - [x] UNIQUE constraints de correo en `docente`, `estudiante`, `tutor`
  - [x] Seed: 6 roles, institución UEM-001, 3 configuraciones iniciales
  - [x] UNIQUE constraints corregidas (sin `NULLS NOT DISTINCT` en campos nullable de `docente`, `estudiante`, `tutor`)
- [ ] Integrar Flyway para versionar migraciones SQL
- [ ] Crear migración inicial `V1__init_schema.sql` desde `db-script.sql`
- [ ] Ejecutar `db-script.sql` contra RDS PostgreSQL (schema `sia`)
- [ ] Verificar conexión desde app con `currentSchema=sia` y `default_schema=sia`

### Verificación Sprint 1
- [x] `mvn clean package` — BUILD SUCCESS con JDK 23
- [ ] App inicia contra PostgreSQL real (`spring-boot:run` sin errores)
- [ ] `POST /api/auth/register` devuelve token JWT válido
- [ ] `POST /api/auth/login` devuelve token JWT válido
- [ ] Token contiene claims `id_institucion` y `roles`

### Validación y manejo de errores
- [ ] Bean Validation en DTOs (`@NotBlank`, `@Email`, `@Size`, etc.)
- [ ] `@Valid` en controllers para activar validaciones
- [ ] `GlobalExceptionHandler` con `@ControllerAdvice`
- [ ] Manejo estándar: `EntityNotFoundException`, `BadRequest`, `AccessDenied`, `MethodArgumentNotValid`
- [ ] Formato uniforme de respuestas de error para el frontend

### Observabilidad mínima
- [ ] Logs estructurados en auth y errores críticos
- [ ] Endpoint `/actuator/health` habilitado

### Preparación para auditoría
- [ ] Definir estrategia de bitácora de auditoría
- [ ] Identificar operaciones críticas que deben auditarse (login, CRUD sensibles)
- [ ] Capturar IP, usuario autenticado, endpoint y operación en eventos auditables

---

## SPRINT 2 — Módulos de Negocio (pendiente)

### Infraestructura común
- [ ] `common/exception/` — `GlobalExceptionHandler` (@ControllerAdvice), `ApiException`
- [ ] `common/dto/` — `ApiResponse<T>` wrapper estándar para todas las respuestas
- [ ] `common/entity/` — `BaseEntity` con `id`, `creado_en`, `actualizado_en`, `estado`
- [ ] Convención de soft delete / estados (`ACTIVO`, `INACTIVO`) aplicada en entidades

### Multitenancy real
- [ ] `TenantContext` — almacena `id_institucion` por request (ThreadLocal o RequestScope)
- [ ] Extraer `id_institucion` desde JWT en `JwtAuthFilter` y escribirlo en `TenantContext`
- [ ] Repositories con filtros obligatorios por `id_institucion`
- [ ] Validar que un usuario no pueda consultar datos de otra institución

### CORS
- [ ] Configurar CORS por ambiente (dev / prod)
- [ ] Permitir origen del frontend Angular en desarrollo y producción

### tenancy/
- [ ] Extracción de `id_institucion` desde JWT claims (para filtrar queries)

### Módulo `instituciones/`
- [ ] Entidad `Institucion.java`
- [ ] Entidad `ConfiguracionInstitucion.java`
- [ ] `InstitucionRepository`, `ConfiguracionInstitucionRepository`
- [ ] `InstitucionService` — CRUD con filtro por `id_institucion`
- [ ] `InstitucionController` — endpoints REST

### Módulo `academico/` (Gestiones, Cursos, Paralelos, Materias)
- [ ] Entidades: `GestionAcademica`, `Curso`, `Paralelo`, `Materia`, `CursoMateria`
- [ ] Repositories correspondientes
- [ ] Services con validaciones de negocio (ej. solo una gestión ACTIVA por institución)
- [ ] Controllers REST

### Módulo `docentes/`
- [ ] Entidad `Docente.java`
- [ ] `DocenteRepository` — búsqueda por `id_institucion`
- [ ] `DocenteService` — CRUD, asociación con `usuario`
- [ ] `DocenteController` — endpoints REST

### Módulo `estudiantes/`
- [ ] Entidad `Estudiante.java`
- [ ] `EstudianteRepository`
- [ ] `EstudianteService`
- [ ] `EstudianteController`

### Módulo `tutores/`
- [ ] Entidad `Tutor.java`
- [ ] Entidad `EstudianteTutor.java` (relación N:N con atributos)
- [ ] `TutorRepository`, `EstudianteTutorRepository`
- [ ] `TutorService`
- [ ] `TutorController`

### Módulo `inscripciones/`
- [ ] Entidad `Inscripcion.java`
- [ ] `InscripcionRepository`
- [ ] `InscripcionService` — reglas: no duplicar inscripción activa por gestión
- [ ] `InscripcionController`

### Módulo `asignaciones/`
- [ ] Entidad `AsignacionDocente.java`
- [ ] `AsignacionDocenteRepository`
- [ ] `AsignacionDocenteService`
- [ ] `AsignacionDocenteController`

---

## SPRINT 3 — Funcionalidades Avanzadas (pendiente)

### Docker y despliegue
- [ ] Crear `Dockerfile` para backend Spring Boot
- [ ] Probar build local de imagen Docker
- [ ] Configurar variables de entorno para imagen (datasource, JWT secret, etc.)
- [ ] Crear `application-prod.yml` / secretos en variables de entorno

### Calidad y tests
- [ ] Tests unitarios de services críticos
- [ ] Tests de seguridad para login y acceso por rol
- [ ] Tests de integración con Spring Boot Test + Testcontainers
- [ ] Documentación API con Springdoc OpenAPI / Swagger UI

### Funcionalidades avanzadas
- [ ] Gestión de contraseñas — cambio obligatorio (`requiere_cambio_contrasena`)
- [ ] Paginación y filtros avanzados en listados
- [ ] Validaciones de negocio complejas
- [ ] Implementación completa de bitácora de auditoría (`bitacora_auditoria`)

---

## Consideraciones pendientes

- [ ] Definir estrategia de sesión: JWT simple o access + refresh token + logout real
- [ ] Confirmar si se mantiene Java 23 o se migra a Java 21 LTS para despliegue en App Runner

---

## Notas técnicas

```
# Siempre usar JDK 23 antes de ejecutar Maven en PowerShell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-23"
.\mvnw.cmd spring-boot:run

# Ejecutar schema en RDS PostgreSQL (schema sia)
psql -h <RDS_HOST> -U <USUARIO> -d <DB_NAME> -f scripts/db/db-script.sql
# El script ya incluye: CREATE SCHEMA IF NOT EXISTS sia; SET search_path TO sia;
```
