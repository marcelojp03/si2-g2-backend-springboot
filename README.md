# SI2 G2 — Backend Spring Boot

[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-23-orange.svg)](https://openjdk.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue.svg)](https://www.postgresql.org/)
[![JWT](https://img.shields.io/badge/Auth-JWT_Stateless-yellow.svg)](https://jwt.io/)
[![AWS](https://img.shields.io/badge/Deploy-AWS_App_Runner-232F3E.svg)](https://aws.amazon.com/apprunner/)

Backend principal del Sistema de Gestión Académica SaaS — UAGRM Sistemas de Información 2, Grupo 2.

---

## Stack

| Capa | Tecnología |
|---|---|
| Framework | Spring Boot 3.5 |
| Lenguaje | Java 23 |
| Build | Maven |
| Base de datos | PostgreSQL 17 (schema `sia`) |
| Auth | JWT stateless — claims: `id_institucion`, `roles`, `plan_codigo`, `modulos_activos`, `permisos` |
| Almacenamiento archivos | AWS S3 (URLs pre-firmadas) |
| Deploy | AWS ECR + App Runner |

---

## Módulos

| Paquete | Sprint | Descripción |
|---|---|---|
| `auth/` | 1 | Login, register, JWT |
| `institucion/` | 1 | CRUD instituciones y configuración |
| `usuario/` | 1 | Usuarios y roles |
| `academico/` | 1 | Gestiones académicas |
| `curso/` | 1 | Cursos y paralelos |
| `materia/` | 1 | Materias y asignación a cursos |
| `docente/` | 1 | Perfil docente |
| `estudiante/` | 1 | Padrón estudiantil |
| `tutor/` | 1 | Tutores y vínculo estudiante-tutor |
| `inscripcion/` | 1 | Inscripción de estudiantes |
| `asignacion/` | 1 | Asignación docente a materia/paralelo |
| `auditoria/` | 1+ | Bitácora de auditoría (todas las operaciones críticas) |
| `storage/` | 1+ | Integración S3, archivos y referencias |
| `aula/` | 2 | Aulas |
| `horario/` | 2 | Horarios de clase |
| `asistencia/` | 2 | Sesiones y registros de asistencia |
| `calificacion/` | 2 | Tipos de evaluación, evaluaciones y calificaciones |
| `dashboard/` | 2 | Métricas institucionales |
| `saas/` | Especial | Planes, suscripciones y solicitudes de onboarding |
| `reporte/` | Especial | Reportes con filtros, analíticos, gerenciales y dinámicos |
| `respaldo/` | Especial | Backups por tenant en S3 y restauraciones |
| `ia/` | Especial | Bridge hacia FastAPI (consulta natural, riesgo, voz) |
| `notificacion/` | 3 | Comunicados y notificaciones push (FCM) |

---

## Requisitos previos

- JDK 23 ([Adoptium](https://adoptium.net/))
- Maven 3.9+
- PostgreSQL 17 (local o RDS)

Verificar versión de Java usada por Maven:

```bash
mvn -v
```

---

## Configuración

### Variables de entorno

Crear `.env` en la raíz del proyecto (mismo nivel que `pom.xml`):

```env
DB_URL=jdbc:postgresql://localhost:5432/sia_db
DB_USERNAME=postgres
DB_PASSWORD=tu_password

JWT_SECRET=base64_largo_minimo_256bits
JWT_EXPIRATION=86400000

AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=AKIA...
AWS_SECRET_ACCESS_KEY=...
AWS_S3_BUCKET=nombre-del-bucket

FASTAPI_BASE_URL=http://localhost:8001
```

> El archivo `application.yml` carga estas variables con `${VAR_NAME}`. No commitear `.env`.

---

## Levantar el servidor

```bash
cd si2-g2-backend-springboot

# Compilar
mvn compile -q

# Ejecutar (carga .env automáticamente)
mvn spring-boot:run

# Build JAR
mvn package -DskipTests
```

El servidor queda disponible en `http://localhost:2026/api/`.

---

## Base de datos

**Sin Flyway ni Liquibase.** Las migraciones son manuales:

```bash
# Aplicar schema completo (primera vez)
psql -U postgres -d sia_db -f scripts/db/db-script.sql

# Aplicar migración Sprint Especial SaaS
psql -U postgres -d sia_db -f scripts/db/sprint-especial-saas-migration.sql
```

Todas las tablas viven en el schema `sia`. Toda tabla de negocio tiene `id_institucion`, `creado_en`, `actualizado_en`.

---

## Estructura del proyecto

```
src/main/java/com/uagrm/si2g2/
├── auth/              — Login, register, JWT
├── common/            — ApiResponse<T>, GlobalExceptionHandler, SecurityUtils
├── config/            — SecurityConfig, CorsConfig, DataInitializer
├── security/          — JwtService, JwtAuthFilter
├── tenant/            — TenantContext (ThreadLocal de id_institucion)
├── auditoria/         — BitacoraAuditoria entity + AuditoriaService
├── storage/           — S3Service, ArchivoController
├── institucion/       — Institucion, ConfiguracionInstitucion
├── usuario/           — Usuario, Rol, UsuarioRol
├── academico/         — GestionAcademica
├── curso/             — Curso, Paralelo
├── materia/           — Materia, CursoMateria
├── docente/           — Docente
├── estudiante/        — Estudiante
├── tutor/             — Tutor, EstudianteTutor
├── inscripcion/       — Inscripcion
├── asignacion/        — AsignacionDocente
├── aula/              — Aula
├── horario/           — HorarioClase
├── asistencia/        — SesionAsistencia, RegistroAsistencia
├── calificacion/      — TipoEvaluacion, Evaluacion, RegistroCalificacion
├── dashboard/         — Métricas por institución
├── saas/              — plan/, suscripcion/, solicitud/
├── reporte/           — api/, application/
├── respaldo/          — api/, application/, domain/
├── ia/                — Bridge FastAPI; api/, application/, dto/
└── notificacion/      — api/, application/, dto/
```

Cada módulo sigue la estructura:

```
modulo/
├── api/          — XxxController.java
├── application/  — XxxService.java
├── domain/       — Xxx.java (entidad), XxxRepository.java
└── dto/          — XxxRequest.java, XxxResponse.java
```

---

## Roles del sistema

| Código | Alcance |
|---|---|
| `SUPER_ADMIN` | Global, sin `id_institucion` |
| `ADMIN_INSTITUCION` | Gestión completa de su institución |
| `DIRECTOR` | Lectura, reportes y alertas |
| `SECRETARIO` | Operación académica |
| `DOCENTE` | Sus asignaciones, asistencia y calificaciones |
| `ESTUDIANTE` | Consulta de sus propios datos (app móvil) |
| `TUTOR` | Consulta de datos del estudiante vinculado |

---

## Deploy

```powershell
# Build imagen y push a ECR
powershell -ExecutionPolicy Bypass -File scripts/deploy/deploy-to-ecr.ps1
```

| Entorno | URL |
|---|---|
| Local | `http://localhost:2026` |
| Producción | `https://s7hwsnmsxf.us-east-1.awsapprunner.com` |
