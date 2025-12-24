#!/bin/bash

# IoT City Initialization Script
# This script initializes the IoT City application by creating cities and their associated actors
# Run this after all services are up and healthy

set -e  # Exit on error

# Configuration
SAF_CONTROL_URL="${SAF_CONTROL_URL:-http://localhost:8080}"
API_KEY="${SAF_API_KEY:-test}"
RETRY_COUNT=30
RETRY_DELAY=2

echo "======================================"
echo "IoT City Initialization Script"
echo "======================================"
echo "SAF Control URL: $SAF_CONTROL_URL"
echo ""

# Function to wait for service health
wait_for_service() {
    local service_name=$1
    local health_url=$2
    local count=0
    
    echo "Waiting for $service_name to be healthy..."
    while [ $count -lt $RETRY_COUNT ]; do
        if curl -sf "$health_url" > /dev/null 2>&1; then
            echo "✓ $service_name is healthy"
            return 0
        fi
        count=$((count + 1))
        echo "  Attempt $count/$RETRY_COUNT - waiting ${RETRY_DELAY}s..."
        sleep $RETRY_DELAY
    done
    
    echo "✗ ERROR: $service_name did not become healthy"
    return 1
}

# Function to create an actor via distributed API
create_actor() {
    local service_id=$1
    local actor_type=$2
    local actor_id=$3
    local params=$4
    
    echo "Creating $actor_type: $actor_id"
    
    response=$(curl -s -w "\n%{http_code}" -X POST \
        -H "Content-Type: application/json" \
        -H "X-API-Key: $API_KEY" \
        -d "{\"serviceId\":\"$service_id\",\"actorType\":\"$actor_type\",\"params\":$params}" \
        "$SAF_CONTROL_URL/api/v1/actors")
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" -eq 200 ] || [ "$http_code" -eq 201 ]; then
        echo "  ✓ Created successfully"
        return 0
    else
        echo "  ✗ Failed (HTTP $http_code): $body"
        return 1
    fi
}

# Wait for all services to be healthy
echo "Step 1: Checking service health..."
echo "-----------------------------------"
wait_for_service "SAF Control" "$SAF_CONTROL_URL/actuator/health" || exit 1
wait_for_service "Client Service" "http://localhost:8084/actuator/health" || exit 1
wait_for_service "Ville Service" "http://localhost:8085/actuator/health" || exit 1
wait_for_service "Capteur Service" "http://localhost:8086/actuator/health" || exit 1
echo ""

# Give services a bit more time to fully register
echo "Waiting 5 seconds for services to fully register..."
sleep 5
echo ""

# Initialize cities
echo "Step 2: Creating cities..."
echo "-----------------------------------"

# Paris
create_actor "ville-service" "VilleActor" "paris" '{
  "nom": "Paris",
  "population": 2161000,
  "superficie": 105.4,
  "climateConfig": {
    "tempMin": -5.0,
    "tempMax": 35.0,
    "humidityMin": 30.0,
    "humidityMax": 90.0,
    "pressureMin": 980.0,
    "pressureMax": 1030.0
  }
}'

# Lyon  
create_actor "ville-service" "VilleActor" "lyon" '{
  "nom": "Lyon",
  "population": 516092,
  "superficie": 47.87,
  "climateConfig": {
    "tempMin": -5.0,
    "tempMax": 35.0,
    "humidityMin": 30.0,
    "humidityMax": 90.0,
    "pressureMin": 980.0,
    "pressureMax": 1030.0
  }
}'

# Marseille
create_actor "ville-service" "VilleActor" "marseille" '{
  "nom": "Marseille",
  "population": 869815,
  "superficie": 240.62,
  "climateConfig": {
    "tempMin": 0.0,
    "tempMax": 40.0,
    "humidityMin": 25.0,
    "humidityMax": 95.0,
    "pressureMin": 985.0,
    "pressureMax": 1025.0
  }
}'

echo ""

# Create clients for each city
echo "Step 3: Creating client actors..."
echo "-----------------------------------"

# Clients for Paris
create_actor "client-service" "ClientActor" "client-paris-1" '{"cityId":"paris","clientName":"Client Paris 1"}'
create_actor "client-service" "ClientActor" "client-paris-2" '{"cityId":"paris","clientName":"Client Paris 2"}'

# Clients for Lyon
create_actor "client-service" "ClientActor" "client-lyon-1" '{"cityId":"lyon","clientName":"Client Lyon 1"}'
create_actor "client-service" "ClientActor" "client-lyon-2" '{"cityId":"lyon","clientName":"Client Lyon 2"}'

# Clients for Marseille
create_actor "client-service" "ClientActor" "client-marseille-1" '{"cityId":"marseille","clientName":"Client Marseille 1"}'
create_actor "client-service" "ClientActor" "client-marseille-2" '{"cityId":"marseille","clientName":"Client Marseille 2"}'

echo ""

# Create sensors for each city
echo "Step 4: Creating sensor actors..."
echo "-----------------------------------"

# Sensors for Paris
create_actor "capteur-service" "CapteurActor" "temp-paris-1" '{"cityId":"paris","sensorType":"TEMPERATURE","location":"Eiffel Tower"}'
create_actor "capteur-service" "CapteurActor" "hum-paris-1" '{"cityId":"paris","sensorType":"HUMIDITY","location":"Louvre"}'
create_actor "capteur-service" "CapteurActor" "pres-paris-1" '{"cityId":"paris","sensorType":"PRESSURE","location":"Notre-Dame"}'

# Sensors for Lyon
create_actor "capteur-service" "CapteurActor" "temp-lyon-1" '{"cityId":"lyon","sensorType":"TEMPERATURE","location":"Fourvière"}'
create_actor "capteur-service" "CapteurActor" "hum-lyon-1" '{"cityId":"lyon","sensorType":"HUMIDITY","location":"Place Bellecour"}'
create_actor "capteur-service" "CapteurActor" "pres-lyon-1" '{"cityId":"lyon","sensorType":"PRESSURE","location":"Parc de la Tête d Or"}'

# Sensors for Marseille  
create_actor "capteur-service" "CapteurActor" "temp-marseille-1" '{"cityId":"marseille","sensorType":"TEMPERATURE","location":"Vieux-Port"}'
create_actor "capteur-service" "CapteurActor" "hum-marseille-1" '{"cityId":"marseille","sensorType":"HUMIDITY","location":"Notre-Dame de la Garde"}'
create_actor "capteur-service" "CapteurActor" "pres-marseille-1" '{"cityId":"marseille","sensorType":"PRESSURE","location":"Calanques"}'

echo ""
echo "======================================"
echo "✓ Initialization completed successfully!"
echo "======================================"
echo ""
echo "Summary:"
echo "  - 3 cities created (Paris, Lyon, Marseille)"
echo "  - 6 client actors created (2 per city)"
echo "  - 9 sensor actors created (3 per city)"
echo ""
echo "You can now access the dashboard at http://localhost:80"
echo ""
