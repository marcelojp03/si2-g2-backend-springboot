param(
    [Parameter(Mandatory = $true)]
    [string]$ApiBase,

    [Parameter(Mandatory = $true)]
    [string]$SuperAdminEmail,

    [Parameter(Mandatory = $true)]
    [SecureString]$SuperAdminPassword,

    [Parameter(Mandatory = $true)]
    [string]$DirectorEmail,

    [Parameter(Mandatory = $true)]
    [SecureString]$DirectorPassword,

    [string]$InstitutionCode = "CSM-001",

    [switch]$ConfirmDemoData
)

$ErrorActionPreference = "Stop"
$ApiBase = $ApiBase.TrimEnd('/')

if (-not $ConfirmDemoData) {
    throw "Este script genera datos academicos demo. Vuelva a ejecutarlo con -ConfirmDemoData."
}

function Login([string]$Email, [SecureString]$Password) {
    $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($Password)
    try {
        $plainPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
        $body = @{ correo = $Email; contrasena = $plainPassword } | ConvertTo-Json
        $response = Invoke-RestMethod -Method Post -Uri "$ApiBase/api/auth/login" `
            -ContentType "application/json" -Body $body
        if ($response.codigo -ne 200 -or -not $response.data.token) {
            throw "No se pudo iniciar sesion con $Email"
        }
        return $response.data.token
    } finally {
        $plainPassword = $null
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    }
}

Write-Host "Generando datos academicos de riesgo para $InstitutionCode..."
$superToken = Login $SuperAdminEmail $SuperAdminPassword
$superHeaders = @{ Authorization = "Bearer $superToken" }
$seed = Invoke-RestMethod -Method Post `
    -Uri "$ApiBase/api/seed/academic-risk?institutionCode=$([uri]::EscapeDataString($InstitutionCode))" `
    -Headers $superHeaders
if ($seed.codigo -ne 200) {
    throw "El seed academico de riesgo no finalizo correctamente"
}

Write-Host "Ejecutando primer analisis institucional..."
$directorToken = Login $DirectorEmail $DirectorPassword
$directorHeaders = @{ Authorization = "Bearer $directorToken" }
$gestiones = Invoke-RestMethod -Method Get -Uri "$ApiBase/api/gestiones" -Headers $directorHeaders
$gestionActiva = @($gestiones.data) | Where-Object { $_.activa } | Select-Object -First 1
if (-not $gestionActiva) {
    throw "La institucion no tiene una gestion academica activa"
}

$analysisBody = @{ idGestion = $gestionActiva.id } | ConvertTo-Json
$analysis = Invoke-RestMethod -Method Post -Uri "$ApiBase/api/alertas-riesgo/analizar/institucion" `
    -Headers $directorHeaders -ContentType "application/json" -Body $analysisBody
if ($analysis.codigo -ne 200) {
    throw "El analisis institucional no finalizo correctamente"
}

Write-Host "Post-deploy completado correctamente."
Write-Host "Estudiantes analizados: $($analysis.data.totalEstudiantes)"
Write-Host "Paralelos analizados: $(@($analysis.data.comparativaParalelos).Count)"
