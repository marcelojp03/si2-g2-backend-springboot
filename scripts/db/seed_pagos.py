"""
Seed pagos v3: planes separados por nivel (Primaria/Secundaria).
Limpia y regenera planes + cuotas para todas las instituciones.
"""
import asyncio, asyncpg, uuid
from datetime import date

DSN = "postgresql://postgres:postgres@dbvpay.cfiek6gqkqd5.us-east-1.rds.amazonaws.com:5432/vpayDB"

async def run():
    conn = await asyncpg.connect(DSN, timeout=10)
    
    insts = await conn.fetch("SELECT id, codigo FROM sia.institucion ORDER BY codigo")
    
    for inst in insts:
        iid = inst["id"]
        cod = inst["codigo"]
        print(f"\n=== {cod} ===")

        # Limpiar datos previos
        await conn.execute("DELETE FROM sia.pago p USING sia.cuota_estudiante c WHERE c.id=p.id_cuota AND c.id_institucion=$1", iid)
        await conn.execute("DELETE FROM sia.cuota_estudiante WHERE id_institucion=$1", iid)
        await conn.execute("DELETE FROM sia.plan_pago WHERE id_institucion=$1", iid)
        await conn.execute("UPDATE sia.inscripcion SET id_plan_pago=NULL WHERE id_institucion=$1", iid)
        
        # Crear 2 planes
        planes = {}
        for nivel, monto in [("PRIMARIA", 150), ("SECUNDARIA", 200)]:
            pid = uuid.uuid4()
            await conn.execute("""
                INSERT INTO sia.plan_pago(id,id_institucion,nombre,tipo_periodo,monto,moneda,cantidad_cuotas,dia_vencimiento,activo,creado_en,actualizado_en)
                VALUES($1,$2,$3,'MENSUAL',$4,'BOB',10,15,true,NOW(),NOW())
            """, pid, iid, f"Pension {nivel.title()} 2026", monto)
            planes[nivel] = {"id": pid, "monto": monto}
            print(f"  Plan {nivel}: Bs {monto}/mes x 10 cuotas")
        
        # Asignar plan segun nivel del curso y generar cuotas
        total_asignadas = 0
        for nivel, key in [("Primaria", "PRIMARIA"), ("Secundaria", "SECUNDARIA")]:
            pid = planes[key]["id"]
            monto = planes[key]["monto"]
            
            # Inscripciones activas de este nivel
            inscs = await conn.fetch("""
                SELECT i.id, i.id_estudiante, i.id_gestion_academica
                FROM sia.inscripcion i
                JOIN sia.paralelo p ON p.id = i.id_paralelo
                JOIN sia.curso c ON c.id = p.id_curso
                WHERE i.id_institucion = $1 AND i.estado = 'ACTIVA' AND c.nivel = $2
            """, iid, nivel)
            
            for ins in inscs:
                await conn.execute("UPDATE sia.inscripcion SET id_plan_pago=$1 WHERE id=$2", pid, ins["id"])
                
                for cn in range(1, 11):
                    try: venc = date(2026, cn + 1, 15)
                    except: venc = date(2026, cn + 1, 28)
                    await conn.execute("""
                        INSERT INTO sia.cuota_estudiante(id,id_institucion,id_estudiante,id_plan_pago,id_gestion_academica,numero_cuota,monto,fecha_vencimiento,estado,creado_en,actualizado_en)
                        VALUES($1,$2,$3,$4,$5,$6,$7,$8,'PENDIENTE',NOW(),NOW())
                    """, uuid.uuid4(), iid, ins["id_estudiante"], pid, ins["id_gestion_academica"], cn, monto, venc)
                    total_asignadas += 1
            
            print(f"  {nivel}: {len(inscs)} estudiantes, {len(inscs)*10} cuotas")
        
        print(f"  Total cuotas generadas: {total_asignadas}")
    
    # Resumen
    print(f"\n=== RESUMEN ===")
    for r in await conn.fetch("SELECT codigo, nombre FROM sia.institucion ORDER BY codigo"):
        iid = await conn.fetchval("SELECT id FROM sia.institucion WHERE codigo=$1", r["codigo"])
        planes = await conn.fetch("SELECT nombre, monto FROM sia.plan_pago WHERE id_institucion=$1", iid)
        cuotas = await conn.fetchval("SELECT count(*) FROM sia.cuota_estudiante WHERE id_institucion=$1", iid)
        ins = await conn.fetchval("SELECT count(*) FROM sia.inscripcion WHERE id_institucion=$1 AND id_plan_pago IS NOT NULL", iid)
        for p in planes:
            print(f"  {r['codigo']:20s} {p['nombre']:30s} Bs {p['monto']}")
        print(f"  {'':20s} {'':30s} cuotas={cuotas} insc_con_plan={ins}")
    
    await conn.close()
    print("\n=== COMPLETADO ===")

asyncio.run(run())
