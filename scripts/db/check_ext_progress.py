import asyncio, asyncpg

DSN = "postgresql://postgres:postgres@dbvpay.cfiek6gqkqd5.us-east-1.rds.amazonaws.com:5432/vpayDB"

async def run():
    conn = await asyncpg.connect(DSN)
    rows = await conn.fetch("SELECT id, codigo, nombre FROM sia.institucion ORDER BY codigo")
    for r in rows:
        iid = r["id"]
        a = await conn.fetchval("SELECT count(*) FROM sia.actividad_evaluativa WHERE id_institucion=$1", iid)
        c = await conn.fetchval("SELECT count(*) FROM sia.calificacion_actividad WHERE id_institucion=$1", iid)
        h = await conn.fetchval("SELECT count(*) FROM sia.horario_clase WHERE id_institucion=$1", iid)
        s = await conn.fetchval("SELECT count(*) FROM sia.calificacion_ser WHERE id_institucion=$1", iid)
        print(f"{r['codigo']:20s} act={a:5d} cal={c:6d} hor={h:4d} ser={s:5d}")
    await conn.close()

asyncio.run(run())
