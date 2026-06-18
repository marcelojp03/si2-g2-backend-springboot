"""
Seed multi-institucion via SQL directo.
Usa NOW() de PostgreSQL para timestamps, evitando problemas de parametros.
"""
import asyncio, asyncpg, uuid, random
from datetime import date, timedelta

DSN = "postgresql://postgres:postgres@dbvpay.cfiek6gqkqd5.us-east-1.rds.amazonaws.com:5432/vpayDB"
PASS_HASH = "$2a$10$" + "x" * 53

NOMBRES_F = ["Sofia","Camila","Valentina","Luciana","Mariana","Gabriela","Daniela","Fernanda","Antonella","Carla","Elena","Paola","Natalia","Andrea","Victoria","Micaela","Camila","Ana","Lucia","Esperanza"]
NOMBRES_M = ["Mateo","Santiago","Diego","Sebastian","Adrian","Nicolas","Lucas","Emiliano","Samuel","Joaquin","Bruno","Rodrigo","Andres","Tomas","Mauricio","Javier","Gustavo","Fernando","Carlos","Miguel"]
APELLIDOS = ["Vargas","Mamani","Rojas","Quispe","Flores","Gutierrez","Rivera","Lopez","Choque","Mendoza","Aguilar","Paz","Suarez","Arce","Cabrera","Medina","Salazar","Ortiz","Romero","Camacho","Torrico","Burgos","Peinado","Ramos","Sanchez","Martinez","Gonzalez","Hernandez","Ramirez","Torres"]
CURSOS = [("PRI-1","1ro Primaria","Primaria",6),("PRI-2","2do Primaria","Primaria",7),("PRI-3","3ro Primaria","Primaria",8),("PRI-4","4to Primaria","Primaria",9),("PRI-5","5to Primaria","Primaria",10),("PRI-6","6to Primaria","Primaria",11),("SEC-1","1ro Secundaria","Secundaria",12),("SEC-2","2do Secundaria","Secundaria",13),("SEC-3","3ro Secundaria","Secundaria",14),("SEC-4","4to Secundaria","Secundaria",15),("SEC-5","5to Secundaria","Secundaria",16),("SEC-6","6to Secundaria","Secundaria",17)]
MATERIAS_DEF = [("MAT","Matematica","Ciencias Exactas",6),("LEN","Lenguaje y Comunicacion","Lenguajes",5),("CN","Ciencias Naturales","Ciencias",4),("CS","Ciencias Sociales","Sociedad",4),("VER","Valores y Espiritualidad","Formacion",2),("APV","Artes Plasticas y Visuales","Arte",2),("EFD","Educacion Fisica y Deportes","Salud",3),("MUS","Educacion Musical","Arte",2),("ING","Lengua Extranjera - Ingles","Lenguajes",3),("TT","Tecnica y Tecnologia","Tecnologia",3),("BIO","Biologia","Ciencias",4),("FIS","Fisica","Ciencias",4),("QUI","Quimica","Ciencias",4),("FIL","Filosofia y Psicologia","Humanidades",3)]
MAT_PRIM = ["MAT","LEN","CN","CS","VER","APV","EFD","MUS"]
MAT_SEC = ["MAT","LEN","CS","VER","APV","EFD","ING","TT","BIO","FIS","QUI","FIL"]
DOCENTES_NOM = ["Ana Rojas","Carlos Mendez","Patricia Vargas","Luis Arce","Ruth Aguilar","Mario Suarez","Claudia Medina","Fernando Paz","Marcela Lopez","Hugo Salazar","Elena Camacho","Jorge Rivera","Marisol Gutierrez","Oscar Romero","Diana Centeno","Pablo Iriarte","Gloria Condori","Raul Peinado","Sandra Veizaga","Alberto Villarroel"]
ESPECIALIDADES = ["Matematica","Lenguaje","Ciencias Naturales","Ciencias Sociales","Educacion Fisica","Musica","Artes","Ingles","Fisica","Quimica","Biologia","Filosofia","Informatica"]
PARENTESCOS = ["Padre","Madre","Padre","Madre","Tio(a)","Abuelo(a)","Hermano(a)","Otro"]


def sql(q):
    return q.replace("{}", "NOW()")


async def run():
    conn = await asyncpg.connect(DSN)
    insts = await conn.fetch("SELECT id, codigo, nombre FROM sia.institucion")
    roles = {}
    for r in ["ADMIN_INSTITUCION","DIRECTOR","SECRETARIO","DOCENTE","ESTUDIANTE","TUTOR"]:
        roles[r] = await conn.fetchval("SELECT id FROM sia.rol WHERE codigo=$1", r)

    for inst in insts:
        if inst["codigo"] == "CSM-001":
            continue
        iid = inst["id"]
        cc = await conn.fetchval("SELECT count(*) FROM sia.curso WHERE id_institucion=$1", iid)
        if cc > 0:
            print(f"{inst['codigo']} ya tiene datos, saltando")
            continue

        cod, nom = inst["codigo"], inst["nombre"]
        dom = cod.lower().replace("-","").replace(" ","") + ".edu.bo"
        gid = uuid.uuid4()
        print(f"\n=== {cod} - {nom} ===")

        await conn.execute(sql("INSERT INTO sia.gestion_academica(id,id_institucion,nombre,fecha_inicio,fecha_fin,activa,creado_en,actualizado_en) VALUES($1,$2,'Gestion Academica 2026','2026-02-03','2026-11-28',true,{},{} ) ON CONFLICT DO NOTHING"), gid, iid)

        # Cursos (12)
        cids = {}
        for ccode, cname, clevel, corder in CURSOS:
            cid = uuid.uuid4()
            await conn.execute(sql("INSERT INTO sia.curso(id,id_institucion,codigo,nombre,nivel,orden_visual,creado_en,actualizado_en) VALUES($1,$2,$3,$4,$5,$6,{},{} ) ON CONFLICT(id_institucion,codigo) DO NOTHING"), cid, iid, ccode, cname, clevel, corder)
            cids[ccode] = await conn.fetchval("SELECT id FROM sia.curso WHERE id_institucion=$1 AND codigo=$2", iid, ccode)
        print(f"  Cursos: {len(cids)}")

        # Materias (14)
        mids = {}
        for mcode, mname, marea, mload in MATERIAS_DEF:
            mid = uuid.uuid4()
            await conn.execute(sql("INSERT INTO sia.materia(id,id_institucion,codigo,nombre,area,carga_horaria,creado_en,actualizado_en) VALUES($1,$2,$3,$4,$5,$6,{},{} ) ON CONFLICT(id_institucion,codigo) DO NOTHING"), mid, iid, mcode, mname, marea, mload)
            mids[mcode] = await conn.fetchval("SELECT id FROM sia.materia WHERE id_institucion=$1 AND codigo=$2", iid, mcode)
        print(f"  Materias: {len(mids)}")

        # Aulas (24)
        for i in range(1, 25):
            bloque = "Bloque A" if i <= 12 else "Bloque B"
            piso = str((i-1)//6 + 1)
            await conn.execute(sql("INSERT INTO sia.aula(id,id_institucion,codigo,nombre,capacidad,ubicacion,recursos,creado_en,actualizado_en) VALUES($1,$2,$3,$4,35,$5,'Pizarra',{},{} ) ON CONFLICT(id_institucion,codigo) DO NOTHING"), uuid.uuid4(), iid, f"{cod}-AULA-{i:03d}", f"Aula {i:02d}", f"{bloque}, Piso {piso}")

        # Paralelos + curso_materia
        for ccode, cid in cids.items():
            nivel = "Secundaria" if ccode.startswith("SEC") else "Primaria"
            mlist = MAT_SEC if nivel == "Secundaria" else MAT_PRIM
            for pname in ["A", "B"]:
                pid = uuid.uuid4()
                await conn.execute(sql("INSERT INTO sia.paralelo(id,id_institucion,id_curso,id_gestion_academica,nombre,capacidad,creado_en,actualizado_en) VALUES($1,$2,$3,$4,$5,35,{},{} ) ON CONFLICT DO NOTHING"), pid, iid, cid, gid, pname)
                apid = await conn.fetchval("SELECT id FROM sia.paralelo WHERE id_institucion=$1 AND id_curso=$2 AND id_gestion_academica=$3 AND nombre=$4", iid, cid, gid, pname)
                for mcode in mlist:
                    await conn.execute(sql("INSERT INTO sia.curso_materia(id,id_institucion,id_curso,id_materia,id_gestion_academica,carga_horaria,estado,creado_en,actualizado_en) VALUES($1,$2,$3,$4,$5,4,'ACTIVO',{},{} ) ON CONFLICT DO NOTHING"), uuid.uuid4(), iid, cid, mids[mcode], gid)

        # Admin users (3)
        for cargo, elocal in [("ADMIN_INSTITUCION","admin"),("DIRECTOR","director"),("SECRETARIO","secretaria")]:
            corr = f"{elocal}@{dom}"
            if not await conn.fetchval("SELECT id FROM sia.usuario WHERE correo=$1", corr):
                uid = uuid.uuid4()
                await conn.execute(sql("INSERT INTO sia.usuario(id,id_institucion,correo,hash_contrasena,nombres,apellidos,creado_en,actualizado_en) VALUES($1,$2,$3,$4,$5,$6,{},{} )"), uid, iid, corr, PASS_HASH, "Admin", cargo.title())
                await conn.execute("INSERT INTO sia.usuario_rol(id_usuario,id_rol) VALUES($1,$2)", uid, roles[cargo])

        # Docentes (20)
        dids = []
        for di, dname in enumerate(DOCENTES_NOM):
            p = dname.split(" ", 1)
            corr = f"docente.{di+1}@{dom}"
            uid = await conn.fetchval("SELECT id FROM sia.usuario WHERE correo=$1", corr)
            if not uid:
                uid = uuid.uuid4()
                await conn.execute(sql("INSERT INTO sia.usuario(id,id_institucion,correo,hash_contrasena,nombres,apellidos,creado_en,actualizado_en) VALUES($1,$2,$3,$4,$5,$6,{},{} )"), uid, iid, corr, PASS_HASH, p[0], p[1])
                await conn.execute("INSERT INTO sia.usuario_rol(id_usuario,id_rol) VALUES($1,$2)", uid, roles["DOCENTE"])
            dcode = f"{cod}-DOC-{di+1:03d}"
            did = await conn.fetchval("SELECT id FROM sia.docente WHERE id_institucion=$1 AND codigo=$2", iid, dcode)
            if not did:
                did = uuid.uuid4()
                await conn.execute(sql("INSERT INTO sia.docente(id,id_institucion,id_usuario,codigo,documento_identidad,nombres,apellidos,telefono,correo,especialidad,creado_en,actualizado_en) VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,{},{} )"), did, iid, uid, dcode, f"{7000000+di*1111}", p[0], p[1], f"7{6000000+di*1111}", corr, ESPECIALIDADES[di%len(ESPECIALIDADES)])
            dids.append(did)

        # Estudiantes (480)
        nxt = 1
        for ccode, cid in cids.items():
            nivel = "Secundaria" if ccode.startswith("SEC") else "Primaria"
            age_base = 12 if nivel == "Secundaria" else 6
            base_age = age_base + int(ccode.split("-")[1])
            for pname in ["A", "B"]:
                apid = await conn.fetchval("SELECT id FROM sia.paralelo WHERE id_institucion=$1 AND id_curso=$2 AND id_gestion_academica=$3 AND nombre=$4", iid, cid, gid, pname)
                for si in range(20):
                    female = si % 2 == 0
                    nom = NOMBRES_F[nxt % len(NOMBRES_F)] if female else NOMBRES_M[nxt % len(NOMBRES_M)]
                    a1 = APELLIDOS[(nxt * 3) % len(APELLIDOS)]
                    a2 = APELLIDOS[(nxt * 7 + 5) % len(APELLIDOS)]
                    cod_est = f"{cod}-EST-{nxt:04d}"
                    corr_est = f"estudiante.{nxt:04d}@{dom}"
                    fnac = date(2026 - base_age, ((nxt * 3) % 12) + 1, ((nxt * 5) % 28) + 1)
                    sex = "FEMENINO" if female else "MASCULINO"

                    uid = await conn.fetchval("SELECT id FROM sia.usuario WHERE correo=$1", corr_est)
                    if not uid:
                        uid = uuid.uuid4()
                        await conn.execute(sql("INSERT INTO sia.usuario(id,id_institucion,correo,hash_contrasena,nombres,apellidos,creado_en,actualizado_en) VALUES($1,$2,$3,$4,$5,$6,{},{} )"), uid, iid, corr_est, PASS_HASH, nom, f"{a1} {a2}")
                        await conn.execute("INSERT INTO sia.usuario_rol(id_usuario,id_rol) VALUES($1,$2)", uid, roles["ESTUDIANTE"])

                    eid = await conn.fetchval("SELECT id FROM sia.estudiante WHERE id_institucion=$1 AND codigo_estudiante=$2", iid, cod_est)
                    if not eid:
                        eid = uuid.uuid4()
                        await conn.execute(sql("INSERT INTO sia.estudiante(id,id_institucion,id_usuario,codigo_estudiante,documento_identidad,nombres,apellidos,fecha_nacimiento,sexo,direccion,telefono,correo,creado_en,actualizado_en) VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,{},{} )"), eid, iid, uid, cod_est, str(8000000 + nxt * 111), nom, f"{a1} {a2}", fnac, sex, f"Zona {APELLIDOS[(nxt*2)%len(APELLIDOS)]}", f"7{7000000+nxt:07d}", corr_est)
                        # Tutor
                        nt = "Maria" if female else "Jose"
                        ta1 = APELLIDOS[(nxt + 10) % len(APELLIDOS)]
                        ta2 = APELLIDOS[(nxt + 15) % len(APELLIDOS)]
                        dtut = str(9000000 + nxt * 111)
                        tid = await conn.fetchval("SELECT id FROM sia.tutor WHERE id_institucion=$1 AND documento_identidad=$2", iid, dtut)
                        if not tid:
                            tid = uuid.uuid4()
                            await conn.execute(sql("INSERT INTO sia.tutor(id,id_institucion,documento_identidad,nombres,apellidos,telefono,direccion,creado_en,actualizado_en) VALUES($1,$2,$3,$4,$5,$6,$7,{},{} )"), tid, iid, dtut, nt, f"{ta1} {ta2}", f"7{8000000+nxt:07d}", f"Zona {APELLIDOS[(nxt+7)%len(APELLIDOS)]}")
                        await conn.execute(sql("INSERT INTO sia.estudiante_tutor(id,id_institucion,id_estudiante,id_tutor,parentesco,es_principal,creado_en,actualizado_en) VALUES($1,$2,$3,$4,$5,true,{},{} ) ON CONFLICT DO NOTHING"), uuid.uuid4(), iid, eid, tid, PARENTESCOS[nxt % len(PARENTESCOS)])
                        await conn.execute(sql("INSERT INTO sia.inscripcion(id,id_institucion,id_estudiante,id_gestion_academica,id_paralelo,fecha_inscripcion,creado_en,actualizado_en) VALUES($1,$2,$3,$4,$5,'2026-02-01',{},{} ) ON CONFLICT DO NOTHING"), uuid.uuid4(), iid, eid, gid, apid)
                    nxt += 1

        # Asignaciones docente
        for ccode, cid in cids.items():
            nivel = "Secundaria" if ccode.startswith("SEC") else "Primaria"
            mlist = MAT_SEC if nivel == "Secundaria" else MAT_PRIM
            for pname in ["A", "B"]:
                apid = await conn.fetchval("SELECT id FROM sia.paralelo WHERE id_institucion=$1 AND id_curso=$2 AND id_gestion_academica=$3 AND nombre=$4", iid, cid, gid, pname)
                base = abs(hash(pname + str(cid))) % len(dids)
                for mi, mcode in enumerate(mlist):
                    await conn.execute(sql("INSERT INTO sia.asignacion_docente(id,id_institucion,id_docente,id_materia,id_paralelo,id_gestion_academica,creado_en,actualizado_en) VALUES($1,$2,$3,$4,$5,$6,{},{} ) ON CONFLICT DO NOTHING"), uuid.uuid4(), iid, dids[(base+mi)%len(dids)], mids[mcode], apid, gid)

        print(f"  Data creada (cursos={len(cids)}, estudiantes={nxt-1}, docentes={len(dids)})")

    # === EXTENSION PARA TODAS ===
    for inst in insts:
        iid = inst["id"]
        print(f"\n=== Extension {inst['codigo']} ===")
        g = await conn.fetchrow("SELECT id, fecha_inicio, fecha_fin FROM sia.gestion_academica WHERE id_institucion=$1 AND activa=true", iid)
        if not g: continue
        gid, ginicio, gfin = g["id"], g["fecha_inicio"], g["fecha_fin"]

        # Periodos
        dias = (gfin - ginicio).days
        tercio = dias // 3
        pids = []
        for pn in range(1, 4):
            pid = uuid.uuid4()
            pi = ginicio + timedelta(days=(pn-1)*tercio)
            pf = ginicio + timedelta(days=pn*tercio-1) if pn < 3 else gfin
            await conn.execute(sql("INSERT INTO sia.periodo_evaluacion(id,id_institucion,id_gestion_academica,numero_periodo,tipo_periodo,fecha_inicio,fecha_fin,estado,peso_ser,peso_saber,peso_hacer,peso_auto,creado_en,actualizado_en) VALUES($1,$2,$3,$4,'TRIMESTRAL',$5,$6,'ABIERTO',10,45,40,5,{},{} ) ON CONFLICT DO NOTHING"), pid, iid, gid, pn, pi, pf)
            pids.append(pid)

        mids = await conn.fetch("SELECT id, codigo FROM sia.materia WHERE id_institucion=$1", iid)
        eids = await conn.fetch("SELECT id FROM sia.estudiante WHERE id_institucion=$1", iid)
        dids = await conn.fetch("SELECT id FROM sia.docente WHERE id_institucion=$1", iid)

        # Evaluaciones
        for m in mids:
            for pn in range(1, 4):
                for ti, tp in enumerate(["PARCIAL", "TRABAJO_PRACTICO"]):
                    pond = 40 if ti == 0 else 60
                    await conn.execute(sql("INSERT INTO sia.evaluacion_materia(id,id_institucion,id_materia,periodo,tipo,nombre,ponderacion,escala,estado,creado_en,actualizado_en) VALUES($1,$2,$3,$4,$5,$6,$7,'NUMERICA','ABIERTA',{},{} ) ON CONFLICT DO NOTHING"), uuid.uuid4(), iid, m["id"], pn, tp, f"{tp} {pn}er Trimestre", pond)

        # Actividades
        for m in mids:
            doc = dids[abs(hash(m["codigo"])) % len(dids)]
            for pid, pn in zip(pids, range(1, 4)):
                pi = ginicio + timedelta(days=(pn-1)*tercio)
                for dim, pmax in [("SABER",40),("HACER",45)]:
                    nombre = f"{dim} - {m['codigo']} T{pn}"
                    fecha = pi + timedelta(days=5 if dim=="SABER" else 20)
                    try:
                        await conn.execute(sql("INSERT INTO sia.actividad_evaluativa(id,id_institucion,id_periodo_evaluacion,id_materia,id_docente,nombre_actividad,dimension,fecha_actividad,puntaje_maximo,estado,creado_en,actualizado_en) VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9,'PUBLICADA',{},{}) ON CONFLICT DO NOTHING"), uuid.uuid4(), iid, pid, m["id"], doc["id"], nombre, dim, fecha, pmax)
                    except: pass

        # Calificaciones bulk
        await conn.execute(f"""
            INSERT INTO sia.calificacion_actividad(id,id_institucion,id_actividad,id_estudiante,nota_obtenida,estado,creado_en,actualizado_en)
            SELECT gen_random_uuid(), '{iid}'::uuid, a.id, e.id,
                   round((a.puntaje_maximo * 0.3 + random() * (a.puntaje_maximo * 0.7))::numeric, 2),
                   'REGISTRADA', NOW(), NOW()
            FROM sia.actividad_evaluativa a, sia.estudiante e
            WHERE a.id_institucion = '{iid}'::uuid AND e.id_institucion = '{iid}'::uuid
            ON CONFLICT DO NOTHING
        """)

        # SER + Auto bulk
        for m in mids:
            for pid in pids:
                await conn.execute(f"""
                    INSERT INTO sia.calificacion_ser(id,id_institucion,id_periodo_evaluacion,id_estudiante,id_materia,id_docente,nota_ser,estado,creado_en,actualizado_en)
                    SELECT gen_random_uuid(), '{iid}'::uuid, '{pid}'::uuid, e.id, '{m["id"]}'::uuid,
                           (SELECT ad.id_docente FROM sia.asignacion_docente ad WHERE ad.id_institucion='{iid}'::uuid AND ad.id_materia='{m["id"]}'::uuid LIMIT 1),
                           round((5+random()*5)::numeric,2), 'REGISTRADA', NOW(), NOW()
                    FROM sia.estudiante e WHERE e.id_institucion = '{iid}'::uuid
                    ON CONFLICT DO NOTHING
                """)
                await conn.execute(f"""
                    INSERT INTO sia.autoevaluacion_trimestral(id,id_institucion,id_periodo_evaluacion,id_estudiante,id_materia,nota_autoevaluacion,estado,creado_en,actualizado_en)
                    SELECT gen_random_uuid(), '{iid}'::uuid, '{pid}'::uuid, e.id, '{m["id"]}'::uuid,
                           round((2+random()*3)::numeric,2), 'REGISTRADA', NOW(), NOW()
                    FROM sia.estudiante e WHERE e.id_institucion = '{iid}'::uuid
                    ON CONFLICT DO NOTHING
                """)
        print(f"  Evaluaciones/calificaciones OK")

        # Horarios
        aulas = await conn.fetch("SELECT id FROM sia.aula WHERE id_institucion=$1 ORDER BY nombre", iid)
        asigs = await conn.fetch("SELECT id, id_materia FROM sia.asignacion_docente WHERE id_institucion=$1", iid)
        slots = [("08:00","09:15"),("09:30","10:45"),("11:00","12:15"),("14:00","15:15"),("15:30","16:45")]
        hcnt = 0
        for ai, asig in enumerate(asigs):
            for h in range(2):
                aula = aulas[(ai + h*5 + ai//5) % len(aulas)]
                si = (ai + h*3) % len(slots)
                dia = ["LUNES","MARTES","MIERCOLES","JUEVES","VIERNES"][(ai//5 + h*2) % 5]
                hi, hf = slots[si]
                try:
                    await conn.execute(sql("INSERT INTO sia.horario_clase(id,id_institucion,id_asignacion_docente,id_aula,dia_semana,hora_inicio,hora_fin,estado,creado_en,actualizado_en) VALUES($1,$2,$3,$4,$5,$6::time,$7::time,'ACTIVO',{},{}) ON CONFLICT DO NOTHING"), uuid.uuid4(), iid, asig["id"], aula["id"], dia, hi, hf)
                    hcnt += 1
                except: pass
        print(f"  Horarios: {hcnt}")

        # Asistencias
        insc = await conn.fetch("SELECT id, id_paralelo FROM sia.inscripcion WHERE id_institucion=$1", iid)
        ins_pp = {}
        for ins in insc:
            pp = ins["id_paralelo"]
            if pp not in ins_pp: ins_pp[pp] = []
            ins_pp[pp].append(ins["id"])
        inicio = ginicio + timedelta(weeks=2)
        rcnt = dcnt = 0
        for asig in asigs:
            ins_ids = ins_pp.get(asig["id_paralelo"], [])
            if not ins_ids: continue
            for s in range(8):
                fecha = inicio + timedelta(weeks=s)
                try:
                    await conn.execute(sql("INSERT INTO sia.asistencia_registro(id,id_institucion,id_asignacion_docente,fecha,estado,creado_en,actualizado_en) VALUES($1,$2,$3,$4,'REGISTRADA',{},{}) ON CONFLICT DO NOTHING"), uuid.uuid4(), iid, asig["id"], fecha)
                    rcnt += 1
                    for ins_id in ins_ids:
                        roll = random.random()
                        acc = 0
                        ea = "PRESENTE"
                        for i, (est, peso) in enumerate(zip(["PRESENTE","AUSENTE","TARDANZA","JUSTIFICADO"],[0.80,0.08,0.07,0.05])):
                            acc += peso
                            if roll <= acc: ea = est; break
                        try:
                            await conn.execute(sql("INSERT INTO sia.asistencia_detalle(id,id_asistencia_registro,id_inscripcion,estado_asistencia,creado_en,actualizado_en) VALUES($1,$2,$3,$4,{},{}) ON CONFLICT DO NOTHING"), uuid.uuid4(), iid, ins_id, ea)
                            dcnt += 1
                        except: pass
                except: pass
        print(f"  Asistencias: {rcnt} registros, {dcnt} detalles")

    await conn.close()
    print("\n=== SEED MULTI-INSTITUCION COMPLETADO ===")

asyncio.run(run())
