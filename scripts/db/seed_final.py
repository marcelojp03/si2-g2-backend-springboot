import asyncio, asyncpg
from datetime import date, timedelta
import random

DSN = "postgresql://postgres:postgres@dbvpay.cfiek6gqkqd5.us-east-1.rds.amazonaws.com:5432/vpayDB"
random.seed(2026)

async def run():
    conn = await asyncpg.connect(DSN)
    for row in await conn.fetch("SELECT id, codigo FROM sia.institucion WHERE codigo IN ('UEC-002','BOLIVIAN-ECEC')"):
        iid, cod = row["id"], row["codigo"]
        print(f"\n=== {cod} ===")
        g = await conn.fetchrow("SELECT id, fecha_inicio, fecha_fin FROM sia.gestion_academica WHERE id_institucion=$1 AND activa=true", iid)
        if not g: continue
        gid, g_ini, g_fin = g["id"], g["fecha_inicio"], g["fecha_fin"]
        dias = (g_fin - g_ini).days
        tercio = dias // 3

        # Periodos
        for pn in range(1, 4):
            pi = g_ini + timedelta(days=(pn-1)*tercio)
            pf = g_ini + timedelta(days=pn*tercio-1) if pn < 3 else g_fin
            await conn.execute("INSERT INTO sia.periodo_evaluacion(id,id_institucion,id_gestion_academica,numero_periodo,tipo_periodo,fecha_inicio,fecha_fin,estado,peso_ser,peso_saber,peso_hacer,peso_auto,creado_en,actualizado_en) VALUES(gen_random_uuid(),$1,$2,$3,'TRIMESTRAL',$4,$5,'ABIERTO',10,45,40,5,NOW(),NOW()) ON CONFLICT DO NOTHING", iid, gid, pn, pi, pf)

        pids = [r["id"] for r in await conn.fetch("SELECT id FROM sia.periodo_evaluacion WHERE id_institucion=$1 ORDER BY numero_periodo", iid)]
        mids = await conn.fetch("SELECT id, codigo FROM sia.materia WHERE id_institucion=$1", iid)
        dids = await conn.fetch("SELECT id FROM sia.docente WHERE id_institucion=$1", iid)

        # Evaluaciones
        for m in mids:
            for pn in range(1, 4):
                for ti, tp in enumerate(["PARCIAL","TRABAJO_PRACTICO"]):
                    pond = 40 if ti == 0 else 60
                    await conn.execute("INSERT INTO sia.evaluacion_materia(id,id_institucion,id_materia,periodo,tipo,nombre,ponderacion,escala,estado,creado_en,actualizado_en) VALUES(gen_random_uuid(),$1,$2,$3,$4,$5,$6,'NUMERICA','ABIERTA',NOW(),NOW()) ON CONFLICT(id_institucion,id_materia,periodo,nombre) DO NOTHING", iid, m["id"], pn, tp, f"{tp} {pn}er Trimestre", pond)

        # Actividades
        for m in mids:
            doc = dids[abs(hash(m["codigo"])) % len(dids)]
            for pid, pn in [(pids[0],1),(pids[1],2),(pids[2],3)]:
                pi = g_ini + timedelta(days=(pn-1)*tercio)
                for dim, pmax in [("SABER",40),("HACER",45)]:
                    fecha = pi + timedelta(days=5 if dim=="SABER" else 20)
                    await conn.execute("INSERT INTO sia.actividad_evaluativa(id,id_institucion,id_periodo_evaluacion,id_materia,id_docente,nombre_actividad,dimension,fecha_actividad,puntaje_maximo,estado,creado_en,actualizado_en) VALUES(gen_random_uuid(),$1,$2,$3,$4,$5,$6,$7,$8,'PUBLICADA',NOW(),NOW()) ON CONFLICT(id_institucion,id_periodo_evaluacion,nombre_actividad) DO NOTHING", iid, pid, m["id"], doc["id"], f"{dim} - {m['codigo']} T{pn}", dim, fecha, pmax)
        print("  Actividades OK")

        # Calificaciones bulk
        await conn.execute(f"""
            INSERT INTO sia.calificacion_actividad(id,id_institucion,id_actividad,id_estudiante,nota_obtenida,estado,creado_en,actualizado_en)
            SELECT gen_random_uuid(), '{iid}'::uuid, a.id, e.id,
                   round((a.puntaje_maximo * 0.3 + random() * (a.puntaje_maximo * 0.7))::numeric, 2),
                   'REGISTRADA', NOW(), NOW()
            FROM sia.actividad_evaluativa a, sia.estudiante e
            WHERE a.id_institucion = '{iid}'::uuid AND e.id_institucion = '{iid}'::uuid
            ON CONFLICT DO NOTHING
        """)
        print("  Calificaciones OK")

        # SER + Auto bulk
        for m in mids:
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
        print("  SER + Auto OK")

    # Resumen final
    print("\n=== RESUMEN FINAL ===")
    for r in await conn.fetch("SELECT codigo FROM sia.institucion ORDER BY codigo"):
        iid = await conn.fetchval("SELECT id FROM sia.institucion WHERE codigo=$1", r["codigo"])
        for t in ["actividad_evaluativa","calificacion_actividad","calificacion_ser","periodo_evaluacion"]:
            c = await conn.fetchval(f"SELECT count(*) FROM sia.{t} WHERE id_institucion=$1", iid)
            print(f"  {r['codigo']:20s} {t:25s} -> {c}")
    await conn.close()

asyncio.run(run())
