Option Explicit

Const FULL_REBUILD = True

Sub Main()

    On Error Resume Next

    Dim selectedPkg
    Dim pkgPack

    Session.Output "==== INICIO GENERACION 3.2.4 ANALISIS DE PAQUETES - SPRINT 1 ===="

    Set selectedPkg = Repository.GetTreeSelectedPackage()

    If selectedPkg Is Nothing Then
        Session.Output "Selecciona un package en el Browser antes de ejecutar el script."
        Exit Sub
    End If

    Set pkgPack = CreateOrGetPackage(selectedPkg, "3.2.4 Analisis de paquetes")

    If FULL_REBUILD Then
        Session.Output "Limpiando 3.2.4 ..."
        ResetPackageContents pkgPack
    End If

    ' =========================================================
    ' 3.2.4 ANALISIS DE PAQUETES - SPRINT 1
    ' =========================================================
    BuildAnalisisDePaquetes pkgPack

    Session.Output "Generacion completada - Sprint 1."
    Session.Output "==== FIN GENERACION 3.2.4 ===="

End Sub

' =========================================================
' LIMPIEZA
' =========================================================

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

' =========================================================
' HELPERS GENERALES
' =========================================================

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

Sub ClearDiagramObjects(dgm)
    Dim i
    For i = dgm.DiagramObjects.Count - 1 To 0 Step -1
        dgm.DiagramObjects.DeleteAt i, False
    Next
    dgm.DiagramObjects.Refresh
End Sub

Sub AddElementToDiagram(dgm, elem, l, t, r, b)
    Dim dobj
    Set dobj = dgm.DiagramObjects.AddNew("l=" & l & ";r=" & r & ";t=" & (-t) & ";b=" & (-b), "")
    dobj.ElementID = elem.ElementID
    dobj.Update
End Sub

Sub AddPackageToDiagram(dgm, pkgObj, l, t, r, b)
    Dim pkgElem
    Set pkgElem = pkgObj.Element
    AddElementToDiagram dgm, pkgElem, l, t, r, b
End Sub

Function GetElementByName(pkg, elemName)
    Dim i
    Dim el

    Set GetElementByName = Nothing

    For i = 0 To pkg.Elements.Count - 1
        Set el = pkg.Elements.GetAt(i)
        If el.Name = elemName Then
            Set GetElementByName = el
            Exit Function
        End If
    Next
End Function

Function CreateOrGetActor(pkg, elementName)
    Dim el

    Set el = GetElementByName(pkg, elementName)
    If Not el Is Nothing Then
        Set CreateOrGetActor = el
        Exit Function
    End If

    Set el = pkg.Elements.AddNew(elementName, "Actor")
    el.Update
    pkg.Elements.Refresh

    Set CreateOrGetActor = el
End Function

Function CreateOrGetAnalysisObject(pkg, elementName, stereotypeName)
    Dim el

    Set el = GetElementByName(pkg, elementName)
    If Not el Is Nothing Then
        el.Stereotype = stereotypeName
        el.Update
        Set CreateOrGetAnalysisObject = el
        Exit Function
    End If

    Set el = pkg.Elements.AddNew(elementName, "Object")
    el.Stereotype = stereotypeName
    el.Update
    pkg.Elements.Refresh

    Set CreateOrGetAnalysisObject = el
End Function

Function ConnectorExistsByName(sourceElem, targetElem, connType, connName)
    Dim i
    Dim c

    ConnectorExistsByName = False

    For i = 0 To sourceElem.Connectors.Count - 1
        Set c = sourceElem.Connectors.GetAt(i)

        If LCase(c.Type) = LCase(connType) And c.SupplierID = targetElem.ElementID Then
            If c.Name = connName Then
                ConnectorExistsByName = True
                Exit Function
            End If
        End If
    Next
End Function

Function ConnectorExistsSimple(sourceElem, targetElem, connType)
    Dim i
    Dim c

    ConnectorExistsSimple = False

    For i = 0 To sourceElem.Connectors.Count - 1
        Set c = sourceElem.Connectors.GetAt(i)
        If LCase(c.Type) = LCase(connType) And c.SupplierID = targetElem.ElementID Then
            ConnectorExistsSimple = True
            Exit Function
        End If
    Next
End Function

Sub AddMessageAssociation(sourceElem, targetElem, msgLabel)
    Dim conn
    Dim finalLabel

    finalLabel = Replace(msgLabel, "~", vbCrLf)

    If Not ConnectorExistsByName(sourceElem, targetElem, "Association", finalLabel) Then
        Set conn = sourceElem.Connectors.AddNew(finalLabel, "Association")
        conn.SupplierID = targetElem.ElementID
        conn.Direction = "Source -> Destination"
        conn.Update
        sourceElem.Connectors.Refresh
    End If
End Sub

Sub AddDependencyIfMissing(sourceElem, targetElem)
    Dim conn

    If Not ConnectorExistsSimple(sourceElem, targetElem, "Dependency") Then
        Set conn = sourceElem.Connectors.AddNew("", "Dependency")
        conn.SupplierID = targetElem.ElementID
        conn.Update
        sourceElem.Connectors.Refresh
    End If
End Sub

' =========================================================
' HELPERS 3.2.3
' =========================================================

Sub BuildCommunicationDiagram(sectionPkg, pkgName, diagramName, participantSpecs, messageSpecs)
    Dim childPkg
    Dim dgm
    Dim i
    Dim parts
    Dim el
    Dim kind
    Dim msg
    Dim sourceEl
    Dim targetEl

    Set childPkg = CreateOrGetPackage(sectionPkg, pkgName)
    Set dgm = CreateOrGetDiagram(childPkg, diagramName, "Analysis")

    ClearDiagramObjects dgm

    For i = 0 To UBound(participantSpecs)
        parts = Split(participantSpecs(i), "|")
        kind = LCase(parts(1))

        If kind = "actor" Then
            Set el = CreateOrGetActor(childPkg, parts(0))
        Else
            Set el = CreateOrGetAnalysisObject(childPkg, parts(0), kind)
        End If

        AddElementToDiagram dgm, el, CInt(parts(2)), CInt(parts(3)), CInt(parts(4)), CInt(parts(5))
    Next

    For i = 0 To UBound(messageSpecs)
        msg = Split(messageSpecs(i), "|")
        Set sourceEl = GetElementByName(childPkg, msg(0))
        Set targetEl = GetElementByName(childPkg, msg(1))

        If Not sourceEl Is Nothing And Not targetEl Is Nothing Then
            AddMessageAssociation sourceEl, targetEl, msg(2)
        End If
    Next

    dgm.Update
    Repository.ReloadDiagram dgm.DiagramID
End Sub

' =========================================================
' 3.2.3 - DIAGRAMAS DE COLABORACION / COMUNICACION
' =========================================================

Sub BuildCU04ReportarEmergencia(sectionPkg)
    BuildCommunicationDiagram sectionPkg, "CU04 Reportar emergencia", "sd CU04 Reportar emergencia", _
        Array( _
            "Cliente|actor|60|220|150|360", _
            "UI.ReporteEmergencia|boundary|250|220|390|360", _
            "IncidenteController|control|500|220|640|360", _
            "vehiculoModel|entity|840|80|980|220", _
            "incidenteModel|entity|840|240|980|380", _
            "evidenciaModel|entity|840|400|980|540" _
        ), _
        Array( _
            "Cliente|UI.ReporteEmergencia|1. ReportarEmergencia(data)", _
            "UI.ReporteEmergencia|IncidenteController|1.1. RegistrarIncidente(data)~1.2. ValidarVehiculo(id)~1.3. GuardarEvidencias(data)", _
            "IncidenteController|vehiculoModel|1.2. ObtenerVehiculo(id)", _
            "IncidenteController|incidenteModel|1.1. Agregar(data)", _
            "IncidenteController|evidenciaModel|1.3. Agregar(data)", _
            "IncidenteController|UI.ReporteEmergencia|1.4. Mostrar()" _
        )
End Sub

Sub BuildCU12AceptarRechazarSolicitud(sectionPkg)
    BuildCommunicationDiagram sectionPkg, "CU12 Aceptar o rechazar solicitud", "sd CU12 Aceptar o rechazar solicitud", _
        Array( _
            "AdministradorTaller|actor|60|220|180|360", _
            "UI.Solicitudes|boundary|250|220|390|360", _
            "SolicitudController|control|500|220|640|360", _
            "incidenteModel|entity|840|100|980|240", _
            "candidatoTallerModel|entity|840|300|1020|440" _
        ), _
        Array( _
            "AdministradorTaller|UI.Solicitudes|1. AceptarSolicitud(id)~2. RechazarSolicitud(id)", _
            "UI.Solicitudes|SolicitudController|1.1. ProcesarDecision(id, estado)", _
            "SolicitudController|incidenteModel|1.2. ObtenerSolicitud(id)~1.3. ActualizarEstado(data)", _
            "SolicitudController|candidatoTallerModel|1.4. ActualizarEstadoCandidato(data)", _
            "SolicitudController|UI.Solicitudes|1.5. Mostrar()" _
        )
End Sub

Sub BuildCU14GestionarAtencion(sectionPkg)
    BuildCommunicationDiagram sectionPkg, "CU14 Gestionar atencion del servicio", "sd CU14 Gestionar atencion del servicio", _
        Array( _
            "AdministradorTaller|actor|60|220|180|360", _
            "UI.AtencionServicio|boundary|250|220|410|360", _
            "AtencionController|control|520|220|660|360", _
            "servicioAtencionModel|entity|860|80|1020|220", _
            "historialEstadoModel|entity|860|240|1040|380", _
            "notificacionModel|entity|860|400|1020|540" _
        ), _
        Array( _
            "AdministradorTaller|UI.AtencionServicio|1. ActualizarAtencion(data)", _
            "UI.AtencionServicio|AtencionController|1.1. GestionarAtencion(data)", _
            "AtencionController|servicioAtencionModel|1.2. ActualizarEstado(data)", _
            "AtencionController|historialEstadoModel|1.3. RegistrarCambio(data)", _
            "AtencionController|notificacionModel|1.4. CrearNotificacion(data)", _
            "AtencionController|UI.AtencionServicio|1.5. Mostrar()" _
        )
End Sub

Sub BuildCU18SeleccionarTalleres(sectionPkg)
    BuildCommunicationDiagram sectionPkg, "CU18 Seleccionar talleres candidatos", "sd CU18 Seleccionar talleres candidatos", _
        Array( _
            "Cliente|actor|60|220|150|360", _
            "UI.ReporteEmergencia|boundary|250|220|390|360", _
            "AsignacionInteligenteController|control|500|220|700|360", _
            "incidenteModel|entity|880|80|1020|220", _
            "tallerModel|entity|880|240|1020|380", _
            "candidatoTallerModel|entity|880|400|1080|540" _
        ), _
        Array( _
            "Cliente|UI.ReporteEmergencia|1. ReportarEmergencia(data)", _
            "UI.ReporteEmergencia|AsignacionInteligenteController|1.1. SeleccionarTalleresCandidatos()", _
            "AsignacionInteligenteController|incidenteModel|1.2. ObtenerIncidenteClasificado(id)", _
            "AsignacionInteligenteController|tallerModel|1.3. ObtenerTalleresCompatibles(id)", _
            "AsignacionInteligenteController|candidatoTallerModel|1.4. RegistrarRanking(data)", _
            "AsignacionInteligenteController|UI.ReporteEmergencia|1.5. Mostrar()" _
        )
End Sub

Sub BuildCU19RegistrarPago(sectionPkg)
    BuildCommunicationDiagram sectionPkg, "CU19 Registrar pago", "sd CU19 Registrar pago", _
        Array( _
            "Cliente|actor|60|220|150|360", _
            "UI.Pagos|boundary|250|220|390|360", _
            "PagoController|control|500|220|640|360", _
            "incidenteModel|entity|840|80|980|220", _
            "VPAYService|entity|840|240|1020|380", _
            "pagoModel|entity|840|400|980|540" _
        ), _
        Array( _
            "Cliente|UI.Pagos|1. IniciarPago()", _
            "UI.Pagos|PagoController|1.1. RegistrarPago(data)", _
            "PagoController|incidenteModel|1.2. ObtenerMontoFinal(id)", _
            "PagoController|VPAYService|1.3. ProcesarTransaccion(data)", _
            "PagoController|pagoModel|1.4. Agregar(data)", _
            "PagoController|UI.Pagos|1.5. Mostrar()" _
        )
End Sub

' =========================================================
' 3.2.4 - ANALISIS DE PAQUETES - SPRINT 1
' Sistema de Gestion Academica SaaS
' =========================================================

Sub BuildAnalisisDePaquetes(sectionPkg)
    Dim dgm
    Dim pSeg, pInst, pEstr, pPers, pOper, pAud
    Dim eSeg, eInst, eEstr, ePers, eOper, eAud

    ' --- Paquetes del sistema ---
    Set pSeg  = CreateOrGetPackage(sectionPkg, "Seguridad y Acceso")
    Set pInst = CreateOrGetPackage(sectionPkg, "Gestion Institucional")
    Set pEstr = CreateOrGetPackage(sectionPkg, "Estructura Academica")
    Set pPers = CreateOrGetPackage(sectionPkg, "Personas Academicas")
    Set pOper = CreateOrGetPackage(sectionPkg, "Operacion Academica")
    Set pAud  = CreateOrGetPackage(sectionPkg, "Auditoria")

    ' Notas de contenido (clases del modelo)
    pSeg.Notes  = "Tablas: rol, usuario, usuario_rol"
    pInst.Notes = "Tablas: institucion, configuracion_institucion"
    pEstr.Notes = "Tablas: gestion_academica, curso, paralelo, materia, curso_materia"
    pPers.Notes = "Tablas: docente, estudiante, tutor, estudiante_tutor"
    pOper.Notes = "Tablas: inscripcion, asignacion_docente"
    pAud.Notes  = "Tablas: bitacora_auditoria"
    pSeg.Update
    pInst.Update
    pEstr.Update
    pPers.Update
    pOper.Update
    pAud.Update

    ' --- Diagrama de paquetes ---
    Set dgm = CreateOrGetDiagram(sectionPkg, "pkg Analisis de paquetes - Sprint 1", "Package")
    ClearDiagramObjects dgm

    Set eSeg  = pSeg.Element
    Set eInst = pInst.Element
    Set eEstr = pEstr.Element
    Set ePers = pPers.Element
    Set eOper = pOper.Element
    Set eAud  = pAud.Element

    ' --- Layout en 3 filas ---
    ' Fila 1: Institucional (centro)
    AddPackageToDiagram dgm, pInst, 420, 60, 780, 200
    ' Fila 2: Seguridad | Estructura academica | Personas
    AddPackageToDiagram dgm, pSeg,  40, 280, 380, 420
    AddPackageToDiagram dgm, pEstr, 440, 280, 900, 420
    AddPackageToDiagram dgm, pPers, 960, 280, 1360, 420
    ' Fila 3: Operacion (centro) | Auditoria
    AddPackageToDiagram dgm, pOper, 360, 540, 760, 680
    AddPackageToDiagram dgm, pAud,  820, 540, 1160, 680

    ' --- Dependencias ---
    ' Todos dependen de Institucional (configuracion raiz)
    AddDependencyIfMissing eSeg,  eInst
    AddDependencyIfMissing eEstr, eInst
    AddDependencyIfMissing ePers, eInst

    ' Personas y operacion dependen de estructura academica
    AddDependencyIfMissing ePers, eEstr
    AddDependencyIfMissing eOper, eEstr

    ' Operacion depende de personas
    AddDependencyIfMissing eOper, ePers

    ' Auditoria depende de seguridad (usuario) y de todos (registro de operaciones)
    AddDependencyIfMissing eAud, eSeg
    AddDependencyIfMissing eAud, eOper

    dgm.Update
    Repository.ReloadDiagram dgm.DiagramID
End Sub

Main