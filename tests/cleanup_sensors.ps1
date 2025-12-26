# Script to clean up and reset sensors/Kafka
# Usage: .\cleanup_sensors.ps1

Write-Host "🧹 Sensor Cleanup Script" -ForegroundColor Cyan
Write-Host "========================" -ForegroundColor Cyan
Write-Host ""

$choice = Read-Host "Select option:
1) Clear Kafka topic (reset messages)
2) Stop all Docker containers
3) Full reset (containers + Kafka)
4) Cancel

Enter choice (1-4)"

switch ($choice) {
    "1" {
        Write-Host "🗑️  Clearing Kafka topic..." -ForegroundColor Yellow
        docker-compose exec -T kafka kafka-topics --delete --topic iot-city-sensor-readings --bootstrap-server localhost:9092
        Write-Host "✓ Kafka topic cleared!" -ForegroundColor Green
    }
    "2" {
        Write-Host "⏹️  Stopping containers..." -ForegroundColor Yellow
        docker-compose stop capteur-service ville-service saf-control
        Write-Host "✓ Containers stopped!" -ForegroundColor Green
        Write-Host "ℹ️  Restart with: docker-compose start" -ForegroundColor Cyan
    }
    "3" {
        Write-Host "🔄 Full reset..." -ForegroundColor Yellow
        docker-compose down
        docker-compose up -d
        Write-Host "✓ Full reset complete!" -ForegroundColor Green
        Write-Host "⏳ Waiting 20s for services to start..." -ForegroundColor Cyan
        sleep 20
        Write-Host "✓ Services ready!" -ForegroundColor Green
    }
    default {
        Write-Host "Cancelled" -ForegroundColor Yellow
    }
}
