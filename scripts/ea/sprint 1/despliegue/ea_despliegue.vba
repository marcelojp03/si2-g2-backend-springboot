Option Explicit

Const FULL_REBUILD = True

Sub Main()

    On Error Resume Next

    Dim selectedPkg
    Dim pkgPhysical

    Session.Output "==== INICIO GENERACION 3.3.1.1 DIAGRAMA DE DESPLIEGUE - SPRINT 1 ===="

    Set selectedPkg = Repository.GetTreeSelectedPackage()

    If selectedPkg Is Nothing Then
        Session.Output "Selecciona un package en el Browser antes de ejecutar el script."
        Exit Sub
    End If

    Set pkgPhysical = CreateOrGetPackage(selectedPkg, "3.3.1.1 Diseno fisico")

    If FULL_REBUILD Then
        Session.Output "Limpiando 3.3.1.1 ..."
        ResetPackageContents pkgPhysical
    End If

    BuildDeploymentDiagram pkgPhysical

    Session.Output "Generacion completada - Sprint 1."
    Session.Output "==== FIN GENERACION 3.3.1.1 ===="

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

Function CreateOrGetElement(pkg, elementName, elementType, stereotype)
    Dim el

    Set el = GetElementByName(pkg, elementName)
    If Not el Is Nothing Then
        If stereotype <> "" Then
            el.Stereotype = stereotype
            el.Update
        End If
        Set CreateOrGetElement = el
        Exit Function
    End If

    Set el = pkg.Elements.AddNew(elementName, elementType)
    If stereotype <> "" Then
        el.Stereotype = stereotype
    End If
    el.Update
    pkg.Elements.Refresh

    Set CreateOrGetElement = el
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

Function ConnectorExistsByName(sourceElem, targetElem, connType, connName, stereo)
    Dim i
    Dim c

    ConnectorExistsByName = False

    For i = 0 To sourceElem.Connectors.Count - 1
        Set c = sourceElem.Connectors.GetAt(i)
        If LCase(c.Type) = LCase(connType) And c.SupplierID = targetElem.ElementID Then
            If c.Name = connName And LCase(c.Stereotype) = LCase(stereo) Then
                ConnectorExistsByName = True
                Exit Function
            End If
        End If
    Next
End Function

Sub AddAssociationIfMissing(sourceElem, targetElem, label)
    Dim conn

    If Not ConnectorExistsByName(sourceElem, targetElem, "Association", label, "") Then
        Set conn = sourceElem.Connectors.AddNew(label, "Association")
        conn.SupplierID = targetElem.ElementID
        conn.Direction = "Source -> Destination"
        conn.Update
        sourceElem.Connectors.Refresh
    End If
End Sub

Sub AddDependencyIfMissing(sourceElem, targetElem, label)
    Dim conn

    If Not ConnectorExistsByName(sourceElem, targetElem, "Dependency", label, "") Then
        Set conn = sourceElem.Connectors.AddNew(label, "Dependency")
        conn.SupplierID = targetElem.ElementID
        conn.Direction = "Source -> Destination"
        conn.Update
        sourceElem.Connectors.Refresh
    End If
End Sub

' =========================================================
' 3.3.1.1 DISENO FISICO - DEPLOYMENT
' =========================================================

Sub BuildDeploymentDiagram(sectionPkg)

    Dim dgm

    ' --- Dispositivo movil ---
    Dim mobileDevice
    Dim mobileOS
    Dim mobileApp

    ' --- Computadora ---
    Dim pcDevice
    Dim pcOS
    Dim browserApp

    ' --- Router / Internet ---
    Dim router

    ' --- Infraestructura AWS (contenedor grande) ---
    Dim awsInfra

    ' --- AWS CloudFront ---
    Dim cloudFront

    ' --- AWS S3 Frontend Web ---
    Dim s3Frontend
    Dim webApp

    ' --- AWS ECR ---
    Dim ecrRegistry
    Dim dockerImage

    ' --- AWS App Runner ---
    Dim appRunner
    Dim dockerEnv
    Dim springBootApp

    ' --- AWS RDS PostgreSQL ---
    Dim rdsPostgres
    Dim postgresEnv
    Dim siDb

    ' --- AWS S3 Evidencias ---
    Dim s3Evidencias
    Dim archivosApp

    Set dgm = CreateOrGetDiagram(sectionPkg, "deployment Diagrama de Despliegue - Sprint 1", "Deployment")
    ClearDiagramObjects dgm

    ' =====================================================
    ' DISPOSITIVO MOVIL
    ' =====================================================
    Set mobileDevice = CreateOrGetElement(sectionPkg, "Dispositivo movil",        "Device",              "device")
    Set mobileOS     = CreateOrGetElement(sectionPkg, "Sistema operativo movil",  "ExecutionEnvironment","executionEnvironment")
    Set mobileApp    = CreateOrGetElement(sectionPkg, "App Movil Flutter",         "Artifact",            "artifact")

    AddElementToDiagram dgm, mobileDevice,  40,  40,  380, 300
    AddElementToDiagram dgm, mobileOS,     100,  95,  330, 250
    AddElementToDiagram dgm, mobileApp,    155, 145,  295, 220

    ' =====================================================
    ' COMPUTADORA
    ' =====================================================
    Set pcDevice   = CreateOrGetElement(sectionPkg, "Computadora",       "Device",              "device")
    Set pcOS       = CreateOrGetElement(sectionPkg, "Sistema operativo", "ExecutionEnvironment","executionEnvironment")
    Set browserApp = CreateOrGetElement(sectionPkg, "Navegador Web",     "Artifact",            "artifact")

    AddElementToDiagram dgm, pcDevice,    40, 340, 380, 620
    AddElementToDiagram dgm, pcOS,       100, 395, 330, 555
    AddElementToDiagram dgm, browserApp, 155, 445, 295, 525

    ' =====================================================
    ' ROUTER / INTERNET
    ' =====================================================
    Set router = CreateOrGetElement(sectionPkg, "Router / Internet", "Device", "device")
    AddElementToDiagram dgm, router, 440, 220, 720, 440

    ' =====================================================
    ' INFRAESTRUCTURA AWS
    ' =====================================================
    Set awsInfra = CreateOrGetElement(sectionPkg, "Infraestructura AWS", "Device", "device")
    AddElementToDiagram dgm, awsInfra, 780, 40, 2380, 1020

    ' =====================================================
    ' AWS CLOUDFRONT
    ' =====================================================
    Set cloudFront = CreateOrGetElement(sectionPkg, "AWS CloudFront", "Node", "")
    AddElementToDiagram dgm, cloudFront, 860, 80, 1160, 260

    ' =====================================================
    ' AWS S3 FRONTEND WEB
    ' =====================================================
    Set s3Frontend = CreateOrGetElement(sectionPkg, "AWS S3 Frontend Web", "Node",    "")
    Set webApp     = CreateOrGetElement(sectionPkg, "Aplicacion Web Angular", "Artifact","artifact")

    AddElementToDiagram dgm, s3Frontend, 820,  300, 1200, 640
    AddElementToDiagram dgm, webApp,     890,  395, 1140, 575

    ' =====================================================
    ' AWS ECR
    ' =====================================================
    Set ecrRegistry  = CreateOrGetElement(sectionPkg, "AWS ECR",          "Node",    "")
    Set dockerImage  = CreateOrGetElement(sectionPkg, "Imagen Docker API","Artifact","artifact")

    AddElementToDiagram dgm, ecrRegistry, 1240,  60, 1540, 340
    AddElementToDiagram dgm, dockerImage, 1290, 140, 1490, 300

    ' =====================================================
    ' AWS APP RUNNER
    ' =====================================================
    Set appRunner     = CreateOrGetElement(sectionPkg, "AWS App Runner",                   "Node",               "")
    Set dockerEnv     = CreateOrGetElement(sectionPkg, "Contenedor Docker",                "ExecutionEnvironment","executionEnvironment")
    Set springBootApp = CreateOrGetElement(sectionPkg, "Backend API REST Spring Boot 3.5", "Artifact",           "artifact")

    AddElementToDiagram dgm, appRunner,     1240,  380, 1660, 840
    AddElementToDiagram dgm, dockerEnv,     1300,  460, 1600, 780
    AddElementToDiagram dgm, springBootApp, 1340,  560, 1570, 700

    ' =====================================================
    ' AWS RDS POSTGRESQL
    ' =====================================================
    Set rdsPostgres = CreateOrGetElement(sectionPkg, "AWS RDS PostgreSQL", "Node",               "")
    Set postgresEnv = CreateOrGetElement(sectionPkg, "Motor PostgreSQL",   "ExecutionEnvironment","executionEnvironment")
    Set siDb        = CreateOrGetElement(sectionPkg, "si2_g2_db",          "Artifact",           "artifact")

    AddElementToDiagram dgm, rdsPostgres, 1720,  380, 2060, 840
    AddElementToDiagram dgm, postgresEnv, 1780,  460, 2000, 780
    AddElementToDiagram dgm, siDb,        1810,  565, 1970, 680

    ' =====================================================
    ' AWS S3 EVIDENCIAS
    ' =====================================================
    Set s3Evidencias = CreateOrGetElement(sectionPkg, "AWS S3 Evidencias",    "Node",    "")
    Set archivosApp  = CreateOrGetElement(sectionPkg, "Archivos y Evidencias","Artifact","artifact")

    AddElementToDiagram dgm, s3Evidencias, 2120,  60, 2340, 600
    AddElementToDiagram dgm, archivosApp,  2150, 140, 2310, 360

    ' =====================================================
    ' CONEXIONES
    ' =====================================================
    AddAssociationIfMissing mobileApp,     router,        "HTTPS / REST"
    AddAssociationIfMissing browserApp,    router,        "HTTPS"
    AddAssociationIfMissing router,        cloudFront,    "HTTPS"
    AddAssociationIfMissing cloudFront,    webApp,        "CDN"
    AddAssociationIfMissing webApp,        springBootApp, "HTTPS / SDK"
    AddAssociationIfMissing mobileApp,     springBootApp, "HTTPS / REST"
    AddAssociationIfMissing ecrRegistry,   appRunner,     "pull imagen"
    AddAssociationIfMissing springBootApp, siDb,          "TCP/IP"
    AddAssociationIfMissing springBootApp, archivosApp,   "AWS SDK S3"

    dgm.Update
    Repository.ReloadDiagram dgm.DiagramID

End Sub

' =========================================================
' 3.3.1.2 DISENO LOGICO - CAPAS - SPRINT 1
' =========================================================

Sub BuildLogicalLayeredDiagram_Sprint1(sectionPkg)

    Dim dgm

    Dim pSeg, pInst, pEstr, pPers, pOper, pAud
    Dim pControllers, pServices, pRepositories
    Dim pSpringBoot, pPostgres
    Dim pRun, pDB

    Set pSeg  = CreateOrGetPackage(sectionPkg, "Seguridad y Acceso")
    Set pInst = CreateOrGetPackage(sectionPkg, "Gestion Institucional")
    Set pEstr = CreateOrGetPackage(sectionPkg, "Estructura Academica")
    Set pPers = CreateOrGetPackage(sectionPkg, "Personas Academicas")
    Set pOper = CreateOrGetPackage(sectionPkg, "Operacion Academica")
    Set pAud  = CreateOrGetPackage(sectionPkg, "Auditoria")

    Set pControllers  = CreateOrGetPackage(sectionPkg, "Controllers (REST)")
    Set pServices     = CreateOrGetPackage(sectionPkg, "Services (Logica de negocio)")
    Set pRepositories = CreateOrGetPackage(sectionPkg, "Repositories (JPA)")

    Set pSpringBoot = CreateOrGetPackage(sectionPkg, "Spring Boot 3.5.0")
    Set pPostgres   = CreateOrGetPackage(sectionPkg, "PostgreSQL 16")

    Set pRun = CreateOrGetPackage(sectionPkg, "JVM 23 - localhost:2026")
    Set pDB  = CreateOrGetPackage(sectionPkg, "localhost:5432 - si2_g2_db")

    Set dgm = CreateOrGetDiagram(sectionPkg, "pkg Diagrama Logico en Capas - Sprint 1", "Package")
    ClearDiagramObjects dgm

    ' Fila 1: modulos del dominio
    AddPackageToDiagram dgm, pSeg,  40,   60, 290,  200
    AddPackageToDiagram dgm, pInst, 350,  60, 640,  200
    AddPackageToDiagram dgm, pEstr, 700,  60, 1050, 200
    AddPackageToDiagram dgm, pPers, 1110, 60, 1420, 200
    AddPackageToDiagram dgm, pOper, 1480, 60, 1760, 200
    AddPackageToDiagram dgm, pAud,  1820, 60, 2080, 200

    ' Fila 2: capas spring
    AddPackageToDiagram dgm, pControllers,  200, 320, 620,  460
    AddPackageToDiagram dgm, pServices,     800, 320, 1280, 460
    AddPackageToDiagram dgm, pRepositories, 1440, 320, 1900, 460

    ' Fila 3: runtimea
    AddPackageToDiagram dgm, pSpringBoot, 500, 580, 900,  720
    AddPackageToDiagram dgm, pPostgres,   1100, 580, 1480, 720

    ' Fila 4: plataforma
    AddPackageToDiagram dgm, pRun, 400,  840, 900,  980
    AddPackageToDiagram dgm, pDB,  1060, 840, 1600, 980

    ' Dependencias dominio -> capas
    AddDependencyIfMissing pSeg.Element,  pControllers.Element, ""
    AddDependencyIfMissing pInst.Element, pControllers.Element, ""
    AddDependencyIfMissing pEstr.Element, pControllers.Element, ""
    AddDependencyIfMissing pPers.Element, pControllers.Element, ""
    AddDependencyIfMissing pOper.Element, pControllers.Element, ""
    AddDependencyIfMissing pAud.Element,  pControllers.Element, ""

    AddDependencyIfMissing pControllers.Element,  pServices.Element, ""
    AddDependencyIfMissing pServices.Element,      pRepositories.Element, ""

    AddDependencyIfMissing pControllers.Element,  pSpringBoot.Element, ""
    AddDependencyIfMissing pRepositories.Element, pPostgres.Element, ""

    AddDependencyIfMissing pSpringBoot.Element, pRun.Element, ""
    AddDependencyIfMissing pPostgres.Element,   pDB.Element, ""

    dgm.Update
    Repository.ReloadDiagram dgm.DiagramID

End Sub

Main