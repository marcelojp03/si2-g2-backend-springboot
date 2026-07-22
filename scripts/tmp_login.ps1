$body = @{ correo = 'superadmin@example.com'; contrasena = 'change_me_super_admin' } | ConvertTo-Json
try {
    $r = Invoke-RestMethod -Uri 'http://localhost:2026/api/auth/login' -Method POST -ContentType 'application/json' -Body $body
    $r | ConvertTo-Json -Depth 4
} catch {
    $resp = $_.Exception.Response
    if ($resp) {
        $reader = New-Object System.IO.StreamReader($resp.GetResponseStream())
        $content = $reader.ReadToEnd()
        Write-Host "ERROR_RESPONSE:`n$content"
    } else {
        Write-Host "ERROR: $_"
    }
}
