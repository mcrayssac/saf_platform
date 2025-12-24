# Docker Deployment Guide

This guide explains how to run the SAF Platform using Docker Compose.

## Prerequisites

- Docker Engine 20.10+
- Docker Compose V2+
- At least 2GB of available RAM

## Quick Start

### 1. Configure Environment Variables

Copy the example environment file and update it with your values:

```bash
cp .env.example .env
```

Edit `.env` and set your API key:

```bash
API_KEY=your-secure-api-key-here
```

### 2. Start All Services

Run the following command from the project root:

```bash
docker-compose up -d
```

This command will:
- Build the SAF-Control Docker image (Spring Boot with Java 21)
- Build the IoT City microservices Docker images (client-service, ville-service, capteur-service)
- Build the frontend Docker image (React with Nginx)
- Start SAF-Control (orchestrator and API gateway)
- Start the 3 IoT City microservices (each hosting their specific actors)
- Start the frontend service
- Start Prometheus for metrics scraping

### 3. Verify Services

Check that all services are running:

```bash
docker-compose ps
```

All services should show as "healthy" after initialization.

### 4. Access the Application

- **Frontend**: http://localhost
- **SAF-Control API**: http://localhost:8080
- **Client service**: http://localhost:8082
- **Ville service**: http://localhost:8083
- **Capteur service**: http://localhost:8084
- **Swagger UI**: http://localhost:8080/swagger
- **Health Check**: http://localhost:8080/actuator/health
- **Prometheus UI**: http://localhost:9090

**Note**: The 3 default cities (Paris, Lyon, Marseille) with their sensors are automatically created on startup.

## Service Architecture

```
┌─────────────┐
│   Browser   │
└──────┬──────┘
       │
       ▼
┌─────────────────┐
│    Frontend     │  (Port 80)
│  (React/Nginx)  │
└────────┬────────┘
         │
         │ /api/* → proxy
         ▼
┌─────────────────────────────────────────┐
│           SAF-Control (Port 8080)       │
│    (Orchestrator & API Gateway)         │
│  - Distributed Actor Registry           │
│  - Service Discovery                    │
│  - Actor Creation & Management          │
│  - Default City Initialization          │
└──────────┬──────────────────────────────┘
           │
           │ HTTP calls to create/manage actors
           │
           ├──────────┬──────────┬──────────┐
           ▼          ▼          ▼          ▼
    ┌──────────┐ ┌──────────┐ ┌──────────┐
    │ client-  │ │  ville-  │ │ capteur- │
    │ service  │ │ service  │ │ service  │
    │ :8082    │ │ :8083    │ │ :8084    │
    │          │ │          │ │          │
    │ Client   │ │ Ville    │ │ Capteur  │
    │ Actors   │ │ Actors   │ │ Actors   │
    └──────────┘ └──────────┘ └──────────┘
           │          │          │
           └──────────┴──────────┴──────────┐
                                            ▼
                                   ┌─────────────────┐
                                   │   Prometheus    │
                                   │   (Port 9090)   │
                                   └─────────────────┘
```

**Architecture Notes**:
- **SAF-Control** is the central orchestrator that manages all actor microservices
- Each **IoT microservice** hosts specific actor types using SAF-Runtime base classes
- Actors communicate via HTTP through SAF-Control's routing layer

## Docker Compose Commands

### Start Services

```bash
# Start all services in detached mode
docker-compose up -d

# Start and view logs
docker-compose up

# Start specific service
docker-compose up -d saf-control
docker-compose up -d client-service
docker-compose up -d ville-service
docker-compose up -d capteur-service
```

### Stop Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

### View Logs

```bash
# View all logs
docker-compose logs

# Follow logs for all services
docker-compose logs -f

# View logs for specific service
docker-compose logs -f saf-control
docker-compose logs -f ville-service
docker-compose logs -f frontend
```

### Restart Services

```bash
# Restart all services
docker-compose restart

# Restart specific service
docker-compose restart saf-control
docker-compose restart client-service
```

### Rebuild Images

```bash
# Rebuild all images
docker-compose build

# Rebuild specific service
docker-compose build saf-control
docker-compose build client-service

# Rebuild and start
docker-compose up -d --build
```

## Service Details

### SAF-Control (Spring Boot)

- **Container**: `saf-control`
- **Port**: 8080
- **Build time**: ~2-3 minutes (first build with dependency download)
- **Startup time**: ~30-60 seconds
- **Health Check**: `/actuator/health`
- **Role**: Central orchestrator and API gateway
- **Responsibilities**:
  - Distributed actor registry
  - Service discovery
  - Actor creation/management via HTTP
  - Default city initialization (3 cities + sensors)
  - API authentication
- **Environment Variables**:
  - `SPRING_PROFILES_ACTIVE`: Spring profile (default: prod)
  - `SAF_SECURITY_API_KEY`: API key for authentication
  - `SAF_INIT_ENABLED`: Enable/disable default cities initialization (default: true)
  - `SAF_INIT_DELAY`: Delay before initialization in seconds (default: 5)
  - `JAVA_OPTS`: JVM options (default: -Xmx512m -Xms256m)

### Client Service (Spring Boot)

- **Container**: `client-service`
- **Port**: 8082
- **Role**: Hosts ClientActor instances
- **Health Check**: `/actuator/health`
- **Actors**: Each ClientActor represents a user/client subscribing to city climate updates

### Ville Service (Spring Boot)

- **Container**: `ville-service`
- **Port**: 8083
- **Role**: Hosts VilleActor instances
- **Health Check**: `/actuator/health`
- **Actors**: Each VilleActor represents a city that aggregates sensor data
- **Default Cities**: Paris, Lyon, Marseille (auto-created on startup)

### Capteur Service (Spring Boot)

- **Container**: `capteur-service`
- **Port**: 8084
- **Role**: Hosts CapteurActor instances
- **Health Check**: `/actuator/health`
- **Actors**: Each CapteurActor represents a sensor (temperature, humidity, pressure)
- **Default Sensors**: 3 sensors per city (9 total, auto-created on startup)

### Frontend (React + Nginx)

- **Container**: `saf-frontend`
- **Port**: 80
- **Build time**: ~2-3 minutes (first build with npm install)
- **Proxy**: API requests to `/api/*` are proxied to SAF-Control at `http://saf-control:8080`
- **Health Check**: Root endpoint `/`
- **Features**:
  - Gzip compression enabled
  - Static asset caching
  - SPA routing support
  - Security headers
  - Dark/Light theme support

## Platform Notes

### Microservices Architecture

The platform implements a **true microservices architecture** where:

1. **Each actor type lives in its own microservice**:
   - ClientActor → client-service
   - VilleActor → ville-service
   - CapteurActor → capteur-service

2. **SAF-Runtime is now a library**, not a service:
   - Embedded in each microservice via `saf-runtime` dependency
   - Provides base classes (`BaseActorRuntimeController`, `ActorSystemConfiguration`)

3. **SAF-Control orchestrates everything**:
   - Maintains distributed actor registry
   - Routes actor creation requests to appropriate microservices
   - Initializes default configuration (3 cities + sensors)

### Default Initialization

On startup, SAF-Control automatically creates:
- **3 Cities**: Paris, Lyon, Marseille (via ville-service)
- **9 Sensors**: 3 per city (temperature, humidity, pressure via capteur-service)

This can be disabled by setting `SAF_INIT_ENABLED=false` in the environment.

### ARM64 / Apple Silicon Support

The frontend Dockerfile has been optimized to work on ARM64 (Apple Silicon) Macs. The build process:

1. Uses Debian-based Node.js image (better native module support than Alpine)
2. Performs a clean install and rebuild of all dependencies
3. Explicitly installs ARM64 binaries for Rollup, LightningCSS, and TailwindCSS

This approach is **cross-platform compatible** and will automatically:
- Install the correct binaries for x64 (Intel/AMD) systems
- Install the correct binaries for ARM64 (Apple Silicon) systems
- Work on any other supported architecture

The build may take slightly longer due to the rebuild step, but ensures all native modules are properly compiled.

## Troubleshooting

### Service Won't Start

Check the logs:

```bash
docker-compose logs [service-name]
```

### Port Already in Use

If ports 80 or 8080 are already in use, modify them in `docker-compose.yml`:

```yaml
services:
  saf-control:
    ports:
      - "8081:8080"  # Change host port to 8081
  frontend:
    ports:
      - "8000:80"    # Change host port to 8000
```

### SAF-Control Health Check Failing

SAF-Control can take 30-60 seconds to fully initialize on first start. Wait and check:

```bash
# Watch SAF-Control logs
docker-compose logs -f saf-control

# Check health directly
curl http://localhost:8080/actuator/health

# Check initialization logs
docker-compose logs saf-control | grep DefaultCityInitializer
```

### Default Cities Not Created

Check the initialization logs:

```bash
docker-compose logs saf-control | grep -E "Initializing|Creating|Created"
```

If initialization failed, ensure:
1. All microservices are running and healthy
2. Services have registered with SAF-Control
3. Wait at least 10 seconds after startup

### Frontend Build Fails

If npm install fails, try rebuilding with no cache:

```bash
docker-compose build --no-cache frontend
```

### Clean Restart

Remove all containers, networks, and start fresh:

```bash
docker-compose down
docker-compose up -d --build
```

### Complete System Clean

Remove everything including images:

```bash
docker-compose down
docker system prune -a -f
docker-compose up -d --build
```

## API Authentication

All API requests (except public endpoints) require the `X-API-KEY` header:

```bash
curl -H "X-API-KEY: your-api-key-here" http://localhost:8080/api/v1/actors
```

Public endpoints (no authentication required):
- `/actuator/**` - Health and metrics
- `/swagger/**` - API documentation
- `/v3/api-docs/**` - OpenAPI specification

## Monitoring

### Health Checks

Check service health:

```bash
# SAF-Control health
curl http://localhost:8080/actuator/health

# Client service health
curl http://localhost:8082/actuator/health

# Ville service health
curl http://localhost:8083/actuator/health

# Capteur service health
curl http://localhost:8084/actuator/health

# Frontend (returns 200 if healthy)
curl -I http://localhost/

# All services status
docker-compose ps
```

### Metrics

Prometheus metrics available at:
- SAF-Control: http://localhost:8080/actuator/prometheus
- Client service: http://localhost:8082/actuator/prometheus
- Ville service: http://localhost:8083/actuator/prometheus
- Capteur service: http://localhost:8084/actuator/prometheus

Prometheus UI (scraping all services):
- http://localhost:9090

### Container Stats

View resource usage:

```bash
docker stats
```

## Development vs Production

### Development Mode

For local development with hot reload, run services separately:

```bash
# SAF-Control
cd backend/framework/saf-control
./mvnw spring-boot:run

# Client service
cd backend/apps/iot-city/client-service
mvn spring-boot:run

# Ville service
cd backend/apps/iot-city/ville-service
mvn spring-boot:run

# Capteur service
cd backend/apps/iot-city/capteur-service
mvn spring-boot:run

# Frontend
cd frontend
npm run dev
```

### Production Mode

Use Docker Compose for production deployment:

1. Set secure API key in `.env`
2. Configure HTTPS/TLS with reverse proxy
3. Set appropriate resource limits
4. Enable monitoring and logging
5. Use external database if needed
6. Consider disabling default initialization: `SAF_INIT_ENABLED=false`

## Network Configuration

All services run in the `saf-network` bridge network, allowing them to communicate using service names as hostnames (e.g., `saf-control`, `ville-service`, `frontend`).

## Resource Requirements

### Minimum

- **CPU**: 2 cores
- **RAM**: 2GB
- **Disk**: 5GB

### Recommended

- **CPU**: 4 cores
- **RAM**: 4GB
- **Disk**: 10GB

## Security Best Practices

1. **Change default credentials**: Update `API_KEY` in `.env`
2. **Don't commit secrets**: Never commit `.env` to version control
3. **Use HTTPS**: Configure reverse proxy with SSL/TLS certificates
4. **Update dependencies**: Regularly update base images
5. **Limit exposure**: Use firewall rules to restrict access
6. **Monitor logs**: Set up centralized logging

## Common Commands

```bash
# Start services
docker-compose up -d

# Stop services
docker-compose down

# View logs
docker-compose logs -f

# Restart SAF-Control
docker-compose restart saf-control

# Rebuild and restart
docker-compose up -d --build

# Check status
docker-compose ps

# Execute command in SAF-Control container
docker-compose exec saf-control sh

# View SAF-Control logs
docker-compose logs -f saf-control

# View resource usage
docker stats
```

## CI/CD Integration

Example GitHub Actions workflow:

```yaml
name: Deploy
on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Deploy with docker-compose
        run: |
          docker-compose build
          docker-compose up -d
```

## Support

For issues or questions:
- Check logs: `docker-compose logs`
- Check status: `docker-compose ps`
- Check resources: `docker stats`
- Review this documentation
- Check application logs in containers
- Verify service registration: `curl http://localhost:8080/api/v1/services`
