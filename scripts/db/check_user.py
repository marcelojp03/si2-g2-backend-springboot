import asyncio, asyncpg
DSN = "postgresql://postgres:postgres@dbvpay.cfiek6gqkqd5.us-east-1.rds.amazonaws.com:5432/vpayDB"
async def run():
    conn = await asyncpg.connect(DSN)
    u = await conn.fetchrow("SELECT id, correo, hash_contrasena FROM sia.usuario WHERE correo=$1", "robertocervantes@gmail.com")
    if u:
        print(f"Usuario encontrado: {u['correo']}")
        print(f"Hash: {str(u['hash_contrasena'])[:60]}...")
        # Check roles
        roles = await conn.fetch("""
            SELECT r.codigo FROM sia.usuario_rol ur
            JOIN sia.rol r ON r.id = ur.id_rol
            WHERE ur.id_usuario = $1
        """, u["id"])
        print(f"Roles: {[r['codigo'] for r in roles]}")
        # Check institucion
        if roles:
            inst = await conn.fetchval("SELECT id_institucion FROM sia.usuario WHERE id=$1", u["id"])
            print(f"id_institucion: {inst}")
    else:
        print("USUARIO NO ENCONTRADO")
    await conn.close()
asyncio.run(run())
