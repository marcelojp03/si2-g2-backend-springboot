# Usuarios del Sistema - Colegio San Miguel

## Credenciales de Acceso

**Contraseña para todos los usuarios:** `Colegio2026!`

---

## Resumen de Usuarios

| Rol | Cantidad | Prefijo Correo |
|-----|----------|---------------|
| SUPER_ADMIN | 1 | superadmin@example.com |
| ADMIN_INSTITUCION | 1 | admin@sanmiguel.edu.bo |
| DIRECTOR | 1 | director@sanmiguel.edu.bo |
| SECRETARIO | 1 | secretaria@sanmiguel.edu.bo |
| DOCENTE | 20 | docente.N@sanmiguel.edu.bo |
| ESTUDIANTE | 480 | estudiante.NNNN@sanmiguel.edu.bo |

**Total: 504 usuarios**

---

## Administradores

| Correo | Rol | Estado |
|--------|-----|--------|
| superadmin@example.com | SUPER_ADMIN | ACTIVO |
| admin@sanmiguel.edu.bo | ADMIN_INSTITUCION | ACTIVO |
| director@sanmiguel.edu.bo | DIRECTOR | ACTIVO |
| secretaria@sanmiguel.edu.bo | SECRETARIO | ACTIVO |

---

## Docentes (20)

| # | Correo | Especialidad |
|---|--------|--------------|
| 1 | docente.1@sanmiguel.edu.bo | Matematica |
| 2 | docente.2@sanmiguel.edu.bo | Lenguaje |
| 3 | docente.3@sanmiguel.edu.bo | Ciencias Naturales |
| 4 | docente.4@sanmiguel.edu.bo | Ciencias Sociales |
| 5 | docente.5@sanmiguel.edu.bo | Educacion Fisica |
| 6 | docente.6@sanmiguel.edu.bo | Musica |
| 7 | docente.7@sanmiguel.edu.bo | Artes |
| 8 | docente.8@sanmiguel.edu.bo | Ingles |
| 9 | docente.9@sanmiguel.edu.bo | Fisica |
| 10 | docente.10@sanmiguel.edu.bo | Quimica |
| 11 | docente.11@sanmiguel.edu.bo | Biologia |
| 12 | docente.12@sanmiguel.edu.bo | Filosofia |
| 13 | docente.13@sanmiguel.edu.bo | Informatica |
| 14 | docente.14@sanmiguel.edu.bo | Matematica |
| 15 | docente.15@sanmiguel.edu.bo | Lenguaje |
| 16 | docente.16@sanmiguel.edu.bo | Ciencias Naturales |
| 17 | docente.17@sanmiguel.edu.bo | Ciencias Sociales |
| 18 | docente.18@sanmiguel.edu.bo | Educacion Fisica |
| 19 | docente.19@sanmiguel.edu.bo | Musica |
| 20 | docente.20@sanmiguel.edu.bo | Artes |

---

## Estudiantes (480)

### Primeria (1ro a 6to) - Paralelos A y B
- estudiante.0001@sanmiguel.edu.bo hasta estudiante.0240@sanmiguel.edu.bo
- 20 estudiantes por paralelo x 6 cursos x 2 paralelos = 240 estudiantes

### Secundaria (1ro a 6to) - Paralelos A y B
- estudiante.0241@sanmiguel.edu.bo hasta estudiante.0480@sanmiguel.edu.bo
- 20 estudiantes por paralelo x 6 cursos x 2 paralelos = 240 estudiantes

---

## Datos del Colegio

| Campo | Valor |
|-------|-------|
| Codigo | CSM-001 |
| Nombre | Colegio San Miguel |
| Tipo | PRIVADO |
| Ciudad | Santa Cruz |
| Direccion | Av. Centro #456, Santa Cruz - Bolivia |
| Dominio | sanmiguel.edu.bo |
| Gestion | Gestion Academica 2026 (Feb 3 - Nov 28, 2026) |

---

## Estructura Academica

### Cursos
- **Primaria:** PRI-1 (1ro) hasta PRI-6 (6to)
- **Secundaria:** SEC-1 (1ro) hasta SEC-6 (6to)

### Paralelos
- 2 paralelos por curso (A y B)
- Total: 24 paralelos

### Materias

**Primaria (8 materias):**
MAT, LEN, CN, CS, VER, APV, EFD, MUS

**Secundaria (12 materias):**
MAT, LEN, CS, VER, APV, EFD, ING, TT, BIO, FIS, QUI, FIL

---

## Tutores (480)

Un tutor principal por cada estudiante (480 tutores)
- Correo: null (sin cuenta de usuario)
- Parentesco: Padre/Madre segun corresponda

---

## Estadisticas

| Entidad | Cantidad |
|---------|----------|
| Instituciones | 1 |
| Usuarios | 504 |
| Docentes | 20 |
| Estudiantes | 480 |
| Tutores | 480 |
| Cursos | 12 |
| Paralelos | 24 |
| Materias | 14 |
| Aulas | 24 |
| Asignaciones docente | ~280 |
| Inscripciones | 480 |

---

## Notas

- El seeder crea automaticamente la estructura academica completa
- Los estudiantes tienen usuarios con correo para acceso al sistema
- Los tutores NO tienen usuario (solo linkage a estudiante)
- La contrasena es la misma para todos: `Colegio2026!`
- Para re-ejecutar el seeder, reiniciar el backend con los contenedores caidos