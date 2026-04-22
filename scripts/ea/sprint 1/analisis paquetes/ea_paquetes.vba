Option Explicit

Const FULL_REBUILD = True

Sub Main()

    On Error Resume Next

    Dim selectedPkg
    Dim pkgIdentify
    Dim pkgRelate

    Session.Output "==== INICIO GENERACION 3.2.1 Y 3.2.2 ===="

    Set selectedPkg = Repository.GetTreeSelectedPackage()

    If selectedPkg Is Nothing Then
        Session.Output "Selecciona un package en el Browser antes de ejecutar el script."
        Exit Sub
    End If

    Set pkgIdentify = CreateOrGetPackage(selectedPkg, "3.2.1 Identificar paquetes")
    Set pkgRelate = CreateOrGetPackage(selectedPkg, "3.2.2 Relacionar paquetes y casos de uso")

    If FULL_REBUILD Then
        Session.Output "Limpiando 3.2.1 ..."
        ResetPackageContents pkgIdentify

        Session.Output "Limpiando 3.2.2 ..."
        ResetPackageContents pkgRelate
    End If

    ' =========================================================
    ' 3.2.1 IDENTIFICAR PAQUETES
    ' =========================================================
    BuildIdentifyPackage pkgIdentify, "Usuarios y acceso"
    BuildIdentifyPackage pkgIdentify, "Clientes y vehiculos"
    BuildIdentifyPackage pkgIdentify, "Incidentes y evidencias"
    BuildIdentifyPackage pkgIdentify, "Talleres y atencion del servicio"
    BuildIdentifyPackage pkgIdentify, "Procesamiento inteligente y asignacion"
    BuildIdentifyPackage pkgIdentify, "Pagos, notificaciones y reportes"

    ' =========================================================
    ' 3.2.2 RELACIONAR PAQUETES Y CASOS DE USO
    ' =========================================================

    ' 1. Usuarios y acceso
    BuildPackageRelationDiagram pkgRelate, "Usuarios y acceso", Array( _
        "CU01. Registrar usuario", _
        "CU02. Iniciar sesion" _
    )

    ' 2. Clientes y vehiculos
    BuildPackageRelationDiagram pkgRelate, "Clientes y vehiculos", Array( _
        "CU03. Registrar vehiculo" _
    )

    ' 3. Incidentes y evidencias
    BuildPackageRelationDiagram pkgRelate, "Incidentes y evidencias", Array( _
        "CU04. Reportar emergencia", _
        "CU05. Adjuntar evidencias", _
        "CU06. Consultar estado de solicitud" _
    )
    AddPackageUseCaseRelation pkgRelate, "Incidentes y evidencias", _
        "CU05. Adjuntar evidencias", "CU04. Reportar emergencia", "extend"

    ' 4. Talleres y atencion del servicio
    BuildPackageRelationDiagram pkgRelate, "Talleres y atencion del servicio", Array( _
        "CU08. Gestionar informacion del taller", _
        "CU09. Gestionar tecnicos", _
        "CU10. Gestionar disponibilidad", _
        "CU11. Visualizar solicitudes disponibles", _
        "CU12. Aceptar o rechazar solicitud", _
        "CU13. Asignar tecnico", _
        "CU14. Gestionar atencion del servicio" _
    )
    AddPackageUseCaseRelation pkgRelate, "Talleres y atencion del servicio", _
        "CU12. Aceptar o rechazar solicitud", "CU11. Visualizar solicitudes disponibles", "extend"

    ' 5. Procesamiento inteligente y asignacion
    BuildPackageRelationDiagram pkgRelate, "Procesamiento inteligente y asignacion", Array( _
        "CU15. Generar resumen del incidente", _
        "CU16. Analizar evidencias del incidente", _
        "CU17. Clasificar y priorizar incidente", _
        "CU18. Seleccionar talleres candidatos" _
    )
    AddPackageUseCaseRelation pkgRelate, "Procesamiento inteligente y asignacion", _
        "CU17. Clasificar y priorizar incidente", "CU16. Analizar evidencias del incidente", "include"

    ' 6. Pagos, notificaciones y reportes
    BuildPackageRelationDiagram pkgRelate, "Pagos, notificaciones y reportes", Array( _
        "CU07. Recibir notificaciones push del servicio", _
        "CU19. Registrar pago", _
        "CU20. Calificar servicio", _
        "CU21. Consultar historial de atenciones", _
        "CU22. Gestionar metricas y reportes" _
    )

    Session.Output "Generacion completada."
    Session.Output "==== FIN GENERACION 3.2.1 Y 3.2.2 ===="

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

Function GetChildPackageByName(parentPkg, pkgName)
    Dim i
    Dim p

    Set GetChildPackageByName = Nothing

    For i = 0 To parentPkg.Packages.Count - 1
        Set p = parentPkg.Packages.GetAt(i)
        If p.Name = pkgName Then
            Set GetChildPackageByName = p
            Exit Function
        End If
    Next
End Function

Function CreateOrGetElement(pkg, elementName, elementType)
    Dim i
    Dim el

    For i = 0 To pkg.Elements.Count - 1
        Set el = pkg.Elements.GetAt(i)
        If el.Name = elementName Then
            Set CreateOrGetElement = el
            Exit Function
        End If
    Next

    Set el = pkg.Elements.AddNew(elementName, elementType)
    el.Update
    pkg.Elements.Refresh

    Set CreateOrGetElement = el
End Function

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
    Set dobj = dgm.DiagramObjects.AddNew("", "")
    dobj.ElementID = elem.ElementID
    dobj.Left   = l
    dobj.Right  = r
    dobj.Top    = -t
    dobj.Bottom = -b
    dobj.Update
End Sub

Sub AddPackageToDiagram(dgm, pkgObj, l, t, r, b)
    Dim dobj
    Set dobj = dgm.DiagramObjects.AddNew("", "")
    dobj.ElementID = pkgObj.Element.ElementID
    dobj.Left   = l
    dobj.Right  = r
    dobj.Top    = -t
    dobj.Bottom = -b
    dobj.Update
End Sub

Function ConnectorExists(sourceElem, targetElem, connType, stereo)
    Dim i
    Dim c

    ConnectorExists = False

    For i = 0 To sourceElem.Connectors.Count - 1
        Set c = sourceElem.Connectors.GetAt(i)

        If LCase(c.Type) = LCase(connType) And c.SupplierID = targetElem.ElementID Then
            If LCase(stereo) = "" Then
                ConnectorExists = True
                Exit Function
            ElseIf LCase(c.Stereotype) = LCase(stereo) Then
                ConnectorExists = True
                Exit Function
            End If
        End If
    Next
End Function

Sub AddDependencyIfMissing(sourceElem, targetElem, stereo)
    Dim conn

    If Not ConnectorExists(sourceElem, targetElem, "Dependency", stereo) Then
        Set conn = sourceElem.Connectors.AddNew("", "Dependency")
        conn.SupplierID = targetElem.ElementID
        conn.Stereotype = stereo
        conn.Update
        sourceElem.Connectors.Refresh
    End If
End Sub

Sub AddUseCaseRelationIfMissing(sourceElem, targetElem, stereo)
    Dim conn

    If Not ConnectorExists(sourceElem, targetElem, "Dependency", stereo) Then
        Set conn = sourceElem.Connectors.AddNew("", "Dependency")
        conn.SupplierID = targetElem.ElementID
        conn.Stereotype = stereo
        conn.Update
        sourceElem.Connectors.Refresh
    End If
End Sub

' =========================================================
' 3.2.1 IDENTIFICAR PAQUETES
' =========================================================

Sub BuildIdentifyPackage(sectionPkg, pkgName)
    Dim childPkg
    Dim dgm

    Set childPkg = CreateOrGetPackage(sectionPkg, pkgName)
    Set dgm = CreateOrGetDiagram(childPkg, "pkg Paquetes", "Package")

    ClearDiagramObjects dgm
    AddPackageToDiagram dgm, childPkg, 120, 120, 520, 300

    dgm.Update
    Repository.ReloadDiagram dgm.DiagramID
End Sub

' =========================================================
' 3.2.2 RELACIONAR PAQUETES Y CASOS DE USO
' =========================================================

Sub BuildPackageRelationDiagram(sectionPkg, pkgName, useCaseNames)
    Dim relationPkg
    Dim dgm
    Dim pkgElem
    Dim uc
    Dim i
    Dim topY

    Set relationPkg = CreateOrGetPackage(sectionPkg, pkgName)
    Set dgm = CreateOrGetDiagram(relationPkg, "pkg " & pkgName, "Package")

    ClearDiagramObjects dgm

    Set pkgElem = relationPkg.Element
    AddElementToDiagram dgm, pkgElem, 80, 260, 420, 520

    topY = 80
    For i = 0 To UBound(useCaseNames)
        Set uc = CreateOrGetElement(relationPkg, useCaseNames(i), "UseCase")
        AddDependencyIfMissing pkgElem, uc, "trace"
        AddElementToDiagram dgm, uc, 700, topY, 1180, topY + 120
        topY = topY + 150
    Next

    dgm.Update
    Repository.ReloadDiagram dgm.DiagramID
End Sub

Sub AddPackageUseCaseRelation(sectionPkg, pkgName, sourceUCName, targetUCName, stereo)
    Dim relationPkg
    Dim sourceUC
    Dim targetUC
    Dim dgm

    Set relationPkg = GetChildPackageByName(sectionPkg, pkgName)
    If relationPkg Is Nothing Then Exit Sub

    Set sourceUC = GetElementByName(relationPkg, sourceUCName)
    Set targetUC = GetElementByName(relationPkg, targetUCName)

    If sourceUC Is Nothing Then Exit Sub
    If targetUC Is Nothing Then Exit Sub

    AddUseCaseRelationIfMissing sourceUC, targetUC, stereo

    Set dgm = CreateOrGetDiagram(relationPkg, "pkg " & pkgName, "Package")
    dgm.Update
    Repository.ReloadDiagram dgm.DiagramID
End Sub

Main