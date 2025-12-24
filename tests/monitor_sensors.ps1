# Script to monitor sensor data flow through Kafka
# Usage: .\monitor_sensors.ps1

Write-Host "🔍 Sensor Monitoring Dashboard" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Choose monitoring mode:" -ForegroundColor Yellow
Write-Host "1) Watch VilleActor receiving messages"
Write-Host "2) Watch CapteurActor sending messages"
Write-Host "3) Watch Kafka topic (raw messages)"
Write-Host "4) All three in separate windows"
Write-Host ""

$choice = Read-Host "Enter choice (1-4)"

switch ($choice) {
    "1" {
        Write-Host "📍 VilleActor receiving sensor readings..." -ForegroundColor Green
        docker-compose logs -f ville-service | Select-String "Paris received"
    }
    "2" {
        Write-Host "📤 CapteurActor broadcasting readings..." -ForegroundColor Green
        docker-compose logs -f capteur-service | Select-String "broadcast|SCHEDULER"
    }
    "3" {
        Write-Host "📊 Raw Kafka messages..." -ForegroundColor Green
        docker-compose exec kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic iot-city-sensor-readings --from-beginning
    }
    "4" {
        Write-Host "🚀 Opening 3 monitoring windows..." -ForegroundColor Green
        
        # Window 1: VilleActor
        Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd 'c:\Users\flofl\Desktop\saf_platform'; docker-compose logs -f ville-service | Select-String 'Paris received'"
        
        # Window 2: CapteurActor
        Start-Sleep -Milliseconds 500
        Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd 'c:\Users\flofl\Desktop\saf_platform'; docker-compose logs -f capteur-service | Select-String 'broadcast|SCHEDULER'"
        
        # Window 3: Kafka topic
        Start-Sleep -Milliseconds 500
        Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd 'c:\Users\flofl\Desktop\saf_platform'; docker-compose exec kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic iot-city-sensor-readings --from-beginning"
        
        Write-Host "✓ Monitoring windows opened!" -ForegroundColor Green
    }
    default {
        Write-Host "Invalid choice" -ForegroundColor Red
    }
}
