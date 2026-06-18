import asyncio, asyncpg
DSN = "postgresql://postgres:postgres@dbvpay.cfiek6gqkqd5.us-east-1.rds.amazonaws.com:5432/vpayDB"
async def run():
    conn = await asyncpg.connect(DSN)
    rows = await conn.fetch("SELECT id, codigo, nombre FROM sia.institucion")
    for r in rows:
        iid = r["id"]
        c = await conn.fetchval("SELECT count(*) FROM sia.curso WHERE id_institucion=$1", iid)
        e = await conn.fetchval("SELECT count(*) FROM sia.estudiante WHERE id_institucion=$1", iid)
        print(f"{r['codigo']:20s} {r['nombre']:35s} cursos={c} estudiantes={e}")
    await conn.close()
asyncio.run(run())
