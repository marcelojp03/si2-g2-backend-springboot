import asyncio, asyncpg
async def run():
    conn = await asyncpg.connect("postgresql://postgres:postgres@dbvpay.cfiek6gqkqd5.us-east-1.rds.amazonaws.com:5432/vpayDB", timeout=10)
    u = await conn.fetchrow("SELECT id FROM sia.usuario WHERE correo = 'robertocervantes@gmail.com'")
    if u:
        roles = await conn.fetch("SELECT r.codigo FROM sia.usuario_rol ur JOIN sia.rol r ON r.id=ur.id_rol WHERE ur.id_usuario=$1", u["id"])
        print(f"Roles: {[r['codigo'] for r in roles]}")
        if not roles:
            print("SIN ROLES - restaurando ADMIN_INSTITUCION")
    else:
        print("USUARIO NO EXISTE")
    await conn.close()
asyncio.run(run())
