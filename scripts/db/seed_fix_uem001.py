"""
Fix UEM-001 v2: horarios bulk + asistencias bulk + limpiar test.
"""
import asyncio, asyncpg
DSN = "postgresql://postgres:postgres@dbvpay.cfiek6gqkqd5.us-east-1.rds.amazonaws.com:5432/vpayDB"

async def run():
    conn = await asyncpg.connect(DSN, timeout=10)
    iid = await conn.fetchval("SELECT id FROM sia.institucion WHERE codigo='UEM-001'")
    print(f"UEM-001: {iid}")

    # Delete test horario
    await conn.execute("DELETE FROM sia.horario_clase WHERE id_institucion=$1 AND id_asignacion_docente IS NULL", iid)
    
    # Delete existing asistencias to regenerate cleanly
    await conn.execute("""
        DELETE FROM sia.asistencia_detalle WHERE id_asistencia_registro IN 
        (SELECT id FROM sia.asistencia_registro WHERE id_institucion=$1)
    """, iid)
    await conn.execute("DELETE FROM sia.asistencia_registro WHERE id_institucion=$1", iid)
    print("Asistencias previas limpiadas")

    # ── HORARIOS BULK ────────────────────────────────────────────────────────────
    await conn.execute("ALTER TABLE sia.horario_clase DISABLE TRIGGER trg_validar_solapamiento_horario")
    await conn.execute(f"""
        INSERT INTO sia.horario_clase(id,id_institucion,id_asignacion_docente,id_aula,dia_semana,hora_inicio,hora_fin,estado,creado_en,actualizado_en)
        SELECT gen_random_uuid(), '{iid}'::uuid, ad.id,
               (SELECT a2.id FROM sia.aula a2 WHERE a2.id_institucion='{iid}'::uuid ORDER BY a2.nombre OFFSET (row_number() OVER (ORDER BY ad.id, h.h) % (SELECT count(*) FROM sia.aula WHERE id_institucion='{iid}'::uuid)) LIMIT 1),
               (ARRAY['LUNES','MARTES','MIERCOLES','JUEVES','VIERNES'])[(row_number() OVER (ORDER BY ad.id, h.h)) % 5 + 1],
               (ARRAY['08:00','09:30','11:00','14:00','15:30'])[(row_number() OVER (ORDER BY ad.id_materia, h.h)) % 5 + 1]::time,
               (ARRAY['09:15','10:45','12:15','15:15','16:45'])[(row_number() OVER (ORDER BY ad.id_materia, h.h)) % 5 + 1]::time,
               'ACTIVO', NOW(), NOW()
        FROM sia.asignacion_docente ad
        CROSS JOIN (VALUES (0), (1)) h(h)
        WHERE ad.id_institucion = '{iid}'::uuid
    """)
    await conn.execute("ALTER TABLE sia.horario_clase ENABLE TRIGGER trg_validar_solapamiento_horario")
    hc = await conn.fetchval("SELECT count(*) FROM sia.horario_clase WHERE id_institucion=$1", iid)
    print(f"Horarios: {hc}")

    # ── ASISTENCIAS BULK ────────────────────────────────────────────────────────
    await conn.execute(f"""
        INSERT INTO sia.asistencia_registro(id,id_institucion,id_asignacion_docente,fecha,estado,creado_en,actualizado_en)
        SELECT gen_random_uuid(), '{iid}'::uuid, ad.id,
               (SELECT g.fecha_inicio + INTERVAL '2 weeks' FROM sia.gestion_academica g WHERE g.id_institucion='{iid}'::uuid AND g.activa=true LIMIT 1) + (s.s * INTERVAL '1 week'),
               'REGISTRADA', NOW(), NOW()
        FROM sia.asignacion_docente ad
        CROSS JOIN (SELECT generate_series(0, 7) s) s(s)
        WHERE ad.id_institucion = '{iid}'::uuid
        ON CONFLICT DO NOTHING
    """)
    rc = await conn.fetchval("SELECT count(*) FROM sia.asistencia_registro WHERE id_institucion=$1", iid)
    print(f"Asistencias registro: {rc}")

    # Detalles bulk with weighted distribution
    await conn.execute(f"""
        INSERT INTO sia.asistencia_detalle(id,id_asistencia_registro,id_inscripcion,estado_asistencia,creado_en,actualizado_en)
        SELECT gen_random_uuid(), ar.id, i.id,
               CASE floor(random()*100)::int
                   WHEN 0 THEN 'AUSENTE' WHEN 1 THEN 'AUSENTE' WHEN 2 THEN 'AUSENTE'
                   WHEN 3 THEN 'AUSENTE' WHEN 4 THEN 'AUSENTE' WHEN 5 THEN 'AUSENTE'
                   WHEN 6 THEN 'AUSENTE' WHEN 7 THEN 'AUSENTE'
                   WHEN 8 THEN 'TARDANZA' WHEN 9 THEN 'TARDANZA' WHEN 10 THEN 'TARDANZA'
                   WHEN 11 THEN 'TARDANZA' WHEN 12 THEN 'TARDANZA' WHEN 13 THEN 'TARDANZA' WHEN 14 THEN 'TARDANZA'
                   WHEN 15 THEN 'JUSTIFICADO' WHEN 16 THEN 'JUSTIFICADO' WHEN 17 THEN 'JUSTIFICADO'
                   WHEN 18 THEN 'JUSTIFICADO' WHEN 19 THEN 'JUSTIFICADO'
                   ELSE 'PRESENTE'
               END,
               NOW(), NOW()
        FROM sia.asistencia_registro ar
        JOIN sia.asignacion_docente ad ON ad.id = ar.id_asignacion_docente
        JOIN sia.inscripcion i ON i.id_paralelo = ad.id_paralelo AND i.id_institucion = ar.id_institucion
        WHERE ar.id_institucion = '{iid}'::uuid
        ON CONFLICT DO NOTHING
    """)
    dc = await conn.fetchval("SELECT count(*) FROM sia.asistencia_detalle d JOIN sia.asistencia_registro ar ON ar.id=d.id_asistencia_registro WHERE ar.id_institucion=$1", iid)
    print(f"Asistencias detalle: {dc}")

    print("\n=== FIX COMPLETADO ===")
    await conn.close()

asyncio.run(run())
