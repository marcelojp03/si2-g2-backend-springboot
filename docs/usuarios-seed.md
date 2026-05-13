# Usuarios de datos sinteticos

El backend incluye un seeder idempotente para crear datos de prueba consistentes. Se puede ejecutar varias veces sin duplicar registros.

## Ejecutar seeder

1. Inicia sesion como super admin:

```text
superadmin@example.com
change_me_super_admin
```

2. Ejecuta el endpoint con el token JWT:

```bash
curl -X POST http://localhost:2026/api/seed/synthetic \
  -H "Authorization: Bearer <TOKEN>"
```

El endpoint requiere rol `SUPER_ADMIN`.

## Institucion demo

```text
Codigo: SEED-001
Nombre: Colegio Demo Semilla
Gestion: Gestion Academica Demo 2026
```

## Usuarios generados

Todos los usuarios sinteticos usan la misma contrasena:

```text
Demo12345!
```

| Correo | Rol | Nombre | Referencia |
| --- | --- | --- | --- |
| admin.demo@si2.test | ADMIN_INSTITUCION | Admin Institucion Demo | Acceso administrativo de institucion |
| director.demo@si2.test | DIRECTOR | Daniel Quiroga | Director academico |
| secretaria.demo@si2.test | SECRETARIO | Mariela Ribera | Secretaria academica |
| docente.mate.demo@si2.test | DOCENTE | Ana Rojas | Docente de Matematica |
| docente.lenguaje.demo@si2.test | DOCENTE | Carlos Mendez | Docente de Lenguaje |
| estudiante.lucia.demo@si2.test | ESTUDIANTE | Lucia Vargas | Estudiante de 1ro Primaria A |
| estudiante.mateo.demo@si2.test | ESTUDIANTE | Mateo Flores | Estudiante de 1ro Primaria A |
| estudiante.sofia.demo@si2.test | ESTUDIANTE | Sofia Rojas | Estudiante de 2do Primaria A |
| tutor.maria.demo@si2.test | TUTOR | Maria Lopez | Tutora de Lucia y Sofia |
| tutor.jorge.demo@si2.test | TUTOR | Jorge Flores | Tutor de Mateo |

## Datos academicos

El seeder crea o reutiliza:

- 1 institucion demo.
- 1 gestion academica activa.
- 3 cursos con paralelo A.
- 3 materias.
- Relaciones curso-materia.
- 2 docentes.
- 3 estudiantes.
- 2 tutores.
- Relaciones estudiante-tutor.
- Inscripciones y asignaciones docentes.
