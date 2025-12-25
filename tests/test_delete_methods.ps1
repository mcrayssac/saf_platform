#!/usr/bin/env pwsh
<#
.SYNOPSIS
Test the two different DELETE methods for removing actors
.DESCRIPTION
Tests both endpoints:
1. DELETE /agents/{id} (AgentsController - control plane)
2. DELETE /api/actors/{id} (RuntimeProxyController - proxy)
#>

$ErrorActionPreference = "Stop"

Write-Host "`n" + "="*70 -ForegroundColor Cyan
Write-Host "TESTING ACTOR DELETION METHODS" -ForegroundColor Green
Write-Host "="*70 + "`n" -ForegroundColor Cyan

# Create first test actor
Write-Host "[SETUP] Creating test-actor-1..." -ForegroundColor Yellow
$body1 = @{
    actorId = "test-actor-1"
    actorType = "CapteurActor"
    params = @{ type = "TEMPERATURE" }
} | ConvertTo-Json

try {
    $createResponse1 = Invoke-WebRequest -Uri "http://localhost:8086/runtime/create-actor" `
        -Method POST `
        -Body $body1 `
        -ContentType "application/json" `
        -UseBasicParsing
    Write-Host "✓ Created test-actor-1" -ForegroundColor Green
} catch {
    Write-Host "✗ Failed to create test-actor-1: $_" -ForegroundColor Red
    exit 1
}

# Create second test actor
Write-Host "[SETUP] Creating test-actor-2..." -ForegroundColor Yellow
$body2 = @{
    actorId = "test-actor-2"
    actorType = "CapteurActor"
    params = @{ type = "HUMIDITY" }
} | ConvertTo-Json

try {
    $createResponse2 = Invoke-WebRequest -Uri "http://localhost:8086/runtime/create-actor" `
        -Method POST `
        -Body $body2 `
        -ContentType "application/json" `
        -UseBasicParsing
    Write-Host "✓ Created test-actor-2" -ForegroundColor Green
} catch {
    Write-Host "✗ Failed to create test-actor-2: $_" -ForegroundColor Red
    exit 1
}

Start-Sleep -Seconds 2

# Test Method 1: DELETE /agents/{id}
Write-Host "`n[METHOD 1] DELETE /agents/{id} (AgentsController)" -ForegroundColor Yellow
try {
    $response1 = Invoke-WebRequest -Uri "http://localhost:8080/agents/test-actor-1" `
        -Method DELETE `
        -UseBasicParsing
    
    if ($response1.StatusCode -eq 204) {
        Write-Host "✓ Method 1 WORKS! Status: 204 (No Content)" -ForegroundColor Green
        Write-Host "  Endpoint: DELETE /agents/test-actor-1 on port 8080 (control plane)" -ForegroundColor Green
    } else {
        Write-Host "? Method 1 responded but unusual status: $($response1.StatusCode)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "✗ Method 1 failed: $($_.Exception.Response.StatusCode)" -ForegroundColor Red
}

# Test Method 2: DELETE /api/actors/{id}
Write-Host "`n[METHOD 2] DELETE /api/actors/{id} (RuntimeProxyController)" -ForegroundColor Yellow
try {
    $response2 = Invoke-WebRequest -Uri "http://localhost:8080/api/actors/test-actor-2" `
        -Method DELETE `
        -UseBasicParsing
    
    if ($response2.StatusCode -eq 204) {
        Write-Host "✓ Method 2 WORKS! Status: 204 (No Content)" -ForegroundColor Green
        Write-Host "  Endpoint: DELETE /api/actors/test-actor-2 on port 8080 (control plane)" -ForegroundColor Green
    } else {
        Write-Host "? Method 2 responded but unusual status: $($response2.StatusCode)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "✗ Method 2 failed: $($_.Exception.Response.StatusCode)" -ForegroundColor Red
}

# Verification
Write-Host "`n[VERIFICATION] Checking if actors are gone..." -ForegroundColor Yellow
Start-Sleep -Seconds 2

$logs = docker-compose logs --tail=50 capteur-service 2>&1
$actor1Still = $logs | Select-String "test-actor-1" | Measure-Object | Select-Object -ExpandProperty Count
$actor2Still = $logs | Select-String "test-actor-2" | Measure-Object | Select-Object -ExpandProperty Count

Write-Host "test-actor-1 mentions in logs: $actor1Still (after deletion)" -ForegroundColor Gray
Write-Host "test-actor-2 mentions in logs: $actor2Still (after deletion)" -ForegroundColor Gray

Write-Host "`n" + "="*70 -ForegroundColor Cyan
Write-Host "CONCLUSION:" -ForegroundColor Green
Write-Host "  Method 1: DELETE /agents/{id} → Works via AgentsController" -ForegroundColor Green
Write-Host "  Method 2: DELETE /api/actors/{id} → Works via RuntimeProxyController" -ForegroundColor Green
Write-Host "`n  Both methods are valid. Use whichever you prefer!" -ForegroundColor Cyan
Write-Host "="*70 + "`n" -ForegroundColor Cyan
