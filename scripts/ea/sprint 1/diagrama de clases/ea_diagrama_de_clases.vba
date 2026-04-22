Option Explicit

Const FULL_REBUILD = True
Const MODELO_NOMBRE = "Modelo BD - Sistema Gestion Academica SaaS"

Sub Main()

    On Error Resume Next

    Dim selectedPkg
    Dim targetPkg
    Dim dgm

    Set selectedPkg = Repository.GetTreeSelectedPackage()

    If selectedPkg Is Nothing Then
        Session.Output "Selecciona un package en el Browser."
        Exit Sub
    End If

    Set targetPkg = CreateOrGetPackage(selectedPkg, MODELO_NOMBRE)

    If FULL_REBUILD Then
        ResetPackageContents targetPkg
    End If

    ' =========================
    ' CREAR CLASES / TABLAS
    ' =========================
    Dim institucion, configuracion_institucion
    Dim rol, usuario, usuario_rol
    Dim gestion_academica, curso, paralelo, materia, curso_materia
    Dim docente, estudiante, tutor, estudiante_tutor
    Dim inscripcion, asignacion_docente
    Dim bitacora_auditoria

    ' ---- INSTITUCION ----
    Set institucion = CreateClass(targetPkg, "institucion")
    AddAttr institucion, "id [PK]", "uuid"
    AddAttr institucion, "codigo", "varchar(30)"
    AddAttr institucion, "nombre", "varchar(200)"
    AddAttr institucion, "tipo_institucion", "varchar(20)"
    AddAttr institucion, "telefono", "varchar(30)"
    AddAttr institucion, "correo", "varchar(150)"
    AddAttr institucion, "direccion", "varchar(255)"
    AddAttr institucion, "logo_url", "text"
    AddAttr institucion, "estado", "varchar(15)"
    AddAttr institucion, "creado_en", "timestamp"
    AddAttr institucion, "actualizado_en", "timestamp"

    ' ---- CONFIGURACION_INSTITUCION ----
    Set configuracion_institucion = CreateClass(targetPkg, "configuracion_institucion")
    AddAttr configuracion_institucion, "id [PK]", "uuid"
    AddAttr configuracion_institucion, "id_institucion [FK]", "uuid"
    AddAttr configuracion_institucion, "clave", "varchar(100)"
    AddAttr configuracion_institucion, "valor", "text"
    AddAttr configuracion_institucion, "tipo_valor", "varchar(30)"
    AddAttr configuracion_institucion, "descripcion", "varchar(255)"
    AddAttr configuracion_institucion, "creado_en", "timestamp"
    AddAttr configuracion_institucion, "actualizado_en", "timestamp"

    ' ---- ROL ----
    Set rol = CreateClass(targetPkg, "rol")
    AddAttr rol, "id [PK]", "uuid"
    AddAttr rol, "codigo", "varchar(50)"
    AddAttr rol, "nombre", "varchar(100)"
    AddAttr rol, "descripcion", "varchar(255)"
    AddAttr rol, "es_global", "boolean"
    AddAttr rol, "estado", "varchar(15)"
    AddAttr rol, "creado_en", "timestamp"
    AddAttr rol, "actualizado_en", "timestamp"

    ' ---- USUARIO ----
    Set usuario = CreateClass(targetPkg, "usuario")
    AddAttr usuario, "id [PK]", "uuid"
    AddAttr usuario, "id_institucion [FK]", "uuid"
    AddAttr usuario, "correo", "varchar(150)"
    AddAttr usuario, "hash_contrasena", "text"
    AddAttr usuario, "nombres", "varchar(120)"
    AddAttr usuario, "apellidos", "varchar(120)"
    AddAttr usuario, "telefono", "varchar(30)"
    AddAttr usuario, "requiere_cambio_contrasena", "boolean"
    AddAttr usuario, "estado", "varchar(15)"
    AddAttr usuario, "ultimo_acceso", "timestamp"
    AddAttr usuario, "creado_en", "timestamp"
    AddAttr usuario, "actualizado_en", "timestamp"

    ' ---- USUARIO_ROL ----
    Set usuario_rol = CreateClass(targetPkg, "usuario_rol")
    AddAttr usuario_rol, "id [PK]", "uuid"
    AddAttr usuario_rol, "id_usuario [FK]", "uuid"
    AddAttr usuario_rol, "id_rol [FK]", "uuid"
    AddAttr usuario_rol, "activo", "boolean"
    AddAttr usuario_rol, "creado_en", "timestamp"
    AddAttr usuario_rol, "actualizado_en", "timestamp"

    ' ---- GESTION_ACADEMICA ----
    Set gestion_academica = CreateClass(targetPkg, "gestion_academica")
    AddAttr gestion_academica, "id [PK]", "uuid"
    AddAttr gestion_academica, "id_institucion [FK]", "uuid"
    AddAttr gestion_academica, "nombre", "varchar(100)"
    AddAttr gestion_academica, "fecha_inicio", "date"
    AddAttr gestion_academica, "fecha_fin", "date"
    AddAttr gestion_academica, "activa", "boolean"
    AddAttr gestion_academica, "estado", "varchar(15)"
    AddAttr gestion_academica, "creado_en", "timestamp"
    AddAttr gestion_academica, "actualizado_en", "timestamp"

    ' ---- CURSO ----
    Set curso = CreateClass(targetPkg, "curso")
    AddAttr curso, "id [PK]", "uuid"
    AddAttr curso, "id_institucion [FK]", "uuid"
    AddAttr curso, "codigo", "varchar(30)"
    AddAttr curso, "nombre", "varchar(100)"
    AddAttr curso, "nivel", "varchar(50)"
    AddAttr curso, "orden_visual", "integer"
    AddAttr curso, "estado", "varchar(15)"
    AddAttr curso, "creado_en", "timestamp"
    AddAttr curso, "actualizado_en", "timestamp"

    ' ---- PARALELO ----
    Set paralelo = CreateClass(targetPkg, "paralelo")
    AddAttr paralelo, "id [PK]", "uuid"
    AddAttr paralelo, "id_institucion [FK]", "uuid"
    AddAttr paralelo, "id_curso [FK]", "uuid"
    AddAttr paralelo, "id_gestion_academica [FK]", "uuid"
    AddAttr paralelo, "nombre", "varchar(20)"
    AddAttr paralelo, "capacidad", "integer"
    AddAttr paralelo, "estado", "varchar(15)"
    AddAttr paralelo, "creado_en", "timestamp"
    AddAttr paralelo, "actualizado_en", "timestamp"

    ' ---- MATERIA ----
    Set materia = CreateClass(targetPkg, "materia")
    AddAttr materia, "id [PK]", "uuid"
    AddAttr materia, "id_institucion [FK]", "uuid"
    AddAttr materia, "codigo", "varchar(30)"
    AddAttr materia, "nombre", "varchar(120)"
    AddAttr materia, "area", "varchar(100)"
    AddAttr materia, "carga_horaria", "integer"
    AddAttr materia, "estado", "varchar(15)"
    AddAttr materia, "creado_en", "timestamp"
    AddAttr materia, "actualizado_en", "timestamp"

    ' ---- CURSO_MATERIA ----
    Set curso_materia = CreateClass(targetPkg, "curso_materia")
    AddAttr curso_materia, "id [PK]", "uuid"
    AddAttr curso_materia, "id_institucion [FK]", "uuid"
    AddAttr curso_materia, "id_curso [FK]", "uuid"
    AddAttr curso_materia, "id_materia [FK]", "uuid"
    AddAttr curso_materia, "id_gestion_academica [FK]", "uuid"
    AddAttr curso_materia, "carga_horaria", "integer"
    AddAttr curso_materia, "estado", "varchar(15)"
    AddAttr curso_materia, "creado_en", "timestamp"
    AddAttr curso_materia, "actualizado_en", "timestamp"

    ' ---- DOCENTE ----
    Set docente = CreateClass(targetPkg, "docente")
    AddAttr docente, "id [PK]", "uuid"
    AddAttr docente, "id_institucion [FK]", "uuid"
    AddAttr docente, "id_usuario [FK]", "uuid"
    AddAttr docente, "codigo", "varchar(30)"
    AddAttr docente, "documento_identidad", "varchar(30)"
    AddAttr docente, "nombres", "varchar(120)"
    AddAttr docente, "apellidos", "varchar(120)"
    AddAttr docente, "telefono", "varchar(30)"
    AddAttr docente, "correo", "varchar(150)"
    AddAttr docente, "especialidad", "varchar(120)"
    AddAttr docente, "estado", "varchar(15)"
    AddAttr docente, "creado_en", "timestamp"
    AddAttr docente, "actualizado_en", "timestamp"

    ' ---- ESTUDIANTE ----
    Set estudiante = CreateClass(targetPkg, "estudiante")
    AddAttr estudiante, "id [PK]", "uuid"
    AddAttr estudiante, "id_institucion [FK]", "uuid"
    AddAttr estudiante, "id_usuario [FK]", "uuid"
    AddAttr estudiante, "codigo_estudiante", "varchar(30)"
    AddAttr estudiante, "documento_identidad", "varchar(30)"
    AddAttr estudiante, "nombres", "varchar(120)"
    AddAttr estudiante, "apellidos", "varchar(120)"
    AddAttr estudiante, "fecha_nacimiento", "date"
    AddAttr estudiante, "sexo", "varchar(15)"
    AddAttr estudiante, "direccion", "varchar(255)"
    AddAttr estudiante, "telefono", "varchar(30)"
    AddAttr estudiante, "correo", "varchar(150)"
    AddAttr estudiante, "estado", "varchar(15)"
    AddAttr estudiante, "creado_en", "timestamp"
    AddAttr estudiante, "actualizado_en", "timestamp"

    ' ---- TUTOR ----
    Set tutor = CreateClass(targetPkg, "tutor")
    AddAttr tutor, "id [PK]", "uuid"
    AddAttr tutor, "id_institucion [FK]", "uuid"
    AddAttr tutor, "id_usuario [FK]", "uuid"
    AddAttr tutor, "documento_identidad", "varchar(30)"
    AddAttr tutor, "nombres", "varchar(120)"
    AddAttr tutor, "apellidos", "varchar(120)"
    AddAttr tutor, "telefono", "varchar(30)"
    AddAttr tutor, "correo", "varchar(150)"
    AddAttr tutor, "direccion", "varchar(255)"
    AddAttr tutor, "estado", "varchar(15)"
    AddAttr tutor, "creado_en", "timestamp"
    AddAttr tutor, "actualizado_en", "timestamp"

    ' ---- ESTUDIANTE_TUTOR ----
    Set estudiante_tutor = CreateClass(targetPkg, "estudiante_tutor")
    AddAttr estudiante_tutor, "id [PK]", "uuid"
    AddAttr estudiante_tutor, "id_institucion [FK]", "uuid"
    AddAttr estudiante_tutor, "id_estudiante [FK]", "uuid"
    AddAttr estudiante_tutor, "id_tutor [FK]", "uuid"
    AddAttr estudiante_tutor, "parentesco", "varchar(50)"
    AddAttr estudiante_tutor, "es_principal", "boolean"
    AddAttr estudiante_tutor, "estado", "varchar(15)"
    AddAttr estudiante_tutor, "creado_en", "timestamp"
    AddAttr estudiante_tutor, "actualizado_en", "timestamp"

    ' ---- INSCRIPCION ----
    Set inscripcion = CreateClass(targetPkg, "inscripcion")
    AddAttr inscripcion, "id [PK]", "uuid"
    AddAttr inscripcion, "id_institucion [FK]", "uuid"
    AddAttr inscripcion, "id_estudiante [FK]", "uuid"
    AddAttr inscripcion, "id_gestion_academica [FK]", "uuid"
    AddAttr inscripcion, "id_paralelo [FK]", "uuid"
    AddAttr inscripcion, "fecha_inscripcion", "date"
    AddAttr inscripcion, "estado", "varchar(15)"
    AddAttr inscripcion, "observacion", "varchar(255)"
    AddAttr inscripcion, "creado_en", "timestamp"
    AddAttr inscripcion, "actualizado_en", "timestamp"

    ' ---- ASIGNACION_DOCENTE ----
    Set asignacion_docente = CreateClass(targetPkg, "asignacion_docente")
    AddAttr asignacion_docente, "id [PK]", "uuid"
    AddAttr asignacion_docente, "id_institucion [FK]", "uuid"
    AddAttr asignacion_docente, "id_docente [FK]", "uuid"
    AddAttr asignacion_docente, "id_materia [FK]", "uuid"
    AddAttr asignacion_docente, "id_paralelo [FK]", "uuid"
    AddAttr asignacion_docente, "id_gestion_academica [FK]", "uuid"
    AddAttr asignacion_docente, "carga_horaria", "integer"
    AddAttr asignacion_docente, "estado", "varchar(15)"
    AddAttr asignacion_docente, "creado_en", "timestamp"
    AddAttr asignacion_docente, "actualizado_en", "timestamp"

    ' ---- BITACORA_AUDITORIA ----
    Set bitacora_auditoria = CreateClass(targetPkg, "bitacora_auditoria")
    AddAttr bitacora_auditoria, "id [PK]", "uuid"
    AddAttr bitacora_auditoria, "id_institucion [FK]", "uuid"
    AddAttr bitacora_auditoria, "id_usuario [FK]", "uuid"
    AddAttr bitacora_auditoria, "fecha_evento", "timestamp"
    AddAttr bitacora_auditoria, "direccion_ip", "varchar(50)"
    AddAttr bitacora_auditoria, "plataforma_cliente", "varchar(30)"
    AddAttr bitacora_auditoria, "agente_usuario", "text"
    AddAttr bitacora_auditoria, "nombre_modulo", "varchar(100)"
    AddAttr bitacora_auditoria, "nombre_entidad", "varchar(100)"
    AddAttr bitacora_auditoria, "id_entidad", "varchar(100)"
    AddAttr bitacora_auditoria, "tipo_operacion", "varchar(30)"
    AddAttr bitacora_auditoria, "datos_antes", "jsonb"
    AddAttr bitacora_auditoria, "datos_despues", "jsonb"
    AddAttr bitacora_auditoria, "exito", "boolean"
    AddAttr bitacora_auditoria, "mensaje", "varchar(255)"
    AddAttr bitacora_auditoria, "creado_en", "timestamp"

    targetPkg.Elements.Refresh

    ' =========================
    ' CREAR DIAGRAMA
    ' =========================
    Set dgm = CreateOrGetDiagram(targetPkg, "Diagrama de Clases Sprint 1", "Logical")
    ClearDiagramObjects dgm

    ' =========================
    ' POSICIONES
    ' FILA 1: institucion, configuracion_institucion, rol
    ' FILA 2: usuario, usuario_rol
    ' FILA 3: gestion_academica, curso, materia
    ' FILA 4: paralelo, curso_materia
    ' FILA 5: docente, estudiante, tutor
    ' FILA 6: estudiante_tutor, inscripcion, asignacion_docente
    ' FILA 7: bitacora_auditoria
    ' =========================
    AddElementToDiagram dgm, institucion.ElementID,               40,   40,  340,  380
    AddElementToDiagram dgm, configuracion_institucion.ElementID, 400,   40,  720,  320
    AddElementToDiagram dgm, rol.ElementID,                       780,   40, 1060,  300

    AddElementToDiagram dgm, usuario.ElementID,                    40,  440,  360,  800
    AddElementToDiagram dgm, usuario_rol.ElementID,               420,  440,  720,  680

    AddElementToDiagram dgm, gestion_academica.ElementID,          40,  860,  360, 1120
    AddElementToDiagram dgm, curso.ElementID,                     420,  860,  720, 1160
    AddElementToDiagram dgm, materia.ElementID,                   780,  860, 1060, 1140

    AddElementToDiagram dgm, paralelo.ElementID,                   40, 1220,  340, 1520
    AddElementToDiagram dgm, curso_materia.ElementID,             400, 1220,  720, 1520

    AddElementToDiagram dgm, docente.ElementID,                    40, 1600,  340, 1960
    AddElementToDiagram dgm, estudiante.ElementID,                400, 1600,  720, 2000
    AddElementToDiagram dgm, tutor.ElementID,                     780, 1600, 1060, 1940

    AddElementToDiagram dgm, estudiante_tutor.ElementID,           40, 2060,  360, 2360
    AddElementToDiagram dgm, inscripcion.ElementID,               420, 2060,  740, 2380
    AddElementToDiagram dgm, asignacion_docente.ElementID,        800, 2060, 1140, 2400

    AddElementToDiagram dgm, bitacora_auditoria.ElementID,         40, 2460,  440, 2900

    ' =========================
    ' RELACIONES
    ' =========================

    ' Institucion -> hijos directos
    AddRelation configuracion_institucion, institucion, "pertenece_a", "0..*", "1"
    AddRelation usuario, institucion, "pertenece_a", "0..*", "0..1"
    AddRelation gestion_academica, institucion, "pertenece_a", "0..*", "1"
    AddRelation curso, institucion, "pertenece_a", "0..*", "1"
    AddRelation materia, institucion, "pertenece_a", "0..*", "1"
    AddRelation docente, institucion, "pertenece_a", "0..*", "1"
    AddRelation estudiante, institucion, "pertenece_a", "0..*", "1"
    AddRelation tutor, institucion, "pertenece_a", "0..*", "1"

    ' Seguridad
    AddRelation usuario_rol, usuario, "asignado_a", "0..*", "1"
    AddRelation usuario_rol, rol, "tiene_rol", "0..*", "1"

    ' Estructura academica
    AddRelation paralelo, institucion, "pertenece_a", "0..*", "1"
    AddRelation paralelo, curso, "es_de", "0..*", "1"
    AddRelation paralelo, gestion_academica, "en_gestion", "0..*", "1"
    AddRelation curso_materia, institucion, "pertenece_a", "0..*", "1"
    AddRelation curso_materia, curso, "incluye_curso", "0..*", "1"
    AddRelation curso_materia, materia, "incluye_materia", "0..*", "1"
    AddRelation curso_materia, gestion_academica, "en_gestion", "0..*", "1"

    ' Personas -> usuario
    AddRelation docente, usuario, "tiene_cuenta", "0..1", "0..1"
    AddRelation estudiante, usuario, "tiene_cuenta", "0..1", "0..1"
    AddRelation tutor, usuario, "tiene_cuenta", "0..1", "0..1"

    ' Relacion estudiante / tutor
    AddRelation estudiante_tutor, institucion, "pertenece_a", "0..*", "1"
    AddRelation estudiante_tutor, estudiante, "tiene_tutor", "0..*", "1"
    AddRelation estudiante_tutor, tutor, "es_tutor_de", "0..*", "1"

    ' Operacion academica
    AddRelation inscripcion, institucion, "pertenece_a", "0..*", "1"
    AddRelation inscripcion, estudiante, "inscribe_a", "0..*", "1"
    AddRelation inscripcion, gestion_academica, "en_gestion", "0..*", "1"
    AddRelation inscripcion, paralelo, "en_paralelo", "0..*", "1"
    AddRelation asignacion_docente, institucion, "pertenece_a", "0..*", "1"
    AddRelation asignacion_docente, docente, "asignado", "0..*", "1"
    AddRelation asignacion_docente, materia, "dicta", "0..*", "1"
    AddRelation asignacion_docente, paralelo, "en_paralelo", "0..*", "1"
    AddRelation asignacion_docente, gestion_academica, "en_gestion", "0..*", "1"

    ' Bitacora
    AddRelation bitacora_auditoria, institucion, "registra_inst", "0..*", "0..1"
    AddRelation bitacora_auditoria, usuario, "registra_usr", "0..*", "0..1"

    dgm.Update
    Repository.ReloadDiagram dgm.DiagramID

    Session.Output "Diagrama de clases Sprint 1 creado correctamente."

End Sub

Function CreateOrGetPackage(parentPkg, pkgName)
    Dim i
    Dim p

    For i = 0 To parentPkg.Packages.Count - 1
        Set p = parentPkg.Packages.GetAt(i)
        If p.Name = pkgName Then
            Set CreateOrGetPackage = p
            Exit Function
        End If
    Next

    Set p = parentPkg.Packages.AddNew(pkgName, "")
    p.Update
    parentPkg.Packages.Refresh

    Set CreateOrGetPackage = p
End Function

Sub ResetPackageContents(pkg)
    Dim i
    Dim childPkg

    For i = pkg.Packages.Count - 1 To 0 Step -1
        Set childPkg = pkg.Packages.GetAt(i)
        ResetPackageContents childPkg
        pkg.Packages.DeleteAt i, False
    Next
    pkg.Packages.Refresh

    For i = pkg.Diagrams.Count - 1 To 0 Step -1
        pkg.Diagrams.DeleteAt i, False
    Next
    pkg.Diagrams.Refresh

    For i = pkg.Elements.Count - 1 To 0 Step -1
        pkg.Elements.DeleteAt i, False
    Next
    pkg.Elements.Refresh
End Sub

Function CreateOrGetDiagram(pkg, diagramName, diagramType)
    Dim i
    Dim dgm

    For i = 0 To pkg.Diagrams.Count - 1
        Set dgm = pkg.Diagrams.GetAt(i)
        If dgm.Name = diagramName Then
            Set CreateOrGetDiagram = dgm
            Exit Function
        End If
    Next

    Set dgm = pkg.Diagrams.AddNew(diagramName, diagramType)
    dgm.Update
    pkg.Diagrams.Refresh

    Set CreateOrGetDiagram = dgm
End Function

Function CreateClass(pkg, className)
    Dim el
    Set el = pkg.Elements.AddNew(className, "Class")
    el.Update
    Set CreateClass = el
End Function

Sub AddAttr(el, attrName, attrType)
    Dim at
    Set at = el.Attributes.AddNew(attrName, attrType)
    at.Update
    el.Attributes.Refresh
End Sub

Sub AddRelation(childEl, parentEl, relName, childCard, parentCard)
    Dim conn
    Set conn = childEl.Connectors.AddNew(relName, "Association")
    conn.SupplierID = parentEl.ElementID
    conn.ClientEnd.Cardinality = childCard
    conn.SupplierEnd.Cardinality = parentCard
    conn.Update
    childEl.Connectors.Refresh
    parentEl.Connectors.Refresh
End Sub

Sub ClearDiagramObjects(dgm)
    Dim i
    For i = dgm.DiagramObjects.Count - 1 To 0 Step -1
        dgm.DiagramObjects.DeleteAt i, False
    Next
    dgm.DiagramObjects.Refresh
End Sub

Sub AddElementToDiagram(dgm, elementId, l, t, r, b)
    Dim dobj
    Set dobj = dgm.DiagramObjects.AddNew("l=" & l & ";r=" & r & ";t=" & (-t) & ";b=" & (-b), "")
    dobj.ElementID = elementId
    dobj.Update
End Sub

Main