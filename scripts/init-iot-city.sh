#!/bin/bash

# IoT City Initialization Script
# Compatible with bash 3.x (macOS default)
#
# This script initializes the IoT City application by creating actors in the correct order:
# 1. Capteurs (9 sensors - 3 per city) - created first
# 2. Villes (3 cities) 
# 3. Associate capteurs to cities via RegisterCapteur messages
# 4. Clients (3 clients - created WITHOUT city assignment)
# 5. Clients enter cities via RegisterClient messages

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

# Variables to store IDs
TEMP_PARIS_ID=""
HUM_PARIS_ID=""
PRES_PARIS_ID=""
TEMP_LYON_ID=""
HUM_LYON_ID=""
PRES_LYON_ID=""
TEMP_MARSEILLE_ID=""
HUM_MARSEILLE_ID=""
PRES_MARSEILLE_ID=""
PARIS_ID=""
LYON_ID=""
MARSEILLE_ID=""
ALICE_ID=""
BOB_ID=""
CHARLIE_ID=""

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

# Function to create an actor via distributed API and return the ID
# Logs go to stderr, ID goes to stdout
create_actor() {
    local service_id=$1
    local actor_type=$2
    local actor_name=$3
    local params=$4
    
    echo "Creating $actor_type: $actor_name" >&2
    
    response=$(curl -s -w "\n%{http_code}" -X POST \
        -H "Content-Type: application/json" \
        -H "X-API-Key: $API_KEY" \
        -d "{\"serviceId\":\"$service_id\",\"actorType\":\"$actor_type\",\"params\":$params}" \
        "$SAF_CONTROL_URL/api/v1/actors")
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" -eq 200 ] || [ "$http_code" -eq 201 ]; then
        actor_id=$(echo "$body" | grep -o '"actorId":"[^"]*"' | head -1 | sed 's/"actorId":"//;s/"//')
        echo "  ✓ Created successfully (ID: $actor_id)" >&2
        echo "$actor_id"
        return 0
    else
        echo "  ✗ Failed (HTTP $http_code): $body" >&2
        return 1
    fi
}

# Function to send a message to an actor (tell pattern)
send_message() {
    local actor_id=$1
    local message_type=$2
    local message_payload=$3
    
    echo "Sending $message_type to $actor_id"
    
    # Generate a unique message ID and timestamp
    local msg_id=$(uuidgen 2>/dev/null || echo "msg-$(date +%s)-$$")
    local timestamp=$(date -u +"%Y-%m-%dT%H:%M:%S.000Z")
    
    # Build the full TellActorCommand with SimpleMessage format
    # The @class field is required for Jackson polymorphic deserialization
    local json_body="{\"targetActorId\":\"$actor_id\",\"senderActorId\":\"system\",\"message\":{\"@class\":\"com.acme.saf.actor.core.SimpleMessage\",\"messageId\":\"$msg_id\",\"timestamp\":\"$timestamp\",\"correlationId\":null,\"payload\":$message_payload}}"
    
    response=$(curl -s -w "\n%{http_code}" -X POST \
        -H "Content-Type: application/json" \
        -H "X-API-Key: $API_KEY" \
        -d "$json_body" \
        "$SAF_CONTROL_URL/api/v1/actors/$actor_id/tell")
    
    http_code=$(echo "$response" | tail -n1)
    
    if [ "$http_code" -eq 200 ]; then
        echo "  ✓ Message sent successfully"
        return 0
    else
        body=$(echo "$response" | sed '$d')
        echo "  ✗ Failed (HTTP $http_code): $body"
        return 1
    fi
}

# Step 1: Wait for all services to be healthy
echo "Step 1: Checking service health..."
echo "-----------------------------------"
wait_for_service "SAF Control" "$SAF_CONTROL_URL/actuator/health" || exit 1
wait_for_service "Client Service" "http://localhost:8084/actuator/health" || exit 1
wait_for_service "Ville Service" "http://localhost:8085/actuator/health" || exit 1
wait_for_service "Capteur Service" "http://localhost:8086/actuator/health" || exit 1
echo ""

echo "Waiting 5 seconds for services to fully register..."
sleep 5
echo ""

# Step 2: Create Capteurs (9 sensors)
echo "Step 2: Creating capteur actors (9 sensors)..."
echo "-----------------------------------"

TEMP_PARIS_ID=$(create_actor "capteur-service" "CapteurActor" "temp-paris" '{"sensorType":"TEMPERATURE","location":"Tour Eiffel","minValue":-10.0,"maxValue":45.0,"unit":"C"}')
HUM_PARIS_ID=$(create_actor "capteur-service" "CapteurActor" "hum-paris" '{"sensorType":"HUMIDITY","location":"Louvre","minValue":20.0,"maxValue":95.0,"unit":"%"}')
PRES_PARIS_ID=$(create_actor "capteur-service" "CapteurActor" "pres-paris" '{"sensorType":"PRESSURE","location":"Notre-Dame","minValue":970.0,"maxValue":1040.0,"unit":"hPa"}')

TEMP_LYON_ID=$(create_actor "capteur-service" "CapteurActor" "temp-lyon" '{"sensorType":"TEMPERATURE","location":"Fourviere","minValue":-10.0,"maxValue":42.0,"unit":"C"}')
HUM_LYON_ID=$(create_actor "capteur-service" "CapteurActor" "hum-lyon" '{"sensorType":"HUMIDITY","location":"Place Bellecour","minValue":25.0,"maxValue":90.0,"unit":"%"}')
PRES_LYON_ID=$(create_actor "capteur-service" "CapteurActor" "pres-lyon" '{"sensorType":"PRESSURE","location":"Parc Tete dOr","minValue":975.0,"maxValue":1035.0,"unit":"hPa"}')

TEMP_MARSEILLE_ID=$(create_actor "capteur-service" "CapteurActor" "temp-marseille" '{"sensorType":"TEMPERATURE","location":"Vieux-Port","minValue":-5.0,"maxValue":48.0,"unit":"C"}')
HUM_MARSEILLE_ID=$(create_actor "capteur-service" "CapteurActor" "hum-marseille" '{"sensorType":"HUMIDITY","location":"Notre-Dame de la Garde","minValue":20.0,"maxValue":98.0,"unit":"%"}')
PRES_MARSEILLE_ID=$(create_actor "capteur-service" "CapteurActor" "pres-marseille" '{"sensorType":"PRESSURE","location":"Calanques","minValue":980.0,"maxValue":1030.0,"unit":"hPa"}')

echo ""
echo "Created 9 capteurs successfully"
echo ""

# Step 3: Create Villes (3 cities)
echo "Step 3: Creating ville actors (3 cities)..."
echo "-----------------------------------"

PARIS_ID=$(create_actor "ville-service" "VilleActor" "paris" '{"nom":"Paris","population":2161000,"superficie":105.4}')
LYON_ID=$(create_actor "ville-service" "VilleActor" "lyon" '{"nom":"Lyon","population":516092,"superficie":47.87}')
MARSEILLE_ID=$(create_actor "ville-service" "VilleActor" "marseille" '{"nom":"Marseille","population":869815,"superficie":240.62}')

echo ""
echo "Created 3 villes successfully"
echo "  Paris ID: $PARIS_ID"
echo "  Lyon ID: $LYON_ID"
echo "  Marseille ID: $MARSEILLE_ID"
echo ""

sleep 3
echo ""

# Step 4: Associate Capteurs to Villes
echo "Step 4: Associating capteurs to villes (RegisterCapteur)..."
echo "-----------------------------------"

send_message "$PARIS_ID" "RegisterCapteur" "{\"capteurId\":\"$TEMP_PARIS_ID\",\"capteurType\":\"TEMPERATURE\",\"kafkaTopic\":\"capteur-temperature-paris\",\"location\":\"Tour Eiffel\"}"
send_message "$PARIS_ID" "RegisterCapteur" "{\"capteurId\":\"$HUM_PARIS_ID\",\"capteurType\":\"HUMIDITY\",\"kafkaTopic\":\"capteur-humidity-paris\",\"location\":\"Louvre\"}"
send_message "$PARIS_ID" "RegisterCapteur" "{\"capteurId\":\"$PRES_PARIS_ID\",\"capteurType\":\"PRESSURE\",\"kafkaTopic\":\"capteur-pressure-paris\",\"location\":\"Notre-Dame\"}"

send_message "$LYON_ID" "RegisterCapteur" "{\"capteurId\":\"$TEMP_LYON_ID\",\"capteurType\":\"TEMPERATURE\",\"kafkaTopic\":\"capteur-temperature-lyon\",\"location\":\"Fourviere\"}"
send_message "$LYON_ID" "RegisterCapteur" "{\"capteurId\":\"$HUM_LYON_ID\",\"capteurType\":\"HUMIDITY\",\"kafkaTopic\":\"capteur-humidity-lyon\",\"location\":\"Place Bellecour\"}"
send_message "$LYON_ID" "RegisterCapteur" "{\"capteurId\":\"$PRES_LYON_ID\",\"capteurType\":\"PRESSURE\",\"kafkaTopic\":\"capteur-pressure-lyon\",\"location\":\"Parc Tete dOr\"}"

send_message "$MARSEILLE_ID" "RegisterCapteur" "{\"capteurId\":\"$TEMP_MARSEILLE_ID\",\"capteurType\":\"TEMPERATURE\",\"kafkaTopic\":\"capteur-temperature-marseille\",\"location\":\"Vieux-Port\"}"
send_message "$MARSEILLE_ID" "RegisterCapteur" "{\"capteurId\":\"$HUM_MARSEILLE_ID\",\"capteurType\":\"HUMIDITY\",\"kafkaTopic\":\"capteur-humidity-marseille\",\"location\":\"Notre-Dame de la Garde\"}"
send_message "$MARSEILLE_ID" "RegisterCapteur" "{\"capteurId\":\"$PRES_MARSEILLE_ID\",\"capteurType\":\"PRESSURE\",\"kafkaTopic\":\"capteur-pressure-marseille\",\"location\":\"Calanques\"}"

echo ""
echo "Associated 9 capteurs to 3 villes"
echo ""

# Step 5: Create Clients (WITHOUT city assignment)
echo "Step 5: Creating client actors (3 clients - unassigned)..."
echo "-----------------------------------"

ALICE_ID=$(create_actor "client-service" "ClientActor" "alice" '{"clientName":"Alice"}')
BOB_ID=$(create_actor "client-service" "ClientActor" "bob" '{"clientName":"Bob"}')
CHARLIE_ID=$(create_actor "client-service" "ClientActor" "charlie" '{"clientName":"Charlie"}')

echo ""
echo "Created 3 clients (not assigned to any city yet)"
echo "  Alice ID: $ALICE_ID"
echo "  Bob ID: $BOB_ID"
echo "  Charlie ID: $CHARLIE_ID"
echo ""

sleep 2
echo ""

# Step 6: Clients enter cities (RegisterClient)
echo "Step 6: Clients entering cities (RegisterClient)..."
echo "-----------------------------------"

send_message "$PARIS_ID" "RegisterClient" "{\"messageType\":\"RegisterClient\",\"clientId\":\"$ALICE_ID\"}"
echo "  Alice entered Paris"

send_message "$LYON_ID" "RegisterClient" "{\"messageType\":\"RegisterClient\",\"clientId\":\"$BOB_ID\"}"
echo "  Bob entered Lyon"

send_message "$MARSEILLE_ID" "RegisterClient" "{\"messageType\":\"RegisterClient\",\"clientId\":\"$CHARLIE_ID\"}"
echo "  Charlie entered Marseille"

echo ""
echo "All clients have entered their cities"
echo ""

echo "======================================"
echo "✓ Initialization completed successfully!"
echo "======================================"
echo ""
echo "Summary:"
echo "  - 9 capteurs created (3 per city: TEMPERATURE, HUMIDITY, PRESSURE)"
echo "  - 3 villes created (Paris, Lyon, Marseille)"
echo "  - 9 capteur-ville associations (RegisterCapteur messages)"
echo "  - 3 clients created (Alice, Bob, Charlie - initially unassigned)"
echo "  - 3 clients entered cities (RegisterClient messages)"
echo ""
echo "Current State:"
echo "  Paris:"
echo "    - Capteurs: temp-paris, hum-paris, pres-paris"
echo "    - Client: Alice"
echo "  Lyon:"
echo "    - Capteurs: temp-lyon, hum-lyon, pres-lyon"
echo "    - Client: Bob"
echo "  Marseille:"
echo "    - Capteurs: temp-marseille, hum-marseille, pres-marseille"
echo "    - Client: Charlie"
echo ""
echo "You can now access the dashboard at http://localhost:80"
echo ""
