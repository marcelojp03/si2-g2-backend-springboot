import asyncio, asyncpg
async def run():
    conn = await asyncpg.connect("postgresql://postgres:postgres@dbvpay.cfiek6gqkqd5.us-east-1.rds.amazonaws.com:5432/vpayDB", timeout=10)
    u = await conn.fetchrow("SELECT correo, hash_contrasena FROM sia.usuario WHERE correo LIKE 'robert%'")
    if u:
        print("Encontrado:", u["correo"])
        print("Hash:", str(u["hash_contrasena"])[:80])
    else:
        print("NO ENCONTRADO")
    await conn.close()
asyncio.run(run())
