# Script para subir imagen a ECR.
# Uso: .\deploy-to-ecr.ps1

param(
    [string]$ImageTag = "latest"
)

Write-Host "Verificando Docker..." -ForegroundColor Cyan

$dockerRunning = $false
try {
    docker ps *> $null
    if ($LASTEXITCODE -eq 0) {
        $dockerRunning = $true
    }
} catch {
}

if (-not $dockerRunning) {
    Write-Host "Docker Desktop no esta corriendo." -ForegroundColor Yellow
    Write-Host "Iniciando Docker Desktop..." -ForegroundColor Cyan
    Start-Process "Docker Desktop" -WindowStyle Hidden

    Write-Host "Esperando a que Docker inicie..." -ForegroundColor Yellow
    $timeoutSeconds = 60
    $elapsedSeconds = 0

    while (-not $dockerRunning -and $elapsedSeconds -lt $timeoutSeconds) {
        Start-Sleep -Seconds 5
        $elapsedSeconds += 5
        try {
            docker ps *> $null
            if ($LASTEXITCODE -eq 0) {
                $dockerRunning = $true
                break
            }
        } catch {
        }
        Write-Host "." -NoNewline -ForegroundColor Gray
    }

    Write-Host ""

    if (-not $dockerRunning) {
        Write-Host "Docker no pudo iniciarse automaticamente." -ForegroundColor Red
        Write-Host "Abre Docker Desktop manualmente y vuelve a ejecutar el script." -ForegroundColor Yellow
        exit 1
    }

    Write-Host "Docker Desktop iniciado correctamente." -ForegroundColor Green
}

$awsRegion = "us-east-1"
$awsAccountId = "851725478821"
$ecrRepoName = "si2-sia-springboot"
$ecrUri = "$awsAccountId.dkr.ecr.$awsRegion.amazonaws.com/$ecrRepoName"

$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
Write-Host "Directorio del proyecto: $projectRoot" -ForegroundColor Gray

Write-Host ""
Write-Host "Construyendo imagen Docker..." -ForegroundColor Cyan
docker build -t "${ecrRepoName}:${ImageTag}" "$projectRoot"
if ($LASTEXITCODE -ne 0) {
    Write-Host "Error construyendo imagen." -ForegroundColor Red
    exit 1
}

Write-Host "Autenticando con ECR..." -ForegroundColor Cyan
$loginPassword = aws ecr get-login-password --region $awsRegion
$loginPassword | docker login --username AWS --password-stdin $ecrUri
if ($LASTEXITCODE -ne 0) {
    Write-Host "Error autenticando con ECR." -ForegroundColor Red
    exit 1
}

Write-Host "Etiquetando imagen..." -ForegroundColor Cyan
docker tag "${ecrRepoName}:${ImageTag}" "${ecrUri}:${ImageTag}"
if ($LASTEXITCODE -ne 0) {
    Write-Host "Error etiquetando imagen." -ForegroundColor Red
    exit 1
}

Write-Host "Subiendo imagen a ECR..." -ForegroundColor Cyan
docker push "${ecrUri}:${ImageTag}"
if ($LASTEXITCODE -ne 0) {
    Write-Host "Error subiendo imagen." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Imagen subida exitosamente." -ForegroundColor Green
Write-Host "ECR URI: ${ecrUri}:${ImageTag}" -ForegroundColor Cyan