import sys, io, asyncio
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

import asyncpg

DSN = "postgresql://postgres:postgres@dbvpay.cfiek6gqkqd5.us-east-1.rds.amazonaws.com:5432/vpayDB"


async def contar(conn, tabla):
    try:
        r = await conn.fetchrow(f"SELECT count(*) FROM sia.{tabla}")
        return r[0]
    except Exception:
        return None


async def run():
    conn = await asyncpg.connect(DSN)

    # 1. Conteo general
    print("=" * 60)
    print("1. CONTEO GENERAL POR TABLA")
    print("=" * 60)
    tablas = [
        "institucion", "usuario", "rol", "estudiante", "docente", "tutor",
        "curso", "paralelo", "materia", "curso_materia",
        "inscripcion", "asignacion_docente",
        "evaluacion", "calificacion_actividad",
        "aula", "gestion_academica",
    ]
    for t in tablas:
        n = await contar(conn, t)
        if n is None:
            print(f"  sia.{t:30s} -> NO EXISTE")
        else:
            print(f"  sia.{t:30s} -> {n:6d} registros")

    print()
    print("=" * 60)
    print("2. DISTRIBUCION POR GENERO")
    print("=" * 60)
    try:
        rows = await conn.fetch("""
            SELECT e.id_institucion, u.sexo, count(*)
            FROM sia.estudiante e JOIN sia.usuario u ON u.id = e.id_usuario
            GROUP BY e.id_institucion, u.sexo ORDER BY e.id_institucion
        """)
        for r in rows:
            print(f"  inst={str(r[0])[:8]} sexo={r[1]:10s} -> {r[2]:4d}")
    except Exception as e:
        print(f"  Error: {e}")

    print()
    print("=" * 60)
    print("3. EVALUACIONES")
    print("=" * 60)
    try:
        r = await conn.fetchrow("SELECT count(*) FROM sia.evaluacion")
        print(f"  Total evaluaciones: {r[0]}")
        if r[0] > 0:
            r2 = await conn.fetch("""
                SELECT e.id_materia, m.nombre, e.nombre, e.fecha
                FROM sia.evaluacion e
                LEFT JOIN sia.materia m ON m.id = e.id_materia
                LIMIT 20
            """)
            for row in r2:
                print(f"    materia={str(row[0])[:8]} nom={row[1]:15s} eval={row[2]:20s} fecha={row[3]}")
    except Exception as e:
        print(f"  Error: {e}")

    print()
    print("=" * 60)
    print("4. CALIFICACIONES")
    print("=" * 60)
    try:
        r = await conn.fetchrow("SELECT count(*) FROM sia.calificacion_actividad")
        print(f"  Total calificaciones: {r[0]}")
    except Exception as e:
        print(f"  Error: {e}")

    print()
    print("=" * 60)
    print("5. CURSOS Y PARALELOS")
    print("=" * 60)
    try:
        rows = await conn.fetch("""
            SELECT c.nombre, count(p.id) paralelos
            FROM sia.curso c LEFT JOIN sia.paralelo p ON p.id_curso = c.id AND p.id_institucion = c.id_institucion
            GROUP BY c.id, c.nombre ORDER BY c.nombre
        """)
        for r in rows:
            print(f"  {r[0]:25s} -> {r[1]} paralelos")
    except Exception as e:
        print(f"  Error: {e}")

    print()
    print("=" * 60)
    print("6. MATERIAS POR CURSO")
    print("=" * 60)
    try:
        rows = await conn.fetch("""
            SELECT c.nombre, count(cm.id_materia) materias
            FROM sia.curso c
            LEFT JOIN sia.curso_materia cm ON cm.id_curso = c.id AND cm.id_institucion = c.id_institucion
            GROUP BY c.id, c.nombre ORDER BY c.nombre
        """)
        for r in rows:
            print(f"  {r[0]:25s} -> {r[1]} materias")
    except Exception as e:
        print(f"  Error: {e}")

    print()
    print("=" * 60)
    print("7. MATERIAS SIN EVALUACIONES")
    print("=" * 60)
    try:
        rows = await conn.fetch("""
            SELECT m.nombre,
                   (SELECT count(*) FROM sia.evaluacion e WHERE e.id_materia = m.id AND e.id_institucion = m.id_institucion) evals
            FROM sia.materia m
            ORDER BY evals
        """)
        for r in rows:
            print(f"  {r[0]:25s} -> {r[1]} evaluaciones")
    except Exception as e:
        print(f"  Error: {e}")

    print()
    print("=" * 60)
    print("8. ESTUDIANTES POR INSTITUCION")
    print("=" * 60)
    try:
        rows = await conn.fetch("""
            SELECT i.nombre, count(e.id)
            FROM sia.estudiante e
            JOIN sia.institucion i ON i.id = e.id_institucion
            GROUP BY i.id, i.nombre ORDER BY i.nombre
        """)
        for r in rows:
            print(f"  {r[0]:30s} -> {r[1]} estudiantes")
    except Exception as e:
        print(f"  Error: {e}")

    print()
    print("=" * 60)
    print("9. INSCRIPCIONES POR PARALELO")
    print("=" * 60)
    try:
        rows = await conn.fetch("""
            SELECT p.nombre, c.nombre curso, count(i.id) inscritos
            FROM sia.inscripcion i
            JOIN sia.paralelo p ON p.id = i.id_paralelo
            JOIN sia.curso c ON c.id = p.id_curso
            GROUP BY p.nombre, c.nombre ORDER BY c.nombre, p.nombre
        """)
        for r in rows:
            print(f"  {r[1]:15s} {r[0]:5s} -> {r[2]} inscritos")
    except Exception as e:
        print(f"  Error: {e}")

    await conn.close()


asyncio.run(run())
