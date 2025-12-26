# Test: Lister les acteurs à travers les microservices
# Avant: /agents (control plane) + /runtime/create-actor (pour créer)
# Nouveau: /runtime/actors (pour lister)

Write-Host "=== TEST ANNUAIRE DES ACTEURS ===" -ForegroundColor Green

[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

Write-Host "`n1️⃣ CONTROL PLANE (GET /agents sur saf-control:8080)" -ForegroundColor Cyan
$headers = @{'X-API-KEY' = 'test'}
try {
    $response = Invoke-WebRequest -Uri 'http://localhost:8080/agents' `
        -Headers $headers `
        -Method Get `
        -UseBasicParsing `
        -ErrorAction Stop
    
    $agents = $response.Content | ConvertFrom-Json
    Write-Host "✅ Agents du control plane:" -ForegroundColor Green
    $agents | Format-Table -AutoSize id, type, state, status, policy
    Write-Host "Total: $($agents.Count) agents"
} catch {
    Write-Host "❌ Erreur: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n2️⃣ CAPTEUR-SERVICE (GET /runtime/actors sur :8086)" -ForegroundColor Cyan
try {
    $response = Invoke-WebRequest -Uri 'http://localhost:8086/runtime/actors' `
        -Method Get `
        -UseBasicParsing `
        -ErrorAction Stop
    
    $actors = $response.Content | ConvertFrom-Json
    Write-Host "✅ Acteurs sur capteur-service:" -ForegroundColor Green
    if ($actors -is [array]) {
        foreach ($actor in $actors) { Write-Host "  • $actor" }
        Write-Host "Total: $($actors.Count) acteurs"
    } else {
        Write-Host "  • $actors"
    }
} catch {
    Write-Host "❌ Erreur: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
}

Write-Host "`n3️⃣ VILLE-SERVICE (GET /runtime/actors sur :8085)" -ForegroundColor Cyan
try {
    $response = Invoke-WebRequest -Uri 'http://localhost:8085/runtime/actors' `
        -Method Get `
        -UseBasicParsing `
        -ErrorAction Stop
    
    $actors = $response.Content | ConvertFrom-Json
    Write-Host "✅ Acteurs sur ville-service:" -ForegroundColor Green
    if ($actors -is [array]) {
        foreach ($actor in $actors) { Write-Host "  • $actor" }
        Write-Host "Total: $($actors.Count) acteurs"
    } else {
        Write-Host "  • $actors"
    }
} catch {
    Write-Host "❌ Erreur: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
}

Write-Host "`n4️⃣ CLIENT-SERVICE (GET /runtime/actors sur :8084)" -ForegroundColor Cyan
try {
    $response = Invoke-WebRequest -Uri 'http://localhost:8084/runtime/actors' `
        -Method Get `
        -UseBasicParsing `
        -ErrorAction Stop
    
    $actors = $response.Content | ConvertFrom-Json
    Write-Host "✅ Acteurs sur client-service:" -ForegroundColor Green
    if ($actors -is [array]) {
        foreach ($actor in $actors) { Write-Host "  • $actor" }
        Write-Host "Total: $($actors.Count) acteurs"
    } else {
        Write-Host "  • $actors"
    }
} catch {
    Write-Host "❌ Erreur: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
}

Write-Host "`n=== RÉSUMÉ ===" -ForegroundColor Yellow
Write-Host "✅ GET /agents - Control plane agents" -ForegroundColor Green
Write-Host "✅ GET /runtime/actors - Acteurs par microservice (NEW)" -ForegroundColor Green
Write-Host "ℹ️  Maintenant tu peux avoir une vue d'ensemble complète" -ForegroundColor Cyan
