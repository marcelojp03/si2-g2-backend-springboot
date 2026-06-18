import asyncio, asyncpg
DSN = "postgresql://postgres:postgres@dbvpay.cfiek6gqkqd5.us-east-1.rds.amazonaws.com:5432/vpayDB"
async def run():
    conn = await asyncpg.connect(DSN)
    for t in ["calificacion_ser","autoevaluacion_trimestral","actividad_evaluativa"]:
        cols = await conn.fetch(f"""
            SELECT column_name, is_nullable, data_type
            FROM information_schema.columns
            WHERE table_schema='sia' AND table_name=$1
            ORDER BY ordinal_position
        """, t)
        print(f"\n{t}:")
        for c in cols:
            print(f"  {c['column_name']:30s} {c['is_nullable']:5s} {c['data_type']}")
    await conn.close()
asyncio.run(run())
