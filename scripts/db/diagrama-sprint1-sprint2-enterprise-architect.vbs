' =========================================================
' DIAGRAMA BASE DE DATOS - SPRINT 1 Y SPRINT 2
' Enterprise Architect / Sparx Systems - VBScript
'
' Uso:
' 1. Abrir Enterprise Architect.
' 2. Ir a Specialize > Tools > Scripting, o usar la ventana de scripts.
' 3. Crear un script VBScript nuevo.
' 4. Pegar este contenido y ejecutar Main.
'
' Resultado:
' - Crea un paquete llamado "BD Sprint 1 y 2".
' - Crea un diagrama con tablas principales.
' - Agrega columnas como atributos.
' - Dibuja relaciones FK principales.
' =========================================================

Option Explicit

Dim tables
Set tables = CreateObject("Scripting.Dictionary")

Sub Main()
    Dim rootPackage
    Set rootPackage = Repository.Models.GetAt(0)

    Dim package
    Set package = rootPackage.Packages.AddNew("BD Sprint 1 y 2", "")
    package.Update
    rootPackage.Packages.Refresh

    Dim diagram
    Set diagram = package.Diagrams.AddNew("DER Sprint 1 y 2", "Logical")
    diagram.Update
    package.Diagrams.Refresh

    CrearTablas package, diagram
    CrearRelaciones

    Repository.ReloadPackage package.PackageID
    Repository.OpenDiagram diagram.DiagramID
    Repository.GetProjectInterface.LayoutDiagramEx diagram.DiagramGUID, 4, 4, 20, 20, False
    Repository.SaveDiagram diagram.DiagramID

    Session.Output "Diagrama Sprint 1 y 2 generado correctamente."
End Sub

Sub CrearTablas(package, diagram)
    AgregarTabla package, diagram, "institucion", Array( _
        "id : UUID PK", "codigo : VARCHAR(30)", "nombre : VARCHAR(200)", _
        "tipo_institucion : VARCHAR(20)", "telefono : VARCHAR(30)", "correo : VARCHAR(255)", _
        "direccion : VARCHAR(255)", "estado : VARCHAR(15)", "creado_en : TIMESTAMP", _
        "actualizado_en : TIMESTAMP" _
    )

    AgregarTabla package, diagram, "configuracion_institucion", Array( _
        "id : UUID PK", "id_institucion : UUID FK", "clave : VARCHAR(100)", _
        "valor : TEXT", "tipo_valor : VARCHAR(30)", "descripcion : VARCHAR(255)", _
        "creado_en : TIMESTAMP", "actualizado_en : TIMESTAMP" _
    )

    AgregarTabla package, diagram, "rol", Array( _
        "id : UUID PK", "codigo : VARCHAR(50)", "nombre : VARCHAR(100)", _
        "id_institucion : UUID FK", "descripcion : VARCHAR(255)", "es_global : BOOLEAN", _
        "estado : VARCHAR(15)", "creado_en : TIMESTAMP", "actualizado_en : TIMESTAMP" _
    )

    AgregarTabla package, diagram, "permiso", Array( _
        "id : UUID PK", "codigo : VARCHAR(60)", "nombre : VARCHAR(120)", _
        "modulo : VARCHAR(60)", "accion : VARCHAR(30)", "descripcion : VARCHAR(255)", _
        "estado : VARCHAR(15)", "creado_en : TIMESTAMP", "actualizado_en : TIMESTAMP" _
    )

    AgregarTabla package, diagram, "rol_permiso", Array( _
        "id : UUID PK", "id_rol : UUID FK", "id_permiso : UUID FK", _
        "creado_en : TIMESTAMP", "actualizado_en : TIMESTAMP" _
    )

    AgregarTabla package, diagram, "usuario", Array( _
        "id : UUID PK", "id_institucion : UUID FK", "correo : VARCHAR(255)", _
        "hash_contrasena : TEXT", "nombres : VARCHAR(120)", "apellidos : VARCHAR(120)", _
        "telefono : VARCHAR(30)", "requiere_cambio_contrasena : BOOLEAN", _
        "estado : VARCHAR(15)", "ultimo_acceso : TIMESTAMP", "creado_en : TIMESTAMP", _
        "actualizado_en : TIMESTAMP" _
    )

    AgregarTabla package, diagram, "usuario_rol", Array( _
        "id : UUID PK", "id_usuario : UUID FK", "id_rol : UUID FK", _
        "activo : BOOLEAN", "creado_en : TIMESTAMP", "actualizado_en : TIMESTAMP" _
    )

    AgregarTabla package, diagram, "gestion_academica", Array( _
        "id : UUID PK", "id_institucion : UUID FK", "nombre : VARCHAR(100)", _
        "fecha_inicio : DATE", "fecha_fin : DATE", "activa : BOOLEAN", _
        "estado : VARCHAR(15)", "creado_en : TIMESTAMP", "actualizado_en : TIMESTAMP" _
    )

    AgregarTabla package, diagram, "curso", Array( _
        "id : UUID PK", "id_institucion : UUID FK", "codigo : VARCHAR(30)", _
        "nombre : VARCHAR(100)", "nivel : VARCHAR(50)", "orden_visual : INTEGER", _
        "estado : VARCHAR(15)", "creado_en : TIMESTAMP", "actualizado_en : TIMESTAMP" _
    )

    AgregarTabla package, diagram, "paralelo", Array( _
        "id : UUID PK", "id_institucion : UUID FK", "id_curso : UUID FK", _
        "id_gestion_academica : UUID FK", "nombre : VARCHAR(20)", "capacidad : INTEGER", _
        "estado : VARCHAR(15)", "creado_en : TIMESTAMP", "actualizado_en : TIMESTAMP" _
    )

    AgregarTabla package, diagram, "aula", Array( _
        "id : UUID PK", "id_institucion : UUID FK", "codigo : VARCHAR(30)", _
        "nombre : VARCHAR(120)", "capacidad : INTEGER", "ubicacion : VARCHAR(180)", _
        "recursos : VARCHAR(500)", "estado : VARCHAR(15)", "creado_en : TIMESTAMP", _
        "actualizado_en : TIMESTAMP" _
    )

    AgregarTabla package, diagram, "materia", Array( _
        "id : UUID PK", "id_institucion : UUID FK", "codigo : VARCHAR(30)", _
        "nombre : VARCHAR(120)", "area : VARCHAR(100)", "carga_horaria : INTEGER", _
        "estado : VARCHAR(15)", "creado_en : TIMESTAMP", "actualizado_en : TIMESTAMP" _
    )

    AgregarTabla package, diagram, "curso_materia", Array( _
        "id : UUID PK", "id_institucion : UUID FK", "id_curso : UUID FK", _
        "id_materia : UUID FK", "id_gestion_academica : UUID FK", _
        "carga_horaria : INTEGER", "estado : VARCHAR(15)", _
        "creado_en : TIMESTAMP", "actualizado_en : TIMESTAMP" _
    )

    AgregarTabla package, diagram, "docente", Array( _
        "id : UUID PK", "id_institucion : UUID FK", "id_usuario : UUID FK", _
        "codigo : VARCHAR(30)", "documento_identidad : VARCHAR(30)", _
        "nombres : VARCHAR(120)", "apellidos : VARCHAR(120)", "telefono : VARCHAR(30)", _
        "correo : VARCHAR(255)", "especialidad : VARCHAR(120)", "estado : VARCHAR(15)", _
        "creado_en : TIMESTAMP", "actualizado_en : TIMESTAMP" _
    )

    AgregarTabla package, diagram, "estudiante", Array( _
        "id : UUID PK", "id_institucion : UUID FK", "id_usuario : UUID FK", _
        "codigo_estudiante : VARCHAR(30)", "documento_identidad : VARCHAR(30)", _
        "nombres : VARCHAR(120)", "apellidos : VARCHAR(120)", "fecha_nacimiento : DATE", _
        "sexo : VARCHAR(15)", "direccion : VARCHAR(255)", "telefono : VARCHAR(30)", _
        "correo : VARCHAR(255)", "estado : VARCHAR(15)", "creado_en : TIMESTAMP", _
        "actualizado_en : TIMESTAMP" _
    )

    AgregarTabla package, diagram, "tutor", Array( _
        "id : UUID PK", "id_institucion : UUID FK", "id_usuario : UUID FK", _
        "documento_identidad : VARCHAR(30)", "nombres : VARCHAR(120)", _
        "apellidos : VARCHAR(120)", "telefono : VARCHAR(30)", "correo : VARCHAR(255)", _
        "direccion : VARCHAR(255)", "estado : VARCHAR(15)", "creado_en : TIMESTAMP", _
        "actualizado_en : TIMESTAMP" _
    )

    AgregarTabla package, diagram, "estudiante_tutor", Array( _
        "id : UUID PK", "id_institucion : UUID FK", "id_estudiante : UUID FK", _
        "id_tutor : UUID FK", "parentesco : VARCHAR(50)", "es_principal : BOOLEAN", _
        "estado : VARCHAR(15)", "creado_en : TIMESTAMP", "actualizado_en : TIMESTAMP" _
    )

    AgregarTabla package, diagram, "inscripcion", Array( _
        "id : UUID PK", "id_institucion : UUID FK", "id_estudiante : UUID FK", _
        "id_gestion_academica : UUID FK", "id_paralelo : UUID FK", _
        "fecha_inscripcion : DATE", "estado : VARCHAR(15)", "observacion : VARCHAR(255)", _
        "creado_en : TIMESTAMP", "actualizado_en : TIMESTAMP" _
    )

    AgregarTabla package, diagram, "asignacion_docente", Array( _
        "id : UUID PK", "id_institucion : UUID FK", "id_docente : UUID FK", _
        "id_materia : UUID FK", "id_paralelo : UUID FK", "id_gestion_academica : UUID FK", _
        "carga_horaria : INTEGER", "estado : VARCHAR(15)", "creado_en : TIMESTAMP", _
        "actualizado_en : TIMESTAMP" _
    )

    AgregarTabla package, diagram, "bitacora_auditoria", Array( _
        "id : UUID PK", "id_institucion : UUID FK", "id_usuario : UUID FK", _
        "fecha_evento : TIMESTAMP", "direccion_ip : VARCHAR(50)", _
        "plataforma_cliente : VARCHAR(30)", "agente_usuario : TEXT", _
        "nombre_modulo : VARCHAR(100)", "nombre_entidad : VARCHAR(100)", _
        "id_entidad : VARCHAR(100)", "tipo_operacion : VARCHAR(30)", _
        "datos_antes : TEXT", "datos_despues : TEXT", "exito : BOOLEAN", _
        "mensaje : VARCHAR(255)", "creado_en : TIMESTAMP" _
    )

    AgregarTabla package, diagram, "archivo", Array( _
        "id : UUID PK", "id_institucion : UUID FK", "id_usuario_subio : UUID FK", _
        "nombre_original : VARCHAR(255)", "nombre_archivo : VARCHAR(255)", _
        "extension : VARCHAR(20)", "mime_type : VARCHAR(100)", "tamano_bytes : BIGINT", _
        "bucket_s3 : VARCHAR(150)", "region_s3 : VARCHAR(50)", "key_s3 : TEXT", _
        "etag : VARCHAR(100)", "checksum_sha256 : VARCHAR(128)", "categoria : VARCHAR(30)", _
        "visibilidad : VARCHAR(20)", "estado : VARCHAR(15)", "creado_en : TIMESTAMP", _
        "actualizado_en : TIMESTAMP" _
    )

    AgregarTabla package, diagram, "archivo_referencia", Array( _
        "id : UUID PK", "id_institucion : UUID FK", "id_archivo : UUID FK", _
        "modulo : VARCHAR(50)", "entidad : VARCHAR(50)", "id_entidad : UUID", _
        "tipo_referencia : VARCHAR(30)", "es_principal : BOOLEAN", "orden_visual : INTEGER", _
        "observacion : VARCHAR(255)", "estado : VARCHAR(15)", "creado_en : TIMESTAMP", _
        "actualizado_en : TIMESTAMP" _
    )

    AgregarTabla package, diagram, "asistencia_registro", Array( _
        "id : UUID PK", "id_institucion : UUID FK", "id_asignacion_docente : UUID FK", _
        "registrado_por : UUID FK", "fecha : DATE", "estado : VARCHAR(15)", _
        "creado_en : TIMESTAMP", "actualizado_en : TIMESTAMP" _
    )

    AgregarTabla package, diagram, "asistencia_detalle", Array( _
        "id : UUID PK", "id_asistencia_registro : UUID FK", "id_inscripcion : UUID FK", _
        "estado_asistencia : VARCHAR(15)", "creado_en : TIMESTAMP", "actualizado_en : TIMESTAMP" _
    )

    AgregarTabla package, diagram, "evaluacion", Array( _
        "id : UUID PK", "id_institucion : UUID FK", "id_asignacion_docente : UUID FK", _
        "creado_por : UUID FK", "periodo : INTEGER", "tipo : VARCHAR(40)", _
        "nombre : VARCHAR(120)", "ponderacion : NUMERIC(5,2)", "escala : VARCHAR(15)", _
        "estado : VARCHAR(15)", "creado_en : TIMESTAMP", "actualizado_en : TIMESTAMP" _
    )

    AgregarTabla package, diagram, "calificacion", Array( _
        "id : UUID PK", "id_institucion : UUID FK", "id_evaluacion : UUID FK", _
        "id_inscripcion : UUID FK", "registrado_por : UUID FK", _
        "nota_numerica : NUMERIC(8,2)", "nota_literal : VARCHAR(5)", _
        "creado_en : TIMESTAMP", "actualizado_en : TIMESTAMP" _
    )

    AgregarTabla package, diagram, "calificacion_cambio", Array( _
        "id : UUID PK", "id_institucion : UUID FK", "id_calificacion : UUID FK", _
        "id_usuario : UUID FK", "valor_anterior : VARCHAR(30)", "valor_nuevo : VARCHAR(30)", _
        "razon : VARCHAR(255)", "fecha_cambio : TIMESTAMP" _
    )
End Sub

Sub AgregarTabla(package, diagram, tableName, columns)
    Dim element
    Set element = package.Elements.AddNew(tableName, "Class")
    element.Stereotype = "table"
    element.Update
    package.Elements.Refresh

    Dim i
    For i = LBound(columns) To UBound(columns)
        Dim attribute
        Set attribute = element.Attributes.AddNew(columns(i), "")
        attribute.Stereotype = "column"
        attribute.Update
    Next
    element.Attributes.Refresh

    tables.Add tableName, element

    Dim leftPos, topPos, rightPos, bottomPos
    Dim index
    index = tables.Count - 1
    leftPos = 20 + ((index Mod 5) * 280)
    topPos = 20 + (Int(index / 5) * 220)
    rightPos = leftPos + 240
    bottomPos = topPos + 180

    Dim diagramObject
    Set diagramObject = diagram.DiagramObjects.AddNew( _
        "l=" & leftPos & ";r=" & rightPos & ";t=" & topPos & ";b=" & bottomPos & ";", "" _
    )
    diagramObject.ElementID = element.ElementID
    diagramObject.Update
    diagram.DiagramObjects.Refresh
End Sub

Sub CrearRelaciones()
    Relacion "configuracion_institucion", "institucion", "FK id_institucion"
    Relacion "rol", "institucion", "FK id_institucion"
    Relacion "rol_permiso", "rol", "FK id_rol"
    Relacion "rol_permiso", "permiso", "FK id_permiso"
    Relacion "usuario", "institucion", "FK id_institucion"
    Relacion "usuario_rol", "usuario", "FK id_usuario"
    Relacion "usuario_rol", "rol", "FK id_rol"

    Relacion "gestion_academica", "institucion", "FK id_institucion"
    Relacion "curso", "institucion", "FK id_institucion"
    Relacion "paralelo", "institucion", "FK id_institucion"
    Relacion "paralelo", "curso", "FK id_curso"
    Relacion "paralelo", "gestion_academica", "FK id_gestion_academica"
    Relacion "aula", "institucion", "FK id_institucion"
    Relacion "materia", "institucion", "FK id_institucion"
    Relacion "curso_materia", "institucion", "FK id_institucion"
    Relacion "curso_materia", "curso", "FK id_curso"
    Relacion "curso_materia", "materia", "FK id_materia"
    Relacion "curso_materia", "gestion_academica", "FK id_gestion_academica"

    Relacion "docente", "institucion", "FK id_institucion"
    Relacion "docente", "usuario", "FK id_usuario"
    Relacion "estudiante", "institucion", "FK id_institucion"
    Relacion "estudiante", "usuario", "FK id_usuario"
    Relacion "tutor", "institucion", "FK id_institucion"
    Relacion "tutor", "usuario", "FK id_usuario"
    Relacion "estudiante_tutor", "institucion", "FK id_institucion"
    Relacion "estudiante_tutor", "estudiante", "FK id_estudiante"
    Relacion "estudiante_tutor", "tutor", "FK id_tutor"

    Relacion "inscripcion", "institucion", "FK id_institucion"
    Relacion "inscripcion", "estudiante", "FK id_estudiante"
    Relacion "inscripcion", "gestion_academica", "FK id_gestion_academica"
    Relacion "inscripcion", "paralelo", "FK id_paralelo"
    Relacion "asignacion_docente", "institucion", "FK id_institucion"
    Relacion "asignacion_docente", "docente", "FK id_docente"
    Relacion "asignacion_docente", "materia", "FK id_materia"
    Relacion "asignacion_docente", "paralelo", "FK id_paralelo"
    Relacion "asignacion_docente", "gestion_academica", "FK id_gestion_academica"

    Relacion "bitacora_auditoria", "institucion", "FK id_institucion"
    Relacion "bitacora_auditoria", "usuario", "FK id_usuario"
    Relacion "archivo", "institucion", "FK id_institucion"
    Relacion "archivo", "usuario", "FK id_usuario_subio"
    Relacion "archivo_referencia", "institucion", "FK id_institucion"
    Relacion "archivo_referencia", "archivo", "FK id_archivo"

    Relacion "asistencia_registro", "institucion", "FK id_institucion"
    Relacion "asistencia_registro", "asignacion_docente", "FK id_asignacion_docente"
    Relacion "asistencia_registro", "usuario", "FK registrado_por"
    Relacion "asistencia_detalle", "asistencia_registro", "FK id_asistencia_registro"
    Relacion "asistencia_detalle", "inscripcion", "FK id_inscripcion"

    Relacion "evaluacion", "institucion", "FK id_institucion"
    Relacion "evaluacion", "asignacion_docente", "FK id_asignacion_docente"
    Relacion "evaluacion", "usuario", "FK creado_por"
    Relacion "calificacion", "institucion", "FK id_institucion"
    Relacion "calificacion", "evaluacion", "FK id_evaluacion"
    Relacion "calificacion", "inscripcion", "FK id_inscripcion"
    Relacion "calificacion", "usuario", "FK registrado_por"
    Relacion "calificacion_cambio", "institucion", "FK id_institucion"
    Relacion "calificacion_cambio", "calificacion", "FK id_calificacion"
    Relacion "calificacion_cambio", "usuario", "FK id_usuario"
End Sub

Sub Relacion(childName, parentName, label)
    If Not tables.Exists(childName) Then Exit Sub
    If Not tables.Exists(parentName) Then Exit Sub

    Dim child
    Dim parent
    Set child = tables(childName)
    Set parent = tables(parentName)

    Dim connector
    Set connector = child.Connectors.AddNew(label, "Association")
    connector.SupplierID = parent.ElementID
    connector.ClientID = child.ElementID
    connector.Stereotype = "FK"
    connector.Update
    child.Connectors.Refresh
End Sub

Main
