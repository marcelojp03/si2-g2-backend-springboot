import asyncio, asyncpg
DSN = "postgresql://postgres:postgres@dbvpay.cfiek6gqkqd5.us-east-1.rds.amazonaws.com:5432/vpayDB"
async def run():
    conn = await asyncpg.connect(DSN)
    for t in ["calificacion_actividad","actividad_evaluativa","evaluacion_materia","calificacion_ser","autoevaluacion_trimestral"]:
        try:
            await conn.execute(f"DELETE FROM sia.{t}")
            print(f"Cleared sia.{t}")
        except Exception as e:
            print(f"Error sia.{t}: {e}")
    await conn.close()
asyncio.run(run())
