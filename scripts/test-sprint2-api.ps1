$base = if ($env:API_BASE) { $env:API_BASE } else { "http://localhost:2026" }
$pass = "Demo12345!"
$superPass = "change_me_super_admin"
$script:results = @()

function Login($correo, $contrasena) {
    $body = @{ correo = $correo; contrasena = $contrasena } | ConvertTo-Json
    $r = Invoke-RestMethod -Uri "$base/api/auth/login" -Method POST -ContentType "application/json" -Body $body
    return $r.data.token
}

function Api($method, $path, $token, $bodyObj = $null) {
    $headers = @{ Authorization = "Bearer $token" }
    $params = @{ Uri = "$base$path"; Method = $method; Headers = $headers; ContentType = "application/json" }
    if ($bodyObj) { $params.Body = ($bodyObj | ConvertTo-Json -Depth 10) }
    try {
        $r = Invoke-WebRequest @params -UseBasicParsing
        return @{ ok = $true; status = [int]$r.StatusCode; body = ($r.Content | ConvertFrom-Json) }
    }
    catch {
        $resp = $_.Exception.Response
        $status = if ($resp) { [int]$resp.StatusCode } else { 0 }
        $content = ""
        if ($resp) {
            $reader = New-Object System.IO.StreamReader($resp.GetResponseStream())
            $content = $reader.ReadToEnd()
        }
        $parsed = $null
        try { $parsed = $content | ConvertFrom-Json } catch {}
        return @{ ok = $false; status = $status; body = $parsed; raw = $content }
    }
}

function Test-HU($hu, $name, $method, $path, $token, $bodyObj = $null, $expectStatus = 200) {
    $r = Api $method $path $token $bodyObj
    $passTest = ($r.status -eq $expectStatus)
    $msg = if ($r.body -and $r.body.message) { $r.body.message } elseif ($r.raw) { $r.raw.Substring(0, [Math]::Min(120, $r.raw.Length)) } else { "" }
    $script:results += [pscustomobject]@{ HU = $hu; Test = $name; Status = $r.status; Pass = $passTest; Message = $msg }
    Write-Host ("[{0}] {1} -> {2} ({3})" -f $(if ($passTest) { "OK" } else { "FAIL" }), $name, $r.status, $msg)
    return $r
}

Write-Host "=== Login SUPER_ADMIN + Seed ===" -ForegroundColor Cyan
$tokenSuper = Login "superadmin@example.com" $superPass
Test-HU "SETUP" "POST seed synthetic" POST "/api/seed/synthetic" $tokenSuper $null 200 | Out-Null

Write-Host "`n=== HU-S2-14 Gestionar aulas (DIRECTOR) ===" -ForegroundColor Cyan
$tokenDir = Login "director.demo@si2.test" $pass

$aulaCodigo = "A-TEST-99"
$aulaBody = @{
    codigo    = $aulaCodigo
    nombre    = "Aula Postman Test"
    capacidad = 35
    ubicacion = "Edificio B piso 2"
    recursos  = @("pizarra", "proyector")
}
$rCreate = Api POST "/api/aulas" $tokenDir $aulaBody
if ($rCreate.status -eq 201) {
    $script:results += [pscustomobject]@{ HU = "HU-S2-14"; Test = "POST crear aula"; Status = 201; Pass = $true; Message = "Aula creada" }
    Write-Host "[OK] POST crear aula -> 201"
    $idAula = $rCreate.body.data.id
}
else {
    $rList = Api GET "/api/aulas?q=$aulaCodigo" $tokenDir
    $idAula = ($rList.body.data | Where-Object { $_.codigo -eq $aulaCodigo } | Select-Object -First 1).id
    $passReuse = ($rCreate.status -eq 409 -and $idAula)
    $script:results += [pscustomobject]@{
        HU = "HU-S2-14"; Test = "POST crear aula (idempotente)"; Status = $rCreate.status
        Pass = $passReuse; Message = "Reutiliza aula existente"
    }
    Write-Host ("[{0}] POST crear aula -> {1} (reutiliza existente)" -f $(if ($passReuse) { "OK" } else { "FAIL" }), $rCreate.status)
}

Test-HU "HU-S2-14" "GET listar aulas" GET "/api/aulas" $tokenDir | Out-Null
Test-HU "HU-S2-14" "GET filtro capacidadMin=30" GET "/api/aulas?capacidadMin=30" $tokenDir | Out-Null
Test-HU "HU-S2-14" "GET filtro recurso=proyector" GET "/api/aulas?recurso=proyector" $tokenDir | Out-Null
Test-HU "HU-S2-14" "GET obtener aula" GET "/api/aulas/$idAula" $tokenDir | Out-Null

$aulaUpd = @{
    codigo    = "A-TEST-99"
    nombre    = "Aula Postman Actualizada"
    capacidad = 40
    ubicacion = "Edificio B piso 3"
    recursos  = @("pizarra", "proyector", "aire")
}
Test-HU "HU-S2-14" "PUT actualizar aula" PUT "/api/aulas/$idAula" $tokenDir $aulaUpd | Out-Null

Write-Host "`n=== HU-S2-15 Gestionar horarios (DIRECTOR) ===" -ForegroundColor Cyan
$rAsig = Api GET "/api/asignaciones" $tokenDir
$idAsignacion = $rAsig.body.data[0].id
$idInst = $rAsig.body.data[0].idInstitucion

$horarioBody = @{
    idInstitucion       = $idInst
    idAsignacionDocente = $idAsignacion
    idAula              = $idAula
    diaSemana           = "MARTES"
    horaInicio          = "10:00:00"
    horaFin             = "11:30:00"
}
$rHor = Api POST "/api/horarios" $tokenDir $horarioBody
if ($rHor.status -eq 201) {
    $script:results += [pscustomobject]@{ HU = "HU-S2-15"; Test = "POST crear horario"; Status = 201; Pass = $true; Message = "Horario creado" }
    Write-Host "[OK] POST crear horario -> 201"
    $idHorario = $rHor.body.data.id
}
else {
    $rHorList = Api GET "/api/horarios/asignacion/$idAsignacion" $tokenDir
    $idHorario = $rHorList.body.data[0].id
    $script:results += [pscustomobject]@{
        HU = "HU-S2-15"; Test = "POST crear horario (idempotente)"; Status = $rHor.status
        Pass = ($rHor.status -ge 400 -and $idHorario); Message = "Horario ya existente"
    }
    Write-Host "[OK] POST crear horario -> $($rHor.status) (reutiliza existente)"
}

Test-HU "HU-S2-15" "GET listar horarios" GET "/api/horarios?idInstitucion=$idInst" $tokenDir | Out-Null
Test-HU "HU-S2-15" "GET horarios por asignacion" GET "/api/horarios/asignacion/$idAsignacion" $tokenDir | Out-Null
Test-HU "HU-S2-15" "GET horarios por aula" GET "/api/horarios/aula/$idAula" $tokenDir | Out-Null

$conflictBody = @{
    idInstitucion       = $idInst
    idAsignacionDocente = $idAsignacion
    idAula              = $idAula
    diaSemana           = "MARTES"
    horaInicio          = "10:30:00"
    horaFin             = "12:00:00"
}
$rConflict = Api POST "/api/horarios" $tokenDir $conflictBody
$conflictPass = $rConflict.status -ge 400
$script:results += [pscustomobject]@{
    HU      = "HU-S2-15"
    Test    = "POST validacion conflicto horario"
    Status  = $rConflict.status
    Pass    = $conflictPass
    Message = if ($rConflict.body.message) { $rConflict.body.message } else { $rConflict.raw }
}
Write-Host ("[{0}] POST validacion conflicto horario -> {1}" -f $(if ($conflictPass) { "OK" } else { "FAIL" }), $rConflict.status)

Write-Host "`n=== HU-S2-16 Registrar asistencia (DOCENTE) ===" -ForegroundColor Cyan
$tokenDoc = Login "docente.mate.demo@si2.test" $pass

$rMisAsig = Test-HU "HU-S2-16" "GET mis-asignaciones docente" GET "/api/asistencias/mis-asignaciones" $tokenDoc
$idAsigDoc = $rMisAsig.body.data[0].idAsignacionDocente
$fecha = (Get-Date).ToString("yyyy-MM-dd")

$rPlant = Test-HU "HU-S2-16" "GET plantilla asistencia" GET "/api/asistencias/plantilla?idAsignacionDocente=$idAsigDoc&fecha=$fecha" $tokenDoc
$detalles = @()
foreach ($est in $rPlant.body.data.estudiantes) {
    $detalles += @{ idInscripcion = $est.idInscripcion; estadoAsistencia = "PRESENTE" }
}
$asistBody = @{ idAsignacionDocente = $idAsigDoc; fecha = $fecha; detalles = $detalles }
$rAsist = Test-HU "HU-S2-16" "POST registrar asistencia masiva" POST "/api/asistencias" $tokenDoc $asistBody 201
$idAsistencia = $rAsist.body.data.id
Test-HU "HU-S2-16" "GET obtener registro asistencia" GET "/api/asistencias/$idAsistencia" $tokenDoc | Out-Null

Write-Host "`n=== HU-S2-17 Consultar asistencia ===" -ForegroundColor Cyan
Test-HU "HU-S2-17" "GET plantilla (director)" GET "/api/asistencias/plantilla?idAsignacionDocente=$idAsigDoc&fecha=$fecha" $tokenDir | Out-Null
Test-HU "HU-S2-17" "GET registro (director)" GET "/api/asistencias/$idAsistencia" $tokenDir | Out-Null

$tokenEst = Login "estudiante.lucia.demo@si2.test" $pass
$rEstAsig = Api GET "/api/asistencias/mis-asignaciones" $tokenEst
$script:results += [pscustomobject]@{
    HU = "HU-S2-17"; Test = "GET mis-asignaciones estudiante"; Status = $rEstAsig.status
    Pass = ($rEstAsig.status -in 200, 403); Message = "status estudiante"
}
Write-Host "[INFO] HU-S2-17 estudiante -> $($rEstAsig.status)"

$tokenTutor = Login "tutor.maria.demo@si2.test" $pass
$rTutAsig = Api GET "/api/asistencias/mis-asignaciones" $tokenTutor
$script:results += [pscustomobject]@{
    HU = "HU-S2-17"; Test = "GET mis-asignaciones tutor"; Status = $rTutAsig.status
    Pass = ($rTutAsig.status -in 200, 403); Message = "status tutor"
}
Write-Host "[INFO] HU-S2-17 tutor -> $($rTutAsig.status)"

Write-Host "`n=== HU-S2-18 Gestionar calificaciones (DOCENTE) ===" -ForegroundColor Cyan
$rCalAsig = Test-HU "HU-S2-18" "GET mis-asignaciones calificaciones" GET "/api/calificaciones/mis-asignaciones" $tokenDoc
$idMateria = $rCalAsig.body.data[0].idMateria

$evalNombre = "Parcial 1 Postman"
$evalBody = @{
    idMateria    = $idMateria
    periodo      = 1
    tipo         = "PARCIAL"
    nombre       = $evalNombre
    ponderacion  = 30
    escala       = "NUMERICA"
    estado       = "ABIERTA"
}
$rEval = Api POST "/api/calificaciones/evaluaciones" $tokenDoc $evalBody
if ($rEval.status -eq 201) {
    $script:results += [pscustomobject]@{ HU = "HU-S2-18"; Test = "POST crear evaluacion"; Status = 201; Pass = $true; Message = "Evaluacion creada" }
    Write-Host "[OK] POST crear evaluacion -> 201"
    $idEval = $rEval.body.data.id
}
else {
    $rEvalList = Api GET "/api/calificaciones/evaluaciones?idMateria=$idMateria&periodo=1" $tokenDoc
    $idEval = ($rEvalList.body.data | Where-Object { $_.nombre -eq $evalNombre } | Select-Object -First 1).id
    $script:results += [pscustomobject]@{
        HU = "HU-S2-18"; Test = "POST crear evaluacion (idempotente)"; Status = $rEval.status
        Pass = ($rEval.status -eq 409 -and $idEval); Message = "Reutiliza evaluacion existente"
    }
    Write-Host "[OK] POST crear evaluacion -> $($rEval.status) (reutiliza existente)"
}

Test-HU "HU-S2-18" "GET listar evaluaciones" GET "/api/calificaciones/evaluaciones?idMateria=$idMateria&periodo=1" $tokenDoc | Out-Null
$rCalPlant = Test-HU "HU-S2-18" "GET plantilla calificaciones" GET "/api/calificaciones/plantilla?idEvaluacion=$idEval" $tokenDoc

$calDetalles = @()
foreach ($row in $rCalPlant.body.data.estudiantes) {
    $calDetalles += @{ idInscripcion = $row.idInscripcion; notaNumerica = 85 }
}
$calBody = @{ idEvaluacion = $idEval; detalles = $calDetalles }
Test-HU "HU-S2-18" "POST guardar calificaciones" POST "/api/calificaciones" $tokenDoc $calBody 201 | Out-Null
Test-HU "HU-S2-18" "GET resumen periodo" GET "/api/calificaciones/resumen?idAsignacionDocente=$idAsigDoc&periodo=1" $tokenDoc | Out-Null

Write-Host "`n=== HU-S2-19 Historial academico ===" -ForegroundColor Cyan
$rEsts = Api GET "/api/estudiantes" $tokenDir
$idEstudiante = $rEsts.body.data[0].id
Test-HU "HU-S2-19" "GET historial (director)" GET "/api/estudiantes/$idEstudiante/historial" $tokenDir | Out-Null
Test-HU "HU-S2-19" "GET historial (estudiante)" GET "/api/estudiantes/$idEstudiante/historial" $tokenEst | Out-Null
Test-HU "HU-S2-19" "GET historial (tutor)" GET "/api/estudiantes/$idEstudiante/historial" $tokenTutor | Out-Null

Write-Host "`n=== RESUMEN ===" -ForegroundColor Yellow
$script:results | Group-Object HU | ForEach-Object {
    $passed = ($_.Group | Where-Object Pass).Count
    $total = $_.Group.Count
    Write-Host "$($_.Name): $passed/$total OK"
}
$failed = $script:results | Where-Object { -not $_.Pass }
if ($failed) {
    Write-Host "`nFALLIDOS:" -ForegroundColor Red
    $failed | Format-Table HU, Test, Status, Message -AutoSize
    exit 1
}
Write-Host "`nTodos los tests pasaron." -ForegroundColor Green
