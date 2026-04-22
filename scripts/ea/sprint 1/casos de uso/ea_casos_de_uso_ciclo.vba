Option Explicit

Const FULL_REBUILD = True

Sub Main()

    On Error Resume Next

    Dim selectedPkg
    Dim pkgCases
    Dim pkgCycles

    Session.Output "==== INICIO REGENERACION CASOS DE USO - SPRINT 1 ===="

    Set selectedPkg = Repository.GetTreeSelectedPackage()

    If selectedPkg Is Nothing Then
        Session.Output "Selecciona un package en el Browser antes de ejecutar el script."
        Exit Sub
    End If

    Set pkgCases = CreateOrGetPackage(selectedPkg, "3.1.4 Especificar casos de uso")
    Set pkgCycles = CreateOrGetPackage(selectedPkg, "3.1.5 Estructurar Modelo de Casos de Uso")

    If FULL_REBUILD Then
        Session.Output "Limpiando contenido previo de 3.1.4 ..."
        ResetPackageContents pkgCases
        Session.Output "Limpiando contenido previo de 3.1.5 ..."
        ResetPackageContents pkgCycles
    End If

    ' =========================
    ' 3.1.4 Casos individuales Sprint 1
    ' =========================

    ' --- CONTEXTO INSTITUCIONAL Y ACCESO ---
    BuildCaseMultiActor pkgCases, _
        "CU-S1-01 Registrar institucion educativa", _
        "CU-S1-01. Registrar institucion educativa", _
        Array("Super Admin")

    BuildCaseMultiActor pkgCases, _
        "CU-S1-02 Configurar institucion educativa", _
        "CU-S1-02. Configurar institucion educativa", _
        Array("Admin Institucion")

    BuildCaseMultiActor pkgCases, _
        "CU-S1-03 Iniciar sesion", _
        "CU-S1-03. Iniciar sesion", _
        Array("Super Admin", "Admin Institucion", "Director", "Secretario", "Docente")

    BuildCaseMultiActor pkgCases, _
        "CU-S1-04 Gestionar usuarios", _
        "CU-S1-04. Gestionar usuarios", _
        Array("Admin Institucion")

    BuildCaseMultiActor pkgCases, _
        "CU-S1-05 Asignar roles a usuarios", _
        "CU-S1-05. Asignar roles a usuarios", _
        Array("Admin Institucion")

    ' --- ESTRUCTURA ACADEMICA ---
    BuildCaseMultiActor pkgCases, _
        "CU-S1-06 Gestionar gestion academica", _
        "CU-S1-06. Gestionar gestion academica", _
        Array("Admin Institucion", "Director")

    BuildCaseMultiActor pkgCases, _
        "CU-S1-07 Gestionar cursos y paralelos", _
        "CU-S1-07. Gestionar cursos y paralelos", _
        Array("Admin Institucion", "Director")

    BuildCaseMultiActor pkgCases, _
        "CU-S1-08 Gestionar materias y asignarlas a cursos", _
        "CU-S1-08. Gestionar materias y asignarlas a cursos", _
        Array("Admin Institucion", "Director")

    ' --- PERSONAS ACADEMICAS ---
    BuildCaseMultiActor pkgCases, _
        "CU-S1-09 Gestionar docentes", _
        "CU-S1-09. Gestionar docentes", _
        Array("Admin Institucion")

    BuildCaseMultiActor pkgCases, _
        "CU-S1-10 Gestionar estudiantes", _
        "CU-S1-10. Gestionar estudiantes", _
        Array("Admin Institucion", "Secretario")

    BuildCaseMultiActor pkgCases, _
        "CU-S1-11 Gestionar tutores y vincularlos con estudiantes", _
        "CU-S1-11. Gestionar tutores y vincularlos con estudiantes", _
        Array("Admin Institucion", "Secretario")

    ' --- OPERACION ACADEMICA ---
    BuildCaseMultiActor pkgCases, _
        "CU-S1-12 Inscribir estudiante en gestion, curso y paralelo", _
        "CU-S1-12. Inscribir estudiante en gestion, curso y paralelo", _
        Array("Admin Institucion", "Secretario")

    BuildCaseMultiActor pkgCases, _
        "CU-S1-13 Asignar docente a materia y paralelo", _
        "CU-S1-13. Asignar docente a materia y paralelo", _
        Array("Admin Institucion", "Director")

    ' =========================
    ' 3.1.5 Diagrama general Sprint 1
    ' =========================
    BuildDiagramaGeneralSprint1 pkgCycles

    Session.Output "Regeneracion completada - Sprint 1."
    Session.Output "==== FIN REGENERACION CASOS DE USO ===="

End Sub

' =========================================================
' Limpieza
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
' Helpers generales
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

Function CreateOrGetDiagram(pkg, diagramName)
    Dim i
    Dim dgm

    For i = 0 To pkg.Diagrams.Count - 1
        Set dgm = pkg.Diagrams.GetAt(i)
        If dgm.Name = diagramName Then
            Set CreateOrGetDiagram = dgm
            Exit Function
        End If
    Next

    Set dgm = pkg.Diagrams.AddNew(diagramName, "Use Case")
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

Sub AddAssociationIfMissing(sourceElem, targetElem)
    Dim conn

    If Not ConnectorExists(sourceElem, targetElem, "Association", "") Then
        Set conn = sourceElem.Connectors.AddNew("", "Association")
        conn.SupplierID = targetElem.ElementID
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

Sub AddElementToDiagram(dgm, elem, l, t, r, b)
    Dim dobj
    Set dobj = dgm.DiagramObjects.AddNew("l=" & l & ";r=" & r & ";t=" & (-t) & ";b=" & (-b), "")
    dobj.ElementID = elem.ElementID
    dobj.Update
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

' =========================================================
' Builders casos individuales
' =========================================================

Sub BuildCaseMultiActor(parentPkg, pkgName, ucName, actorNames)
    Dim casePkg
    Dim dgm
    Dim uc
    Dim i
    Dim actor
    Dim y

    Set casePkg = CreateOrGetPackage(parentPkg, pkgName)
    Set uc = CreateOrGetElement(casePkg, ucName, "UseCase")

    For i = 0 To UBound(actorNames)
        Set actor = CreateOrGetElement(casePkg, actorNames(i), "Actor")
        AddAssociationIfMissing actor, uc
    Next

    Set dgm = CreateOrGetDiagram(casePkg, pkgName)
    ClearDiagramObjects dgm

    y = 120
    For i = 0 To UBound(actorNames)
        Set actor = GetElementByName(casePkg, actorNames(i))
        AddElementToDiagram dgm, actor, 80, y, 300, y + 140
        y = y + 180
    Next

    AddElementToDiagram dgm, uc, 560, 240, 1120, 380

    dgm.Update
    Repository.ReloadDiagram dgm.DiagramID
End Sub

' =========================================================
' 3.1.5 DIAGRAMA GENERAL DE CASOS DE USO - SPRINT 1
' =========================================================

Sub BuildDiagramaGeneralSprint1(parentPkg)
    Dim pkg
    Dim dgm

    Dim SA, AI, DR, SC, DC

    Dim CU01, CU02, CU03, CU04, CU05
    Dim CU06, CU07, CU08, CU09, CU10
    Dim CU11, CU12, CU13

    Set pkg = CreateOrGetPackage(parentPkg, "Sprint 1")
    Set dgm = CreateOrGetDiagram(pkg, "Diagrama general de casos de uso - Sprint 1")
    ClearDiagramObjects dgm

    ' --- ACTORES ---
    Set SA = CreateOrGetElement(pkg, "Super Admin", "Actor")
    Set AI = CreateOrGetElement(pkg, "Admin Institucion", "Actor")
    Set DR = CreateOrGetElement(pkg, "Director", "Actor")
    Set SC = CreateOrGetElement(pkg, "Secretario", "Actor")
    Set DC = CreateOrGetElement(pkg, "Docente", "Actor")

    ' --- CASOS DE USO ---
    Set CU01 = CreateOrGetElement(pkg, "CU-S1-01. Registrar institucion educativa", "UseCase")
    Set CU02 = CreateOrGetElement(pkg, "CU-S1-02. Configurar institucion educativa", "UseCase")
    Set CU03 = CreateOrGetElement(pkg, "CU-S1-03. Iniciar sesion", "UseCase")
    Set CU04 = CreateOrGetElement(pkg, "CU-S1-04. Gestionar usuarios", "UseCase")
    Set CU05 = CreateOrGetElement(pkg, "CU-S1-05. Asignar roles a usuarios", "UseCase")
    Set CU06 = CreateOrGetElement(pkg, "CU-S1-06. Gestionar gestion academica", "UseCase")
    Set CU07 = CreateOrGetElement(pkg, "CU-S1-07. Gestionar cursos y paralelos", "UseCase")
    Set CU08 = CreateOrGetElement(pkg, "CU-S1-08. Gestionar materias y asignarlas a cursos", "UseCase")
    Set CU09 = CreateOrGetElement(pkg, "CU-S1-09. Gestionar docentes", "UseCase")
    Set CU10 = CreateOrGetElement(pkg, "CU-S1-10. Gestionar estudiantes", "UseCase")
    Set CU11 = CreateOrGetElement(pkg, "CU-S1-11. Gestionar tutores y vincularlos con estudiantes", "UseCase")
    Set CU12 = CreateOrGetElement(pkg, "CU-S1-12. Inscribir estudiante en gestion, curso y paralelo", "UseCase")
    Set CU13 = CreateOrGetElement(pkg, "CU-S1-13. Asignar docente a materia y paralelo", "UseCase")

    ' --- ASOCIACIONES ACTOR -> CASO DE USO ---
    AddAssociationIfMissing SA, CU01
    AddAssociationIfMissing SA, CU03

    AddAssociationIfMissing AI, CU02
    AddAssociationIfMissing AI, CU03
    AddAssociationIfMissing AI, CU04
    AddAssociationIfMissing AI, CU05
    AddAssociationIfMissing AI, CU06
    AddAssociationIfMissing AI, CU07
    AddAssociationIfMissing AI, CU08
    AddAssociationIfMissing AI, CU09
    AddAssociationIfMissing AI, CU10
    AddAssociationIfMissing AI, CU11
    AddAssociationIfMissing AI, CU12
    AddAssociationIfMissing AI, CU13

    AddAssociationIfMissing DR, CU03
    AddAssociationIfMissing DR, CU06
    AddAssociationIfMissing DR, CU07
    AddAssociationIfMissing DR, CU08
    AddAssociationIfMissing DR, CU13

    AddAssociationIfMissing SC, CU03
    AddAssociationIfMissing SC, CU10
    AddAssociationIfMissing SC, CU11
    AddAssociationIfMissing SC, CU12

    AddAssociationIfMissing DC, CU03

    ' --- RELACIONES ENTRE CASOS DE USO ---
    AddUseCaseRelationIfMissing CU04, CU05, "extend"

    ' --- POSICIONES ---
    ' Actores
    AddElementToDiagram dgm, SA,  60,   60, 240,  200
    AddElementToDiagram dgm, AI,  60,  260, 240,  400
    AddElementToDiagram dgm, DR,  60,  460, 240,  600
    AddElementToDiagram dgm, SC,  60,  660, 240,  800
    AddElementToDiagram dgm, DC,  60,  860, 240, 1000

    ' Bloque institucional / acceso
    AddElementToDiagram dgm, CU01,  320,   60,  860,  180
    AddElementToDiagram dgm, CU02,  940,   60, 1500,  180
    AddElementToDiagram dgm, CU03,  320,  220,  860,  340
    AddElementToDiagram dgm, CU04,  940,  220, 1500,  340
    AddElementToDiagram dgm, CU05, 1560,  220, 2120,  340

    ' Bloque estructura academica
    AddElementToDiagram dgm, CU06,  320,  420,  900,  540
    AddElementToDiagram dgm, CU07,  940,  420, 1520,  540
    AddElementToDiagram dgm, CU08, 1560,  420, 2200,  540

    ' Bloque personas / operacion academica
    AddElementToDiagram dgm, CU09,  320,  620,  860,  740
    AddElementToDiagram dgm, CU10,  940,  620, 1520,  740
    AddElementToDiagram dgm, CU11, 1560,  620, 2280,  740
    AddElementToDiagram dgm, CU12,  620,  820, 1320,  940
    AddElementToDiagram dgm, CU13, 1420,  820, 2120,  940

    dgm.Update
    Repository.ReloadDiagram dgm.DiagramID
End Sub

Main