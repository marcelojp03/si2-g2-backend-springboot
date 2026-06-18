import asyncio, asyncpg

DSN = "postgresql://postgres:postgres@dbvpay.cfiek6gqkqd5.us-east-1.rds.amazonaws.com:5432/vpayDB"

async def run():
    conn = await asyncpg.connect(DSN)
    # Check CSM-001
    r = await conn.fetchrow("SELECT id, nombre FROM sia.institucion WHERE codigo = 'CSM-001'")
    if r:
        print(f"CSM-001 found: id={str(r['id'])[:8]} name={r['name'] if 'name' in r else r['nombre']}")
    else:
        print("CSM-001 NOT FOUND")
    # Check gestion_academica
    for t in ["curso","materia","paralelo","estudiante","evaluacion_materia","periodo_evaluacion","inscripcion","asignacion_docente","horario_clase","usuario"]:
        cnt = await conn.fetchval(f"SELECT count(*) FROM sia.{t}")
        print(f"  sia.{t:25s} -> {cnt}")
    await conn.close()

asyncio.run(run())
