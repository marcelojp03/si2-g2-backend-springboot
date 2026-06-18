import asyncio, asyncpg
DSN = "postgresql://postgres:postgres@dbvpay.cfiek6gqkqd5.us-east-1.rds.amazonaws.com:5432/vpayDB"
async def run():
    conn = await asyncpg.connect(DSN)
    insts = await conn.fetch("SELECT id, codigo FROM sia.institucion WHERE codigo != 'CSM-001'")
    tables = ["calificacion_actividad","actividad_evaluativa","evaluacion_materia","periodo_evaluacion",
              "calificacion_ser","autoevaluacion_trimestral","horario_clase",
              "asistencia_registro","asistencia_detalle","inscripcion","estudiante_tutor",
              "asignacion_docente","estudiante","tutor","docente","curso_materia","paralelo",
              "curso","aula","gestion_academica","configuracion_institucion"]
    for inst in insts:
        iid = inst["id"]
        cod = inst["codigo"]
        # Clean users for this institution
        await conn.execute("""
            DELETE FROM sia.usuario_rol WHERE id_usuario IN (
                SELECT u.id FROM sia.usuario u WHERE u.id_institucion = $1
                AND u.id NOT IN (
                    SELECT ur.id_usuario FROM sia.usuario_rol ur
                    JOIN sia.rol r ON r.id = ur.id_rol
                    WHERE r.codigo IN ('SUPER_ADMIN','ADMIN_INSTITUCION','DIRECTOR','SECRETARIO')
                )
            )
        """, iid)
        await conn.execute("DELETE FROM sia.usuario WHERE id_institucion = $1 AND id NOT IN (SELECT ur.id_usuario FROM sia.usuario_rol ur)", iid)
        # Clean business tables
        for t in tables:
            try:
                await conn.execute(f"DELETE FROM sia.{t} WHERE id_institucion = $1", iid)
            except:
                pass
        print(f"Limpiado {cod}")
    await conn.close()
    print("OK")
asyncio.run(run())
