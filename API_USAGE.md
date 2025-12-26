# SAF Platform - API Usage Guide

After running `docker compose up -d`, you can interact with the platform through the SAF Control API Gateway.

## Service Endpoints

- **SAF Control (API Gateway)**: http://localhost:8080
- **Client Service**: http://localhost:8084
- **Ville Service**: http://localhost:8085
- **Capteur Service**: http://localhost:8086
- **Kafka**: localhost:29092 (external), kafka:9092 (internal)
- **Zookeeper**: zookeeper:2181 (internal)
- **Frontend**: http://localhost:80
- **Prometheus**: http://localhost:9090

## Authentication

All API requests (except public endpoints) require an API key header:

```bash
X-API-KEY: test
```

The API key is configured in the `.env` file:
```
SAF_API_KEY=test
```

### Public Endpoints (No API Key Required)

- `GET /actuator/health` - Health check
- `GET /actuator/prometheus` - Prometheus metrics
- `GET /swagger` - Swagger UI documentation
- `GET /v3/api-docs` - OpenAPI specification

### Protected Endpoints (API Key Required)

All other endpoints require the `X-API-KEY` header.

---

## Distributed Actor System - IoT City

The platform uses a distributed architecture where each actor type runs in its own microservice:

- **client-service** (Port 8084): Manages ClientActors
- **ville-service** (Port 8085): Manages VilleActors
- **capteur-service** (Port 8086): Manages CapteurActors

### Architecture Flow

```
Frontend / API Clients
        ↓
SAF Control (Port 8080) - Orchestration & Discovery
        ↓
    ┌───┴───┬───────────┬────────────┐
    ↓       ↓           ↓            ↓
client  ville      capteur     [other services]
(8084)  (8085)     (8086)
```

---

## Service Registration & Discovery

### 1. List All Registered Services

```bash
curl -H "X-API-KEY: test" \
  http://localhost:8080/api/v1/services
```

**Expected Response:**
```json
[
  {
    "serviceId": "client-service",
    "serviceUrl": "http://client-service:8084",
    "lastHeartbeat": "2025-12-23T20:15:30.123Z",
    "healthy": true
  },
  {
    "serviceId": "ville-service",
    "serviceUrl": "http://ville-service:8085",
    "lastHeartbeat": "2025-12-23T20:15:31.456Z",
    "healthy": true
  },
  {
    "serviceId": "capteur-service",
    "serviceUrl": "http://capteur-service:8086",
    "lastHeartbeat": "2025-12-23T20:15:32.789Z",
    "healthy": true
  }
]
```

---

## Creating Distributed Actors

### 1. Create a ClientActor (in client-service)

```bash
curl -X POST http://localhost:8080/api/v1/actors \
  -H "X-API-KEY: test" \
  -H "Content-Type: application/json" \
  -d '{
    "serviceId": "client-service",
    "actorType": "ClientActor",
    "params": {
      "clientName": "Alice"
    }
  }'
```

**Expected Response:**
```json
{
  "actorId": "client-alice-abc123",
  "serviceId": "client-service",
  "actorType": "CLIENT",
  "status": "CREATED",
  "serviceUrl": "http://client-service:8084"
}
```

### 2. Create a VilleActor (in ville-service)

```bash
curl -X POST http://localhost:8080/api/v1/actors \
  -H "X-API-KEY: test" \
  -H "Content-Type: application/json" \
  -d '{
    "serviceId": "ville-service",
    "actorType": "VilleActor",
    "params": {
      "nom": "Paris",
      "population": 2161000,
      "superficie": 105.4
    }
  }'
```

**Expected Response:**
```json
{
  "actorId": "ville-paris-xyz789",
  "serviceId": "ville-service",
  "actorType": "VILLE",
  "status": "CREATED",
  "serviceUrl": "http://ville-service:8085"
}
```

### 3. Create a CapteurActor (in capteur-service)

```bash
curl -X POST http://localhost:8080/api/v1/actors \
  -H "X-API-KEY: test" \
  -H "Content-Type: application/json" \
  -d '{
    "serviceId": "capteur-service",
    "actorType": "CapteurActor",
    "params": {
      "sensorType": "TEMPERATURE",
      "location": "Tour Eiffel",
      "minValue": -10.0,
      "maxValue": 45.0,
      "unit": "C"
    }
  }'
```

**Expected Response:**
```json
{
  "actorId": "capteur-temp-paris-def456",
  "serviceId": "capteur-service",
  "actorType": "CAPTEUR",
  "status": "CREATED",
  "serviceUrl": "http://capteur-service:8086"
}
```

---

## Sending Messages to Actors

Messages are sent using the `/tell` endpoint with a `TellActorCommand` structure containing a `SimpleMessage`.

**Important:** The message format uses Jackson polymorphic typing with `@class` annotation.

### Message Format

```json
{
  "targetActorId": "<actor-id>",
  "senderActorId": "<sender-id or null>",
  "message": {
    "@class": "com.acme.saf.actor.core.SimpleMessage",
    "messageId": "<unique-uuid>",
    "timestamp": "<ISO-8601 datetime>",
    "correlationId": null,
    "payload": {
      // Message-specific data here
    }
  }
}
```

### 1. Register a Capteur to a Ville (RegisterCapteur)

```bash
curl -X POST http://localhost:8080/api/v1/actors/{villeActorId}/tell \
  -H "X-API-KEY: test" \
  -H "Content-Type: application/json" \
  -d '{
    "targetActorId": "ville-paris-xyz789",
    "senderActorId": "system",
    "message": {
      "@class": "com.acme.saf.actor.core.SimpleMessage",
      "messageId": "msg-001",
      "timestamp": "2025-12-26T10:00:00.000Z",
      "correlationId": null,
      "payload": {
        "capteurId": "capteur-temp-paris-def456",
        "capteurType": "TEMPERATURE",
        "kafkaTopic": "capteur-temperature-paris",
        "location": "Tour Eiffel"
      }
    }
  }'
```

### 2. Register a Client to a Ville (RegisterClient)

```bash
curl -X POST http://localhost:8080/api/v1/actors/{villeActorId}/tell \
  -H "X-API-KEY: test" \
  -H "Content-Type: application/json" \
  -d '{
    "targetActorId": "ville-paris-xyz789",
    "senderActorId": "system",
    "message": {
      "@class": "com.acme.saf.actor.core.SimpleMessage",
      "messageId": "msg-002",
      "timestamp": "2025-12-26T10:00:00.000Z",
      "correlationId": null,
      "payload": {
        "messageType": "RegisterClient",
        "clientId": "client-alice-abc123"
      }
    }
  }'
```

### 3. Unregister a Client from a Ville (UnregisterClient)

```bash
curl -X POST http://localhost:8080/api/v1/actors/{villeActorId}/tell \
  -H "X-API-KEY: test" \
  -H "Content-Type: application/json" \
  -d '{
    "targetActorId": "ville-paris-xyz789",
    "senderActorId": "system",
    "message": {
      "@class": "com.acme.saf.actor.core.SimpleMessage",
      "messageId": "msg-003",
      "timestamp": "2025-12-26T10:00:00.000Z",
      "correlationId": null,
      "payload": {
        "messageType": "UnregisterClient",
        "clientId": "client-alice-abc123"
      }
    }
  }'
```

### 4. Request Ville Info via Client Actor

The frontend sends commands to the ClientActor, which forwards requests to the appropriate VilleActor.

```bash
curl -X POST http://localhost:8080/api/v1/actors/{clientActorId}/tell \
  -H "X-API-KEY: test" \
  -H "Content-Type: application/json" \
  -d '{
    "targetActorId": "client-alice-abc123",
    "senderActorId": null,
    "message": {
      "@class": "com.acme.saf.actor.core.SimpleMessage",
      "messageId": "msg-004",
      "timestamp": "2025-12-26T10:00:00.000Z",
      "correlationId": null,
      "payload": {
        "command": "GET_VILLE_INFO:ville-paris-xyz789"
      }
    }
  }'
```

### 5. Enter a City via Client Actor

```bash
curl -X POST http://localhost:8080/api/v1/actors/{clientActorId}/tell \
  -H "X-API-KEY: test" \
  -H "Content-Type: application/json" \
  -d '{
    "targetActorId": "client-alice-abc123",
    "senderActorId": null,
    "message": {
      "@class": "com.acme.saf.actor.core.SimpleMessage",
      "messageId": "msg-005",
      "timestamp": "2025-12-26T10:00:00.000Z",
      "correlationId": null,
      "payload": {
        "command": "ENTER:ville-paris-xyz789"
      }
    }
  }'
```

### 6. Leave a City via Client Actor

```bash
curl -X POST http://localhost:8080/api/v1/actors/{clientActorId}/tell \
  -H "X-API-KEY: test" \
  -H "Content-Type: application/json" \
  -d '{
    "targetActorId": "client-alice-abc123",
    "senderActorId": null,
    "message": {
      "@class": "com.acme.saf.actor.core.SimpleMessage",
      "messageId": "msg-006",
      "timestamp": "2025-12-26T10:00:00.000Z",
      "correlationId": null,
      "payload": {
        "command": "LEAVE"
      }
    }
  }'
```

---

## Querying Actor Information

### 1. Get Actor Details

```bash
curl -H "X-API-KEY: test" \
  http://localhost:8080/api/v1/actors/{actorId}
```

**Expected Response:**
```json
{
  "actorId": "ville-paris-xyz789",
  "serviceId": "ville-service",
  "actorType": "VILLE",
  "status": "ACTIVE",
  "serviceUrl": "http://ville-service:8085",
  "properties": {
    "nom": "Paris",
    "population": 2161000,
    "superficie": 105.4
  }
}
```

### 2. List All Actors

```bash
curl -H "X-API-KEY: test" \
  http://localhost:8080/api/v1/actors
```

### 3. List Actors by Service

```bash
# List all actors in client-service
curl -H "X-API-KEY: test" \
  http://localhost:8080/api/v1/actors/by-service/client-service

# List all actors in ville-service
curl -H "X-API-KEY: test" \
  http://localhost:8080/api/v1/actors/by-service/ville-service

# List all actors in capteur-service
curl -H "X-API-KEY: test" \
  http://localhost:8080/api/v1/actors/by-service/capteur-service
```

---

## Complete IoT City Workflow Example

Here's a complete example of setting up a distributed IoT city system using the correct message format:

```bash
# 1. Create a capteur (temperature sensor)
CAPTEUR_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/actors \
  -H "X-API-KEY: test" \
  -H "Content-Type: application/json" \
  -d '{
    "serviceId": "capteur-service",
    "actorType": "CapteurActor",
    "params": {
      "sensorType": "TEMPERATURE",
      "location": "Fourviere",
      "minValue": -10.0,
      "maxValue": 42.0,
      "unit": "C"
    }
  }')
CAPTEUR_ID=$(echo $CAPTEUR_RESPONSE | jq -r '.actorId')
echo "Created capteur: $CAPTEUR_ID"

# 2. Create a city (Ville)
VILLE_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/actors \
  -H "X-API-KEY: test" \
  -H "Content-Type: application/json" \
  -d '{
    "serviceId": "ville-service",
    "actorType": "VilleActor",
    "params": {
      "nom": "Lyon",
      "population": 516092,
      "superficie": 47.87
    }
  }')
VILLE_ID=$(echo $VILLE_RESPONSE | jq -r '.actorId')
echo "Created ville: $VILLE_ID"

# 3. Create a client
CLIENT_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/actors \
  -H "X-API-KEY: test" \
  -H "Content-Type: application/json" \
  -d '{
    "serviceId": "client-service",
    "actorType": "ClientActor",
    "params": {
      "clientName": "Bob"
    }
  }')
CLIENT_ID=$(echo $CLIENT_RESPONSE | jq -r '.actorId')
echo "Created client: $CLIENT_ID"

# 4. Register capteur to ville
curl -X POST "http://localhost:8080/api/v1/actors/$VILLE_ID/tell" \
  -H "X-API-KEY: test" \
  -H "Content-Type: application/json" \
  -d "{
    \"targetActorId\": \"$VILLE_ID\",
    \"senderActorId\": \"system\",
    \"message\": {
      \"@class\": \"com.acme.saf.actor.core.SimpleMessage\",
      \"messageId\": \"$(uuidgen)\",
      \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%S.000Z)\",
      \"correlationId\": null,
      \"payload\": {
        \"capteurId\": \"$CAPTEUR_ID\",
        \"capteurType\": \"TEMPERATURE\",
        \"kafkaTopic\": \"capteur-temperature-lyon\",
        \"location\": \"Fourviere\"
      }
    }
  }"

# 5. Register client to ville
curl -X POST "http://localhost:8080/api/v1/actors/$VILLE_ID/tell" \
  -H "X-API-KEY: test" \
  -H "Content-Type: application/json" \
  -d "{
    \"targetActorId\": \"$VILLE_ID\",
    \"senderActorId\": \"system\",
    \"message\": {
      \"@class\": \"com.acme.saf.actor.core.SimpleMessage\",
      \"messageId\": \"$(uuidgen)\",
      \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%S.000Z)\",
      \"correlationId\": null,
      \"payload\": {
        \"messageType\": \"RegisterClient\",
        \"clientId\": \"$CLIENT_ID\"
      }
    }
  }"

# 6. Query city status
curl -H "X-API-KEY: test" \
  "http://localhost:8080/api/v1/actors/$VILLE_ID"
```

---

## Health Checks

Check if services are running:

```bash
# SAF Control (API Gateway)
curl http://localhost:8080/actuator/health

# Client Service
curl http://localhost:8084/actuator/health

# Ville Service
curl http://localhost:8085/actuator/health

# Capteur Service
curl http://localhost:8086/actuator/health
```

**Expected Response:**
```json
{
  "status": "UP"
}
```

---

## Monitoring & Metrics

### Prometheus Metrics

View metrics for all services:
```
http://localhost:9090
```

Useful queries:
- `up` - Service availability
- `jvm_memory_used_bytes` - Memory usage
- `http_server_requests_seconds_count` - Request count
- `actor_message_processing_seconds_count` - Actor message processing

### Service Metrics Endpoints

```bash
# SAF Control metrics
curl http://localhost:8080/actuator/prometheus

# Client Service metrics
curl http://localhost:8084/actuator/prometheus

# Ville Service metrics
curl http://localhost:8085/actuator/prometheus

# Capteur Service metrics
curl http://localhost:8086/actuator/prometheus
```

---

## WebSocket Connection

The frontend connects to a WebSocket to receive real-time updates from actors.

### WebSocket URL

```
ws://localhost:8084/ws/actor/{actorId}
```

Note: WebSocket connections go directly to the client-service (port 8084), not through saf-control.

### Message Types Received via WebSocket

1. **ClimateReport** - Climate data updates from VilleActor
2. **VilleInfoResponse** - Ville details response

---

## Using the Frontend

1. Open http://localhost in your browser
2. Navigate to the IoT City Dashboard
3. The dashboard automatically creates a ClientActor for your session
4. Select a city to view real-time climate updates
5. Monitor temperature, humidity, and pressure data
6. View historical data in time series charts

---

## Initialization Script

Use the provided script to initialize the IoT City with sample data:

```bash
./scripts/init-iot-city.sh
```

This script:
1. Creates 9 capteurs (3 per city: TEMPERATURE, HUMIDITY, PRESSURE)
2. Creates 3 villes (Paris, Lyon, Marseille)
3. Associates capteurs to villes via RegisterCapteur messages
4. Creates 3 clients (Alice, Bob, Charlie)
5. Registers clients to cities via RegisterClient messages

---

## Common Issues & Troubleshooting

### Issue: Getting 403 Forbidden

**Cause:** Missing or incorrect API key

**Solution:** Ensure you include the API key header:
```bash
curl -H "X-API-KEY: test" http://localhost:8080/api/v1/services
```

### Issue: Service Not Found

**Cause:** Service hasn't registered yet or is unhealthy

**Solution:** 
1. Check service health:
   ```bash
   docker compose ps
   ```
2. Check service logs:
   ```bash
   docker compose logs client-service
   docker compose logs ville-service
   docker compose logs capteur-service
   ```
3. Wait for services to register (check heartbeats):
   ```bash
   curl -H "X-API-KEY: test" http://localhost:8080/api/v1/services
   ```

### Issue: Actor Creation Fails

**Cause:** Service not registered or actor already exists

**Solution:**
1. Verify service is registered
2. Use unique actor IDs
3. Check service logs for errors

### Issue: Messages Not Being Delivered

**Cause:** Actor doesn't exist or wrong actor ID

**Solution:**
1. Verify actor exists:
   ```bash
   curl -H "X-API-KEY: test" http://localhost:8080/api/v1/actors/{actorId}
   ```
2. Check actor ID spelling
3. Ensure target service is healthy

### Issue: Message Deserialization Error

**Cause:** Missing `@class` annotation in message

**Solution:** Ensure your message includes the Jackson type info:
```json
{
  "message": {
    "@class": "com.acme.saf.actor.core.SimpleMessage",
    ...
  }
}
```

### Issue: Need to change API key

1. Update `.env` file:
   ```
   SAF_API_KEY=your-new-key
   ```

2. Restart services:
   ```bash
   docker compose down
   docker compose up -d
   ```

---

## Docker Commands

```bash
# Start all services
docker compose up -d

# View logs for specific service
docker compose logs -f saf-control
docker compose logs -f client-service
docker compose logs -f ville-service
docker compose logs -f capteur-service

# View all logs
docker compose logs -f

# Stop all services
docker compose down

# Rebuild and restart
docker compose up -d --build

# View running containers
docker compose ps

# Check service health
docker compose ps
# Look for "(healthy)" status

# Restart a specific service
docker compose restart client-service
```

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│           Frontend (React - Port 80)                 │
│  - Creates ClientActor on session start              │
│  - WebSocket to client-service for updates           │
└────────────────────┬────────────────────────────────┘
                     ↓ HTTP (via nginx proxy)
┌─────────────────────────────────────────────────────┐
│      SAF Control - API Gateway (Port 8080)          │
│  - Actor Registry                                    │
│  - Service Discovery                                 │
│  - Request Routing                                   │
│  - API Key Authentication                            │
└─────┬─────────────┬─────────────┬───────────────────┘
      ↓ HTTP        ↓ HTTP        ↓ HTTP
┌─────────────┐ ┌──────────────┐ ┌──────────────────┐
│Client Service│ │Ville Service │ │Capteur Service   │
│  (8084)     │ │   (8085)     │ │    (8086)        │
│             │ │              │ │                  │
│ClientActors │ │VilleActors   │ │CapteurActors     │
│  + WebSocket│ │              │ │  + Kafka Producer│
└──────┬──────┘ └───────┬──────┘ └────────┬─────────┘
       │                │                  │
       │    ┌───────────┴──────────────────┘
       │    │      Kafka (9092)
       └────┴───── actor-{actorId} topics
```

Each microservice:
- Runs its own ActorSystem
- Auto-registers with SAF Control on startup
- Sends periodic heartbeats
- Handles its specific actor types
- Communicates via **HTTP** for actor management (create, delete, tell via API)
- Communicates via **Kafka** for inter-actor async messaging

---

## Apache Kafka Messaging

The platform uses **Apache Kafka** for asynchronous communication between actors across microservices.

### Communication Pattern

| Channel | Purpose | Direction |
|---------|---------|-----------|
| **HTTP/REST** | Actor creation, deletion, management | Client → SAF Control → Microservices |
| **Kafka** | Inter-actor messaging (fire & forget) | Actor → Kafka → Actor |

### Kafka Topics

Each actor has a dedicated topic named `actor-{actorId}`:

- **CapteurActor** publishes to `actor-{villeActorId}` → **VilleActor** receives
- **VilleActor** publishes to `actor-{clientActorId}` → **ClientActor** receives
- **VilleActor** publishes to `actor-{capteurActorId}` → **CapteurActor** receives (config)

### Message Flows

```
┌──────────────────┐        Kafka         ┌──────────────────┐
│  CapteurActor    │ ──SensorReading────► │   VilleActor     │
│  (capteur-svc)   │   topic:actor-{ville}│   (ville-svc)    │
└──────────────────┘                      └────────┬─────────┘
                                                   │
                                     ClimateReport │ topic:actor-{client}
                                                   ▼
                                          ┌──────────────────┐
                                          │   ClientActor    │
                                          │   (client-svc)   │
                                          │   + WebSocket    │
                                          └──────────────────┘
```

### Message Types via Kafka

1. **SensorReading** (Capteur → Ville via Kafka)
   ```json
   {
     "capteurId": "capteur-temp-paris-01",
     "type": "TEMPERATURE",
     "value": 22.5,
     "unit": "C",
     "timestamp": "2025-12-26T10:00:00Z"
   }
   ```

2. **ClimateReport** (Ville → Client via Kafka)
   ```json
   {
     "villeId": "ville-paris-xyz789",
     "villeName": "Paris",
     "aggregatedData": {
       "TEMPERATURE": 21.3,
       "HUMIDITY": 55.2,
       "PRESSURE": 1013.5
     },
     "activeCapteurs": 3,
     "timestampMillis": 1735210810000
   }
   ```

3. **AssociateCapteurToVille** (Ville → Capteur via Kafka)
   ```json
   {
     "villeId": "ville-paris-xyz789",
     "villeName": "Paris",
     "climateConfig": {
       "targetTemperature": 20.0,
       "targetHumidity": 50.0,
       "targetPressure": 1013.25
     }
   }
   ```

### Monitoring Kafka

```bash
# View Kafka logs
docker compose logs -f kafka

# List all topics
docker compose exec kafka kafka-topics --list --bootstrap-server localhost:9092

# View messages for a specific actor
docker compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic actor-{actorId} \
  --from-beginning

# Check consumer groups
docker compose exec kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --list
```

### Kafka Health Check

```bash
# Check Kafka is running
docker compose exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Check broker status
docker compose logs kafka | grep -i "started"
```

### Troubleshooting Kafka

**Messages not being received:**
1. Check Kafka is running: `docker compose ps kafka`
2. Check Zookeeper: `docker compose ps zookeeper`
3. Check service logs: `docker compose logs ville-service | grep -i kafka`
4. Verify topic exists: `docker compose exec kafka kafka-topics --list --bootstrap-server localhost:9092`

**Consumer not receiving messages:**
1. Check consumer group: `docker compose exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 --group ville-service-group --describe`
2. Check offset: Consumer might be at latest offset, try `--from-beginning`

---

## Access Swagger UI

Open in browser:
```
http://localhost:8080/swagger
```

Or get OpenAPI documentation:
```bash
curl http://localhost:8080/v3/api-docs
