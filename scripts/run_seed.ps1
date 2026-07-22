$base = if ($env:API_BASE) { $env:API_BASE } else { "http://localhost:2026" }
$super = @{ correo = 'superadmin@example.com'; contrasena = 'change_me_super_admin' } | ConvertTo-Json
try {
    $login = Invoke-RestMethod -Uri "$base/api/auth/login" -Method POST -ContentType 'application/json' -Body $super
    $token = $login.data.token
    Write-Host "Login OK, token length: $($token.Length)"
    $headers = @{ Authorization = "Bearer $token" }
    $resp = Invoke-RestMethod -Uri "$base/api/seed/synthetic" -Method POST -Headers $headers -ContentType 'application/json'
    Write-Host "Seed response:" -ForegroundColor Green
    $resp | ConvertTo-Json -Depth 6
} catch {
    $respErr = $_.Exception.Response
    if ($respErr) {
        $reader = New-Object System.IO.StreamReader($respErr.GetResponseStream())
        $content = $reader.ReadToEnd()
        Write-Host "ERROR_RESPONSE:`n$content" -ForegroundColor Red
    } else {
        Write-Host "ERROR: $_" -ForegroundColor Red
    }
    exit 1
}
