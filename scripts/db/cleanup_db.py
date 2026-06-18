import asyncio, asyncpg

DSN = "postgresql://postgres:postgres@dbvpay.cfiek6gqkqd5.us-east-1.rds.amazonaws.com:5432/vpayDB"

async def run():
    conn = await asyncpg.connect(DSN)
    tables = [
        "archivo_referencia", "archivo", "bitacora_auditoria",
        "calificacion_cambio", "observacion_ser", "autoevaluacion_trimestral",
        "calificacion_ser", "calificacion_actividad", "actividad_evaluativa",
        "evaluacion_materia", "evaluacion", "calificacion",
        "periodo_evaluacion", "asistencia_detalle", "asistencia_registro",
        "horario_clase", "inscripcion", "estudiante_tutor", "asignacion_docente",
        "estudiante", "tutor", "docente", "curso_materia", "paralelo",
        "curso", "aula", "materia", "gestion_academica", "configuracion_institucion",
    ]
    for t in tables:
        try:
            r = await conn.execute(f"DELETE FROM sia.{t}")
            print(f"  sia.{t:30s} -> {r}")
        except Exception as e:
            print(f"  sia.{t:30s} -> ERROR: {e}")

    await conn.execute("""
        DELETE FROM sia.usuario_rol WHERE id_usuario IN (
            SELECT u.id FROM sia.usuario u WHERE u.id NOT IN (
                SELECT ur.id_usuario FROM sia.usuario_rol ur
                JOIN sia.rol r ON r.id = ur.id_rol
                WHERE r.codigo IN ('SUPER_ADMIN','ADMIN_INSTITUCION','DIRECTOR','SECRETARIO')
            )
        )
    """)
    print("  usuario_rol no-admin -> DELETED")

    await conn.execute("""
        DELETE FROM sia.usuario u WHERE u.id NOT IN (
            SELECT ur.id_usuario FROM sia.usuario_rol ur
            JOIN sia.rol r ON r.id = ur.id_rol
            WHERE r.codigo IN ('SUPER_ADMIN','ADMIN_INSTITUCION','DIRECTOR','SECRETARIO')
        )
    """)
    print("  usuarios no-admin -> DELETED")

    print()
    print("VERIFICACION POST-LIMPIEZA:")
    for t in ["curso","materia","estudiante","docente","evaluacion_materia",
              "periodo_evaluacion","horario_clase","asistencia_registro","inscripcion","paralelo"]:
        try:
            r = await conn.fetchval(f"SELECT count(*) FROM sia.{t}")
            print(f"  sia.{t:25s} -> {r}")
        except Exception as e:
            print(f"  sia.{t:25s} -> ERROR: {e}")

    r = await conn.fetchval("SELECT count(*) FROM sia.usuario")
    print(f"  sia.usuario (post-cleanup) -> {r}")

    await conn.close()

asyncio.run(run())
