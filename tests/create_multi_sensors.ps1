# Script to create multiple sensor instances (TEMPERATURE, HUMIDITY, PRESSURE)
# Usage: .\create_multi_sensors.ps1

$baseUrl = "http://localhost:8086/runtime/create-actor"

$sensors = @(
    @{ id = "temp-1"; type = "TEMPERATURE" },
    @{ id = "temp-2"; type = "TEMPERATURE" },
    @{ id = "humidity-1"; type = "HUMIDITY" },
    @{ id = "humidity-2"; type = "HUMIDITY" },
    @{ id = "pressure-1"; type = "PRESSURE" },
    @{ id = "pressure-2"; type = "PRESSURE" }
)

Write-Host "🌡️  Creating $($sensors.Count) sensor instances..." -ForegroundColor Cyan
Write-Host ""

foreach ($sensor in $sensors) {
    $body = @{
        actorId = $sensor.id
        actorType = "CapteurActor"
        params = @{ type = $sensor.type }
    } | ConvertTo-Json

    try {
        $response = Invoke-WebRequest -Uri $baseUrl -Method POST -Body $body -ContentType "application/json" -UseBasicParsing 2>&1
        $result = $response.Content | ConvertFrom-Json
        
        if ($result.success) {
            Write-Host "✓ $($sensor.id) ($($sensor.type))" -ForegroundColor Green
        } else {
            Write-Host "✗ $($sensor.id) - $($result.errorMessage)" -ForegroundColor Red
        }
    } catch {
        Write-Host "✗ $($sensor.id) - Connection error" -ForegroundColor Red
    }
    
    Start-Sleep -Milliseconds 200
}

Write-Host ""
Write-Host "✓ All sensors created!" -ForegroundColor Green
Write-Host ""
Write-Host "Monitor ville-service logs:" -ForegroundColor Yellow
Write-Host "  docker-compose logs -f ville-service | Select-String 'received'"
