import asyncio, asyncpg
DSN = "postgresql://postgres:postgres@dbvpay.cfiek6gqkqd5.us-east-1.rds.amazonaws.com:5432/vpayDB"
async def run():
    conn = await asyncpg.connect(DSN)
    iid = await conn.fetchval("SELECT id FROM sia.institucion WHERE codigo='CSM-001'")
    for t in ["calificacion_actividad","actividad_evaluativa","evaluacion_materia","periodo_evaluacion","calificacion_ser","autoevaluacion_trimestral"]:
        try: await conn.execute(f"DELETE FROM sia.{t}")
        except: print(f"  Cannot delete sia.{t}, trying with WHERE...")
        await conn.execute(f"DELETE FROM sia.{t} WHERE id_institucion=$1", iid)
    for t in ["asistencia_detalle","asistencia_registro","horario_clase"]:
        await conn.execute(f"DELETE FROM sia.{t}")
        print(f"Cleared sia.{t}")
    await conn.close()
asyncio.run(run())
