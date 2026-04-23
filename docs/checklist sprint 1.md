# Checklist Backend — SI2 Grupo 2 · Sprint 1

> Stack: Spring Boot 3.5.0 · Java 23 · PostgreSQL (RDS) · Schema `sia` · JWT · Maven  
> Multitenancy: Shared DB, aislamiento por `id_institucion`  
> Migraciones: SQL manual (`scripts/db/`) — sin Flyway

---

## Historias de usuario Sprint 1

| ID | Historia | SP | Estado |
|----|----------|----|--------|
| HU-S1-01 | Registrar institución educativa | 2 | ✅ Completado |
| HU-S1-02 | Configurar institución educativa | 3 | ✅ Completado |
| HU-S1-03 | Iniciar sesión | 5 | ✅ Completado |
| HU-S1-04 | Gestionar usuarios | 5 | ✅ Completado |
| HU-S1-05 | Asignar roles a usuarios | 3 | ✅ Completado |
| HU-S1-06 | Gestionar gestión académica | 3 | ✅ Completado |
| HU-S1-07 | Gestionar cursos y paralelos | 5 | ✅ Completado |
| HU-S1-08 | Gestionar materias y asignarlas a cursos | 5 | ✅ Completado |
| HU-S1-09 | Gestionar docentes | 5 | ✅ Completado |
| HU-S1-10 | Gestionar estudiantes | 5 | ✅ Completado |
| HU-S1-11 | Gestionar tutores y vincularlos con estudiantes | 5 | ✅ Completado |
| HU-S1-12 | Inscribir estudiante en gestión, curso y paralelo | 5 | ✅ Completado |
| HU-S1-13 | Asignar docente a materia y paralelo | 5 | ✅ Completado |

**Total estimado:** 56 SP

---

## Bloque 1 — Base del proyecto
> Soporta todas las HU

- [x] Proyecto Spring Boot 3.5.0 creado (Java 23, Maven, jar)
- [x] Estructura modular por paquetes (`auth`, `common`, `config`, `security`)
- [x] Conexión a PostgreSQL RDS configurada (`currentSchema=sia`, `default_schema=sia`)
- [x] Variables de entorno via `.env` + `spring.config.import` (sin Flyway)
- [x] `ddl-auto: none` — schema gestionado por SQL manual (`scripts/db/db-script.sql`)
- [x] Schema `sia` ejecutado en RDS: tablas, triggers, índices, seed de roles e institución

---

## Bloque 2 — Seguridad y acceso
> Soporta: HU-S1-03, HU-S1-04, HU-S1-05

- [x] `JwtService` — generación y validación de tokens (jjwt 0.12.6)
- [x] `JwtAuthFilter` — extracción Bearer token, escritura en SecurityContext
- [x] `SecurityConfig` — CSRF off, stateless, CORS, `/api/auth/**` público, `/actuator/health` público
- [x] `ApplicationConfig` — `UserDetailsService`, `DaoAuthenticationProvider`, BCrypt
- [x] `AuthController` — `POST /api/auth/login`, `POST /api/auth/register`
- [x] `AuthService` — login, register, JWT con claims `id_institucion` + `roles`, validación email duplicado (409)
- [x] `RegisterRequest` — `codigoRol` opcional (default: `ADMIN_INSTITUCION`)
- [x] `DataInitializer` — crea `SUPER_ADMIN` al arrancar si no existe
- [x] CORS configurado — `app.cors.allowed-origins` en `application.yml`
- [x] `/actuator/health` habilitado y público
- [x] Restringir endpoints sensibles por rol (`@PreAuthorize`) — 60 anotaciones en 12 controllers
- [x] Definir y documentar matriz de acceso por rol:
  - [x] `SUPER_ADMIN` — acceso total
  - [x] `ADMIN_INSTITUCION` — gestión de su institución
  - [x] `DIRECTOR` — lectura/reportes de su institución
  - [x] `SECRETARIO` — gestión académica operativa
  - [x] `DOCENTE` — acceso a sus asignaciones
- [x] `GET /api/usuarios` — listar usuarios de la institución (HU-S1-04)
- [x] `PUT /api/usuarios/{id}` — editar usuario (HU-S1-04)
- [x] `DELETE /api/usuarios/{id}` — desactivar usuario (HU-S1-04)
- [x] `POST /api/usuarios/{id}/roles` — asignar rol (HU-S1-05)

---

## Bloque 3 — Institución y configuración
> Soporta: HU-S1-01, HU-S1-02

- [x] Entidad `Institucion.java`
- [x] Entidad `ConfiguracionInstitucion.java`
- [x] `InstitucionRepository`, `ConfiguracionInstitucionRepository`
- [x] `InstitucionService` — CRUD, validar unicidad de código
- [x] `InstitucionController` — `POST /api/instituciones`, `GET`, `PUT`
- [x] `ConfiguracionInstitucionService` — leer y actualizar configuración
- [x] `ConfiguracionInstitucionController` — `GET /api/instituciones/{id}/configuracion`, `PUT`

---

## Bloque 4 — Gestión académica
> Soporta: HU-S1-06

- [x] Entidad `GestionAcademica.java`
- [x] `GestionAcademicaRepository`
- [x] `GestionAcademicaService` — CRUD, validar única gestión ACTIVA por institución
- [x] `GestionAcademicaController` — `POST /api/gestiones`, `GET`, `PUT`, `DELETE`

---

## Bloque 5 — Cursos, paralelos y materias
> Soporta: HU-S1-07, HU-S1-08

- [x] Entidad `Curso.java`
- [x] Entidad `Paralelo.java`
- [x] Entidad `Materia.java`
- [x] Entidad `CursoMateria.java`
- [x] Repositories correspondientes
- [x] `CursoService` — CRUD con filtro por `id_institucion`
- [x] `ParaleloService` — CRUD, asociado a `Curso`
- [x] `MateriaService` — CRUD con filtro por `id_institucion`
- [x] `CursoMateriaService` — asignar/desasignar materias a cursos, validar duplicados
- [x] Controllers REST (`/api/cursos`, `/api/paralelos`, `/api/materias`)

---

## Bloque 6 — Personas académicas
> Soporta: HU-S1-09, HU-S1-10, HU-S1-11

- [x] Entidad `Docente.java`
- [x] Entidad `Estudiante.java`
- [x] Entidad `Tutor.java`
- [x] Entidad `EstudianteTutor.java` (N:N con atributos: `es_principal`, `parentesco`)
- [x] Repositories correspondientes
- [x] `DocenteService` — CRUD, filtro por `id_institucion`, asociación con `Usuario`
- [x] `EstudianteService` — CRUD, filtro por `id_institucion`
- [x] `TutorService` — CRUD, filtro por `id_institucion`
- [x] `EstudianteTutorService` — vincular tutor con estudiante, validar único tutor principal activo por estudiante
- [x] Controllers REST (`/api/docentes`, `/api/estudiantes`, `/api/tutores`)

---

## Bloque 7 — Operación académica
> Soporta: HU-S1-12, HU-S1-13

- [x] Entidad `Inscripcion.java`
- [x] Entidad `AsignacionDocente.java`
- [x] `InscripcionRepository`, `AsignacionDocenteRepository`
- [x] `InscripcionService` — inscribir estudiante en gestión+curso+paralelo, validar no duplicar inscripción activa
- [x] `AsignacionDocenteService` — asignar docente a materia+paralelo, validar sin duplicados
- [x] `InscripcionController` — `POST /api/inscripciones`, `GET`, `DELETE`
- [x] `AsignacionDocenteController` — `POST /api/asignaciones`, `GET`, `DELETE`

---

## Bloque 8 — Multitenancy
> Soporta todos los módulos de negocio

- [x] `TenantContext` — almacena `id_institucion` por request (ThreadLocal)
- [x] Extraer `id_institucion` desde JWT en `JwtAuthFilter` → escribir en `TenantContext`
- [x] Repositories con filtros obligatorios por `id_institucion` (aplicado en 11 services)
- [x] Validar que un usuario no pueda consultar datos de otra institución

---

## Bloque 9 — Validaciones y errores
> Soporta todas las HU

- [x] Bean Validation en DTOs (`@NotBlank`, `@Email`, `@Size`)
- [x] `@Valid` en controllers
- [x] `GlobalExceptionHandler` — 400, 401, 403, 404, 409, 500
- [x] `ApiResponse<T>` — wrapper uniforme para todas las respuestas
- [x] Logs estructurados en operaciones críticas (`@Slf4j` en `AuthService` y `GlobalExceptionHandler`)

---

## Bloque 10 — Auditoría mínima

- [x] Definir estrategia: tabla `bitacora_auditoria` (en `db-script.sql`) + `AuditoriaService`
- [x] Entidad `BitacoraAuditoria.java` (id_usuario, id_institucion, fecha_evento, direccion_ip, agente_usuario, nombre_modulo, tipo_operacion, nombre_entidad, exito, mensaje)
- [x] Registrar: login exitoso, login fallido, register

---

## Bloque 11 — Verificación y entregables
> Criterios de aceptación del Sprint 1

- [x] App compila (`mvn package` BUILD SUCCESS, Java 23)
- [x] App inicia contra PostgreSQL RDS sin errores
- [x] `POST /api/auth/login` — devuelve JWT con `id_institucion` y `roles`
- [x] `POST /api/auth/register` — devuelve JWT, asigna rol, valida email duplicado
- [x] App desplegada en AWS App Runner (`https://s7hwsnmsxf.us-east-1.awsapprunner.com`)
- [x] `GET /actuator/health` — responde `{"status":"UP"}`
- [ ] CRUD institución probado en Postman
- [ ] CRUD usuarios probado en Postman
- [ ] CRUD gestión académica probado en Postman
- [ ] CRUD cursos/paralelos/materias probado en Postman
- [ ] CRUD docentes/estudiantes/tutores probado en Postman
- [ ] Inscripción y asignación docente probadas en Postman
- [ ] Endpoints protegidos por rol verificados

---

## Notas técnicas

```powershell
# Arrancar la app (carga .env automáticamente via spring.config.import)
mvn spring-boot:run

# Ejecutar schema manualmente en RDS
psql -h <RDS_HOST> -U <USUARIO> -d <DB_NAME> -f scripts/db/db-script.sql
```
