#!/usr/bin/env pwsh
# Test the Agent Directory (Annuaire des agents)

Write-Host "`n" + "="*80 -ForegroundColor Cyan
Write-Host "AGENT DIRECTORY TEST - Annuaire des Agents" -ForegroundColor Green
Write-Host "="*80 + "`n" -ForegroundColor Cyan

# Verify the actors we created earlier are in the directory
Write-Host "[TEST 1] Query all agents in the directory" -ForegroundColor Yellow
Write-Host "Endpoint: GET /api/v1/actors" -ForegroundColor Gray

try {
    $allAgents = Invoke-WebRequest -Uri "http://localhost:8080/api/v1/actors" `
        -Method GET -UseBasicParsing
    
    $agents = $allAgents.Content | ConvertFrom-Json
    
    if ($agents -and $agents.Count -gt 0) {
        Write-Host "Status: 200 OK" -ForegroundColor Green
        Write-Host "Total agents in directory: $($agents.Count)" -ForegroundColor Cyan
        Write-Host ""
        
        Write-Host "Agents found:" -ForegroundColor Yellow
        foreach ($agent in $agents) {
            Write-Host "  - $($agent.actorId)" -ForegroundColor Green
            Write-Host "    Type: $($agent.actorType)" -ForegroundColor Gray
            Write-Host "    Service: $($agent.serviceId)" -ForegroundColor Gray
            Write-Host "    URL: $($agent.serviceUrl)" -ForegroundColor Gray
            Write-Host ""
        }
    } else {
        Write-Host "Status: 200 OK but no agents found" -ForegroundColor Yellow
    }
} catch {
    Write-Host "ERROR: $_" -ForegroundColor Red
}

# Test 2: Query specific agent
Write-Host "[TEST 2] Query specific agent from directory" -ForegroundColor Yellow
Write-Host "Endpoint: GET /api/v1/actors/{actorId}" -ForegroundColor Gray

$testActorId = "sensor-temperature"
try {
    $agentResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/v1/actors/$testActorId" `
        -Method GET -UseBasicParsing
    
    $agent = $agentResponse.Content | ConvertFrom-Json
    Write-Host "Status: 200 OK" -ForegroundColor Green
    Write-Host "Agent found: $($agent.actorId)" -ForegroundColor Cyan
    Write-Host "  Type: $($agent.actorType)" -ForegroundColor Gray
    Write-Host "  Service: $($agent.serviceId)" -ForegroundColor Gray
    Write-Host "  URL: $($agent.serviceUrl)" -ForegroundColor Gray
} catch {
    if ($_.Exception.Response.StatusCode -eq 404) {
        Write-Host "Status: 404 NOT FOUND (agent not in directory)" -ForegroundColor Yellow
    } else {
        Write-Host "ERROR: $_" -ForegroundColor Red
    }
}

# Test 3: Query agents by service
Write-Host "`n[TEST 3] Query agents by service" -ForegroundColor Yellow
Write-Host "Endpoint: GET /api/v1/actors/by-service/{serviceId}" -ForegroundColor Gray

$services = @("capteur-service", "ville-service", "client-service")

foreach ($serviceId in $services) {
    try {
        $serviceAgents = Invoke-WebRequest -Uri "http://localhost:8080/api/v1/actors/by-service/$serviceId" `
            -Method GET -UseBasicParsing
        
        $agents = $serviceAgents.Content | ConvertFrom-Json
        $count = if ($agents -is [array]) { $agents.Count } else { if ($agents) { 1 } else { 0 } }
        
        Write-Host "Service: $serviceId" -ForegroundColor Yellow
        Write-Host "  Agents: $count" -ForegroundColor Cyan
        
        if ($agents) {
            if ($agents -is [array]) {
                foreach ($a in $agents) {
                    Write-Host "    - $($a.actorId)" -ForegroundColor Green
                }
            } else {
                Write-Host "    - $($agents.actorId)" -ForegroundColor Green
            }
        }
        Write-Host ""
    } catch {
        Write-Host "Service: $serviceId - ERROR: $_" -ForegroundColor Yellow
    }
}

Write-Host "="*80 -ForegroundColor Cyan
Write-Host "DIRECTORY TEST COMPLETE" -ForegroundColor Green
Write-Host "="*80 + "`n" -ForegroundColor Cyan
