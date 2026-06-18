"""
Seed extendido BULK SQL v2 - queries mas simples
"""
import asyncio, asyncpg, uuid
from datetime import datetime, timezone

DSN = "postgresql://postgres:postgres@dbvpay.cfiek6gqkqd5.us-east-1.rds.amazonaws.com:5432/vpayDB"
NOW = datetime(2026, 6, 9, tzinfo=timezone.utc)


async def run():
    conn = await asyncpg.connect(DSN)
    iid = await conn.fetchval("SELECT id FROM sia.institucion WHERE codigo='CSM-001'")

    # 1. Actividades (bulk desde curso_materia)
    n = await conn.execute("""
        INSERT INTO sia.actividad_evaluativa(id,id_institucion,id_periodo_evaluacion,id_materia,id_docente,nombre_actividad,dimension,fecha_actividad,puntaje_maximo,estado,creado_en,actualizado_en)
        SELECT gen_random_uuid(), ad.id_institucion, pe.id, ad.id_materia, ad.id_docente,
               d.dim || ' - ' || m.codigo || ' T' || pe.numero_periodo, d.dim,
               pe.fecha_inicio + (CASE WHEN d.dim='SABER' THEN 5 ELSE 20 END)::int,
               CASE WHEN d.dim='SABER' THEN 40 ELSE 45 END,
               'PUBLICADA', $1::timestamptz, $1::timestamptz
        FROM (
            SELECT DISTINCT id_institucion, id_materia, id_docente
            FROM sia.asignacion_docente WHERE id_institucion = $2
        ) ad
        CROSS JOIN (VALUES ('SABER'), ('HACER')) d(dim)
        JOIN sia.periodo_evaluacion pe ON pe.id_institucion = ad.id_institucion
        JOIN sia.materia m ON m.id = ad.id_materia
        ON CONFLICT DO NOTHING
    """, NOW, iid)
    print(f"Actividades: {n.replace('INSERT 0 ','') if 'INSERT' in str(n) else n}")

    # 2. Calificaciones actividad
    n = await conn.execute("""
        INSERT INTO sia.calificacion_actividad(id,id_institucion,id_actividad,id_estudiante,nota_obtenida,estado,creado_en,actualizado_en)
        SELECT gen_random_uuid(), a.id_institucion, a.id, e.id,
               round((a.puntaje_maximo * 0.3 + random() * (a.puntaje_maximo * 0.7))::numeric, 2),
               'REGISTRADA', $1::timestamptz, $1::timestamptz
        FROM sia.actividad_evaluativa a
        JOIN sia.inscripcion i ON i.id_institucion = a.id_institucion
        JOIN sia.curso_materia cm ON cm.id_materia = a.id_materia AND cm.id_institucion = a.id_institucion
        JOIN sia.paralelo p ON p.id_curso = cm.id_curso AND p.id_institucion = a.id_institucion
        JOIN sia.estudiante e ON e.id = i.id_estudiante
        WHERE a.id_institucion = $2 AND a.estado = 'PUBLICADA'
          AND i.id_paralelo = p.id
        ON CONFLICT DO NOTHING
    """, NOW, iid)
    print(f"Calificaciones actividad: {str(n)[:50]}")

    # 3. Calificacion SER bulk
    n = await conn.execute("""
        INSERT INTO sia.calificacion_ser(id,id_institucion,id_periodo_evaluacion,id_estudiante,id_materia,id_docente,nota_ser,estado,creado_en,actualizado_en)
        SELECT gen_random_uuid(), pe.id_institucion, pe.id, e.id, m.id,
               (SELECT ad.id_docente FROM sia.asignacion_docente ad WHERE ad.id_institucion = pe.id_institucion AND ad.id_materia = m.id LIMIT 1),
               round((5 + random()*5)::numeric, 2), 'REGISTRADA', $1::timestamptz, $1::timestamptz
        FROM sia.periodo_evaluacion pe
        CROSS JOIN sia.estudiante e
        CROSS JOIN sia.materia m
        WHERE pe.id_institucion = $2 AND e.id_institucion = $2 AND m.id_institucion = $2
        ON CONFLICT DO NOTHING
    """, NOW, iid)
    print(f"Calificaciones SER: {str(n)[:50]}")

    # 4. Autoevaluaciones bulk
    n = await conn.execute("""
        INSERT INTO sia.autoevaluacion_trimestral(id,id_institucion,id_periodo_evaluacion,id_estudiante,id_materia,nota_autoevaluacion,estado,creado_en,actualizado_en)
        SELECT gen_random_uuid(), pe.id_institucion, pe.id, e.id, m.id,
               round((2 + random()*3)::numeric, 2), 'REGISTRADA', $1::timestamptz, $1::timestamptz
        FROM sia.periodo_evaluacion pe
        CROSS JOIN sia.estudiante e
        CROSS JOIN sia.materia m
        WHERE pe.id_institucion = $2 AND e.id_institucion = $2 AND m.id_institucion = $2
        ON CONFLICT DO NOTHING
    """, NOW, iid)
    print(f"Autoevaluaciones: {str(n)[:50]}")

    # 5. Horarios con slots distribuidos para evitar solapamiento
    aulas = await conn.fetch("SELECT id FROM sia.aula WHERE id_institucion=$1 ORDER BY nombre", iid)
    asigs = await conn.fetch("SELECT id, id_materia FROM sia.asignacion_docente WHERE id_institucion=$1", iid)
    horarios_sql = []
    slots = [("08:00","09:15"),("09:30","10:45"),("11:00","12:15"),("14:00","15:15"),("15:30","16:45")]
    for ai, asig in enumerate(asigs):
        for h in range(2):
            aula = aulas[(ai + h*5 + ai//5) % len(aulas)]
            slot_idx = (ai + h*3) % len(slots)
            dia = ["LUNES","MARTES","MIERCOLES","JUEVES","VIERNES"][(ai // 5 + h*2) % 5]
            h_ini, h_fin = slots[slot_idx]
            horarios_sql.append(f"('{uuid.uuid4()}','{iid}','{asig['id']}','{aula['id']}','{dia}','{h_ini}','{h_fin}','ACTIVO','{NOW}','{NOW}')")
    # Insert por lotes para evitar conflictos
    for i in range(0, len(horarios_sql), 50):
        batch = ",".join(horarios_sql[i:i+50])
        try:
            await conn.execute(f"""
                INSERT INTO sia.horario_clase(id,id_institucion,id_asignacion_docente,id_aula,dia_semana,hora_inicio,hora_fin,estado,creado_en,actualizado_en)
                VALUES {batch}
                ON CONFLICT DO NOTHING
            """)
        except asyncpg.RaiseError:
            # Insert one by one if batch fails due to trigger
            for row in horarios_sql[i:i+50]:
                try:
                    await conn.execute(f"""
                        INSERT INTO sia.horario_clase(id,id_institucion,id_asignacion_docente,id_aula,dia_semana,hora_inicio,hora_fin,estado,creado_en,actualizado_en)
                        VALUES {row}
                        ON CONFLICT DO NOTHING
                    """)
                except asyncpg.RaiseError:
                    pass
    print(f"Horarios intentados: {len(horarios_sql)}")

    # 6. Asistencia registro (8 semanas)
    n = await conn.execute("""
        INSERT INTO sia.asistencia_registro(id,id_institucion,id_asignacion_docente,fecha,estado,creado_en,actualizado_en)
        SELECT gen_random_uuid(), $2, ad.id,
               (SELECT g.fecha_inicio + s.s * 7 FROM sia.gestion_academica g WHERE g.id_institucion = $2 AND g.activa = true LIMIT 1),
               'REGISTRADA', $1::timestamptz, $1::timestamptz
        FROM sia.asignacion_docente ad
        CROSS JOIN (SELECT generate_series(0, 7) s) s(s)
        WHERE ad.id_institucion = $2
        ON CONFLICT DO NOTHING
    """, NOW, iid)
    print(f"Asistencia registros: {str(n)[:50]}")

    # 7. Asistencia detalle bulk
    n = await conn.execute("""
        INSERT INTO sia.asistencia_detalle(id,id_asistencia_registro,id_inscripcion,estado_asistencia,creado_en,actualizado_en)
        SELECT gen_random_uuid(), ar.id, i.id,
               (CASE floor(random()*100)::int
                    WHEN 0 THEN 'AUSENTE' WHEN 1 THEN 'AUSENTE' WHEN 2 THEN 'AUSENTE'
                    WHEN 3 THEN 'AUSENTE' WHEN 4 THEN 'AUSENTE' WHEN 5 THEN 'AUSENTE'
                    WHEN 6 THEN 'AUSENTE' WHEN 7 THEN 'AUSENTE'
                    WHEN 8 THEN 'TARDANZA' WHEN 9 THEN 'TARDANZA' WHEN 10 THEN 'TARDANZA'
                    WHEN 11 THEN 'TARDANZA' WHEN 12 THEN 'TARDANZA' WHEN 13 THEN 'TARDANZA' WHEN 14 THEN 'TARDANZA'
                    WHEN 15 THEN 'JUSTIFICADO' WHEN 16 THEN 'JUSTIFICADO' WHEN 17 THEN 'JUSTIFICADO'
                    WHEN 18 THEN 'JUSTIFICADO' WHEN 19 THEN 'JUSTIFICADO'
                    ELSE 'PRESENTE'
               END),
               $1::timestamptz, $1::timestamptz
        FROM sia.asistencia_registro ar
        JOIN sia.asignacion_docente ad ON ad.id = ar.id_asignacion_docente
        JOIN sia.inscripcion i ON i.id_paralelo = ad.id_paralelo AND i.id_institucion = ar.id_institucion
        WHERE ar.id_institucion = $2
        ON CONFLICT DO NOTHING
    """, NOW, iid)
    print(f"Asistencia detalles: {str(n)[:50]}")

    # Verificacion final
    for t in ["actividad_evaluativa","calificacion_actividad","calificacion_ser","autoevaluacion_trimestral","horario_clase","asistencia_registro","asistencia_detalle"]:
        c = await conn.fetchval(f"SELECT count(*) FROM sia.{t}")
        print(f"  sia.{t}: {c}")

    await conn.close()
    print("=== SEED EXTENDIDO COMPLETADO ===")


asyncio.run(run())
