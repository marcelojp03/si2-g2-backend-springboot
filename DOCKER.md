# Docker - Backend Spring Boot

Este proyecto ya incluye un `Dockerfile` multi-etapa que compila la aplicacion con Maven y ejecuta el JAR con Eclipse Temurin.

## Variables de entorno

Copia el archivo de ejemplo y completa los valores reales:

```bash
cp .env.example .env
```

El archivo `.env` no debe subirse a git.

Variables principales:

- `DB_URL`: URL JDBC de PostgreSQL, por ejemplo `jdbc:postgresql://host.docker.internal:5432/sia?currentSchema=sia`.
- `DB_USERNAME`: usuario de la base de datos.
- `DB_PASSWORD`: contrasena de la base de datos.
- `JWT_SECRET`: secreto para firmar tokens JWT.
- `SUPER_ADMIN_EMAIL` y `SUPER_ADMIN_PASSWORD`: credenciales iniciales del super admin.

## Construir imagen

```bash
docker build -t si2-g2-backend .
```

## Ejecutar contenedor

```bash
docker run --rm --env-file .env -p 2026:2026 si2-g2-backend
```

La API quedara disponible en:

```text
http://localhost:2026
```

El endpoint de salud queda disponible en:

```text
http://localhost:2026/actuator/health
```
