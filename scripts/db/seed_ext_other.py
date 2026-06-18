"""
Seed extension para UEM-001, UEC-002, BOLIVIAN-ECEC.
SQL directo mas rapido que JPA. Solo crea datos si no existen.
"""
import asyncio, asyncpg, uuid, random
from datetime import date, timedelta

DSN = "postgresql://postgres:postgres@dbvpay.cfiek6gqkqd5.us-east-1.rds.amazonaws.com:5432/vpayDB"

async def run():
    conn = await asyncpg.connect(DSN)
    random.seed(2026)

    for row in await conn.fetch("SELECT id, codigo FROM sia.institucion WHERE codigo != 'CSM-001'"):
        iid = row["id"]
        cod = row["codigo"]
        print(f"\n=== Extension {cod} ===")

        g = await conn.fetchrow("SELECT id, fecha_inicio, fecha_fin FROM sia.gestion_academica WHERE id_institucion=$1 AND activa=true", iid)
        if not g:
            print("  No gestion activa")
            continue
        gid, g_ini, g_fin = g["id"], g["fecha_inicio"], g["fecha_fin"]

        # 1. Periodos (3 trimestres) - usar IDs existentes o crear
        dias = (g_fin - g_ini).days
        tercio = dias // 3
        for pn in range(1, 4):
            pi = g_ini + timedelta(days=(pn-1)*tercio)
            pf = g_ini + timedelta(days=pn*tercio-1) if pn < 3 else g_fin
            await conn.execute("""
                INSERT INTO sia.periodo_evaluacion(id,id_institucion,id_gestion_academica,numero_periodo,tipo_periodo,fecha_inicio,fecha_fin,estado,peso_ser,peso_saber,peso_hacer,peso_auto,creado_en,actualizado_en)
                VALUES(gen_random_uuid(),$1,$2,$3,'TRIMESTRAL',$4,$5,'ABIERTO',10,45,40,5,NOW(),NOW()) ON CONFLICT(id_institucion,id_gestion_academica,numero_periodo) DO NOTHING
            """, iid, gid, pn, pi, pf)
        # Obtener IDs reales
        pids = [r["id"] for r in await conn.fetch(
            "SELECT id FROM sia.periodo_evaluacion WHERE id_institucion=$1 AND id_gestion_academica=$2 ORDER BY numero_periodo", iid, gid)]
        print(f"  Periodos: {len(pids)}")

        materias = await conn.fetch("SELECT id, codigo FROM sia.materia WHERE id_institucion=$1", iid)
        estudiantes = await conn.fetch("SELECT id FROM sia.estudiante WHERE id_institucion=$1", iid)
        docentes = await conn.fetch("SELECT id FROM sia.docente WHERE id_institucion=$1", iid)
        if not materias or not estudiantes:
            print("  Sin materias o estudiantes")
            continue

        # 2. Evaluaciones materia
        for m in materias:
            for pn in range(1, 4):
                for ti, tp in enumerate(["PARCIAL","TRABAJO_PRACTICO"]):
                    pond = 40 if ti == 0 else 60
                    await conn.execute("""
                        INSERT INTO sia.evaluacion_materia(id,id_institucion,id_materia,periodo,tipo,nombre,ponderacion,escala,estado,creado_en,actualizado_en)
                        VALUES(gen_random_uuid(),$1,$2,$3,$4,$5,$6,'NUMERICA','ABIERTA',NOW(),NOW()) ON CONFLICT(id_institucion,id_materia,periodo,nombre) DO NOTHING
                    """, iid, m["id"], pn, tp, f"{tp} {pn}er Trimestre", pond)

        # 3. Actividades
        act_count = 0
        for m in materias:
            doc = docentes[abs(hash(m["codigo"])) % len(docentes)]
            for pid, pn in [(pids[0],1),(pids[1],2),(pids[2],3)]:
                pi = g_ini + timedelta(days=(pn-1)*tercio)
                for dim, pmax in [("SABER",40),("HACER",45)]:
                    try:
                        await conn.execute("""
                            INSERT INTO sia.actividad_evaluativa(id,id_institucion,id_periodo_evaluacion,id_materia,id_docente,nombre_actividad,dimension,fecha_actividad,puntaje_maximo,estado,creado_en,actualizado_en)
                            VALUES(gen_random_uuid(),$1,$2,$3,$4,$5,$6,$7,$8,'PUBLICADA',NOW(),NOW()) ON CONFLICT(id_institucion,id_periodo_evaluacion,nombre_actividad) DO NOTHING
                        """, iid, pid, m["id"], doc["id"], f"{dim} - {m['codigo']} T{pn}", dim, pi + timedelta(days=5 if dim=="SABER" else 20), pmax)
                        act_count += 1
                    except:
                        pass
        print(f"  Actividades: {act_count}")

        # 4. Calificaciones bulk
        await conn.execute(f"""
            INSERT INTO sia.calificacion_actividad(id,id_institucion,id_actividad,id_estudiante,nota_obtenida,estado,creado_en,actualizado_en)
            SELECT gen_random_uuid(), '{iid}'::uuid, a.id, e.id,
                   round((a.puntaje_maximo * 0.3 + random() * (a.puntaje_maximo * 0.7))::numeric, 2),
                   'REGISTRADA', NOW(), NOW()
            FROM sia.actividad_evaluativa a, sia.estudiante e
            WHERE a.id_institucion = '{iid}'::uuid AND e.id_institucion = '{iid}'::uuid
              AND NOT EXISTS (SELECT 1 FROM sia.calificacion_actividad ca WHERE ca.id_actividad=a.id AND ca.id_estudiante=e.id)
        """)
        print(f"  Calificaciones OK")

        # 5. Calificacion SER + Autoevaluacion bulk
        for m in materias:
            for pid in pids:
                await conn.execute(f"""
                    INSERT INTO sia.calificacion_ser(id,id_institucion,id_periodo_evaluacion,id_estudiante,id_materia,id_docente,nota_ser,estado,creado_en,actualizado_en)
                    SELECT gen_random_uuid(), '{iid}'::uuid, '{pid}'::uuid, e.id, '{m["id"]}'::uuid,
                           (SELECT ad.id_docente FROM sia.asignacion_docente ad WHERE ad.id_institucion='{iid}'::uuid AND ad.id_materia='{m["id"]}'::uuid LIMIT 1),
                           round((5+random()*5)::numeric,2), 'REGISTRADA', NOW(), NOW()
                    FROM sia.estudiante e WHERE e.id_institucion = '{iid}'::uuid
                    ON CONFLICT DO NOTHING
                """)
                await conn.execute(f"""
                    INSERT INTO sia.autoevaluacion_trimestral(id,id_institucion,id_periodo_evaluacion,id_estudiante,id_materia,nota_autoevaluacion,estado,creado_en,actualizado_en)
                    SELECT gen_random_uuid(), '{iid}'::uuid, '{pid}'::uuid, e.id, '{m["id"]}'::uuid,
                           round((2+random()*3)::numeric,2), 'REGISTRADA', NOW(), NOW()
                    FROM sia.estudiante e WHERE e.id_institucion = '{iid}'::uuid
                    ON CONFLICT DO NOTHING
                """)
        print(f"  SER + Auto OK")

        # 6. Horarios
        aulas = await conn.fetch("SELECT id FROM sia.aula WHERE id_institucion=$1 ORDER BY nombre", iid)
        asignaciones = await conn.fetch("SELECT id, id_materia, id_paralelo FROM sia.asignacion_docente WHERE id_institucion=$1", iid)
        slots = [("08:00","09:15"),("09:30","10:45"),("11:00","12:15"),("14:00","15:15"),("15:30","16:45")]
        hcnt = 0
        for ai, asig in enumerate(asignaciones):
            for h in range(2):
                aula = aulas[(ai + h*5 + ai//5) % len(aulas)]
                si = (ai + h*3) % len(slots)
                dia = ["LUNES","MARTES","MIERCOLES","JUEVES","VIERNES"][(ai//5 + h*2) % 5]
                hi, hf = slots[si]
                try:
                    await conn.execute("""
                        INSERT INTO sia.horario_clase(id,id_institucion,id_asignacion_docente,id_aula,dia_semana,hora_inicio,hora_fin,estado,creado_en,actualizado_en)
                        VALUES($1,$2,$3,$4,$5,$6::time,$7::time,'ACTIVO',NOW(),NOW()) ON CONFLICT DO NOTHING
                    """, uuid.uuid4(), iid, asig["id"], aula["id"], dia, hi, hf)
                    hcnt += 1
                except:
                    pass
        print(f"  Horarios: {hcnt}")

        # 7. Asistencias
        insc = await conn.fetch("SELECT id, id_paralelo FROM sia.inscripcion WHERE id_institucion=$1", iid)
        ins_pp = {}
        for ins in insc:
            pp = ins["id_paralelo"]
            if pp not in ins_pp:
                ins_pp[pp] = []
            ins_pp[pp].append(ins["id"])
        inicio = g_ini + timedelta(weeks=2)
        rcnt = dcnt = 0
        for asig in asignaciones:
            ins_ids = ins_pp.get(asig["id_paralelo"], [])
            if not ins_ids: continue
            for s in range(8):
                fecha = inicio + timedelta(weeks=s)
                registro_id = uuid.uuid4()
                try:
                    await conn.execute("""
                        INSERT INTO sia.asistencia_registro(id,id_institucion,id_asignacion_docente,fecha,estado,creado_en,actualizado_en)
                        VALUES($1,$2,$3,$4,'REGISTRADA',NOW(),NOW()) ON CONFLICT DO NOTHING
                    """, registro_id, iid, asig["id"], fecha)
                    rcnt += 1
                    for ins_id in ins_ids:
                        roll = random.random()
                        acc = 0
                        ea = "PRESENTE"
                        for i, (est, peso) in enumerate(zip(["PRESENTE","AUSENTE","TARDANZA","JUSTIFICADO"],[0.80,0.08,0.07,0.05])):
                            acc += peso
                            if roll <= acc:
                                ea = est
                                break
                        try:
                            await conn.execute("""
                                INSERT INTO sia.asistencia_detalle(id,id_institucion,id_asistencia_registro,id_inscripcion,estado_asistencia,creado_en,actualizado_en)
                                VALUES($1,$2,$3,$4,$5,NOW(),NOW()) ON CONFLICT DO NOTHING
                            """, uuid.uuid4(), iid, registro_id, ins_id, ea)
                            dcnt += 1
                        except:
                            pass
                except:
                    pass
        print(f"  Asistencias: {rcnt} registros, {dcnt} detalles")

    # Verificacion final
    print("\n=== VERIFICACION FINAL ===")
    for r in await conn.fetch("SELECT codigo, nombre FROM sia.institucion ORDER BY codigo"):
        iid = await conn.fetchval("SELECT id FROM sia.institucion WHERE codigo=$1", r["codigo"])
        for t in ["actividad_evaluativa","calificacion_actividad","calificacion_ser","autoevaluacion_trimestral","horario_clase","asistencia_registro"]:
            c = await conn.fetchval(f"SELECT count(*) FROM sia.{t} WHERE id_institucion=$1", iid)
            print(f"  {r['codigo']:20s} {t:30s} -> {c}")

    await conn.close()
    print("\n=== COMPLETADO ===")

asyncio.run(run())
