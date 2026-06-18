import asyncio, asyncpg
DSN = "postgresql://postgres:postgres@dbvpay.cfiek6gqkqd5.us-east-1.rds.amazonaws.com:5432/vpayDB"
async def run():
    conn = await asyncpg.connect(DSN)
    rows = await conn.fetch("""
        SELECT conname, pg_get_constraintdef(oid) as def
        FROM pg_constraint 
        WHERE conrelid = 'sia.actividad_evaluativa'::regclass
          AND contype = 'c'
    """)
    for r in rows:
        print(f"{r['conname']}: {r['def']}")
    await conn.close()
asyncio.run(run())
