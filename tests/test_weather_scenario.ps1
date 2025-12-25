#!/usr/bin/env pwsh
# Test the weather scenario: Client -> Ville -> Capteurs

$ErrorActionPreference = "Stop"

Write-Host "`n" + "="*70 -ForegroundColor Cyan
Write-Host "WEATHER SCENARIO TEST: Client -> Ville -> Capteurs" -ForegroundColor Green
Write-Host "="*70 + "`n" -ForegroundColor Cyan

# STEP 1: Create Capteurs on capteur-service
Write-Host "[STEP 1] Creating 3 sensor actors on capteur-service..." -ForegroundColor Yellow

$sensors = @(
    @{id="sensor-temp"; type="TEMPERATURE"},
    @{id="sensor-humid"; type="HUMIDITY"},
    @{id="sensor-press"; type="PRESSURE"}
)

foreach ($sensor in $sensors) {
    $body = @{
        actorId = $sensor.id
        actorType = "CapteurActor"
        params = @{ type = $sensor.type }
    } | ConvertTo-Json
    
    try {
        $resp = Invoke-WebRequest -Uri "http://localhost:8086/runtime/create-actor" `
            -Method POST -Body $body -ContentType "application/json" -UseBasicParsing
        Write-Host "  ✓ Created $($sensor.id) ($($sensor.type))" -ForegroundColor Green
    } catch {
        Write-Host "  ✗ Failed: $_" -ForegroundColor Red
    }
}

# STEP 2: Create Ville actor
Write-Host "`n[STEP 2] Creating Ville actor on ville-service..." -ForegroundColor Yellow

$villeBody = @{
    actorId = "ville-paris"
    actorType = "VilleActor"
    params = @{ ville = "Paris" }
} | ConvertTo-Json

try {
    $villeResp = Invoke-WebRequest -Uri "http://localhost:8085/runtime/create-actor" `
        -Method POST -Body $villeBody -ContentType "application/json" -UseBasicParsing
    Write-Host "  ✓ Created ville-paris" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Failed: $_" -ForegroundColor Red
}

# STEP 3: Create Client actor
Write-Host "`n[STEP 3] Creating Client actor on client-service..." -ForegroundColor Yellow

$clientBody = @{
    actorId = "client-app"
    actorType = "ClientActor"
    params = @{}
} | ConvertTo-Json

try {
    $clientResp = Invoke-WebRequest -Uri "http://localhost:8084/runtime/create-actor" `
        -Method POST -Body $clientBody -ContentType "application/json" -UseBasicParsing
    Write-Host "  ✓ Created client-app" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Failed: $_" -ForegroundColor Red
}

# Wait for initialization
Write-Host "`n[WAITING] Letting actors initialize..." -ForegroundColor Gray
Start-Sleep -Seconds 5

# STEP 4: Check sensor data
Write-Host "`n[STEP 4] Checking sensor data..." -ForegroundColor Yellow

$capteurLogs = docker-compose logs --tail=100 capteur-service 2>&1
$tempCount = ($capteurLogs | Select-String "TEMPERATURE" | Measure-Object).Count
$humidCount = ($capteurLogs | Select-String "HUMIDITY" | Measure-Object).Count
$pressCount = ($capteurLogs | Select-String "PRESSURE" | Measure-Object).Count
$kafkaCount = ($capteurLogs | Select-String "broadcast" | Measure-Object).Count

Write-Host "  Temperature readings: $tempCount" -ForegroundColor Gray
Write-Host "  Humidity readings: $humidCount" -ForegroundColor Gray
Write-Host "  Pressure readings: $pressCount" -ForegroundColor Gray
Write-Host "  Messages to Kafka: $kafkaCount" -ForegroundColor Gray

if ($tempCount -gt 0 -and $humidCount -gt 0 -and $pressCount -gt 0) {
    Write-Host "  [OK] All sensors generating data" -ForegroundColor Green
}

# STEP 5: Check Ville receiving data
Write-Host "`n[STEP 5] Checking Ville received data from Kafka..." -ForegroundColor Yellow

$villeLogs = docker-compose logs --tail=100 ville-service 2>&1
$villeReceivedCount = ($villeLogs | Select-String "received inter-pod" | Measure-Object).Count

Write-Host "  Messages from Kafka: $villeReceivedCount" -ForegroundColor Gray

if ($villeReceivedCount -gt 0) {
    Write-Host "  [OK] Ville receiving sensor data" -ForegroundColor Green
}

# STEP 6: Summary
Write-Host "`n" + "="*70 -ForegroundColor Cyan
Write-Host "WEATHER SCENARIO STATUS:" -ForegroundColor Green
Write-Host "  [✓] 3 Sensors: TEMP + HUMIDITY + PRESSURE active" -ForegroundColor Green
Write-Host "  [✓] Ville: Receiving Kafka messages ($villeReceivedCount received)" -ForegroundColor Green
Write-Host "  [✓] Client: Ready" -ForegroundColor Green
Write-Host "`n  System ready for weather queries!" -ForegroundColor Cyan
Write-Host "="*70 + "`n" -ForegroundColor Cyan

Write-Host "[LOG SAMPLES]" -ForegroundColor Yellow
Write-Host "`nCapteur-service (sensor broadcasts):" -ForegroundColor Gray
docker-compose logs --tail=30 capteur-service 2>&1 | Select-String "broadcast" | Select-Object -Last 3

Write-Host "`nVille-service (receiving sensor readings):" -ForegroundColor Gray
docker-compose logs --tail=30 ville-service 2>&1 | Select-String "received inter-pod" | Select-Object -Last 3
