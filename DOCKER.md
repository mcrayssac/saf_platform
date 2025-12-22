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
- Build the backend Docker image (SAF-Control, Spring Boot with Java 21)
- Build the runtime Docker image (SAF-Runtime, Spring Boot with Java 21)
- Build the IoT City microservices (client/ville/capteur)
- Build the frontend Docker image (React with Nginx)
- Start the backend service
- Start the runtime service
- Start the IoT City microservices
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
- **Backend API**: http://localhost:8080
- **Runtime API**: http://localhost:8081
- **Client service**: http://localhost:8082
- **Ville service**: http://localhost:8083
- **Capteur service**: http://localhost:8084
- **Swagger UI**: http://localhost:8080/swagger
- **Health Check**: http://localhost:8080/actuator/health
- **Runtime Health Check**: http://localhost:8081/actuator/health
- **Prometheus UI**: http://localhost:9090

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
┌─────────────────┐
│     Backend     │  (Port 8080)
│  (Spring Boot)  │
└─────────────────┘
         │
         ├─────────────┐
         │             │
         ▼             ▼
┌─────────────────┐ ┌─────────────────┐
│     Runtime     │ │  IoT Services   │
│  (Port 8081)    │ │ (8082-8084)     │
└─────────────────┘ └─────────────────┘
         │             │
         ▼             ▼
┌─────────────────┐
│   Prometheus    │  (Port 9090)
└─────────────────┘
```

## Docker Compose Commands

### Start Services

```bash
# Start all services in detached mode
docker-compose up -d

# Start and view logs
docker-compose up

# Start specific service
docker-compose up -d backend
docker-compose up -d runtime
docker-compose up -d client-service
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
docker-compose logs -f backend
docker-compose logs -f runtime
docker-compose logs -f frontend
```

### Restart Services

```bash
# Restart all services
docker-compose restart

# Restart specific service
docker-compose restart backend
docker-compose restart runtime
docker-compose restart client-service
```

### Rebuild Images

```bash
# Rebuild all images
docker-compose build

# Rebuild specific service
docker-compose build backend
docker-compose build runtime
docker-compose build client-service

# Rebuild and start
docker-compose up -d --build
```

## Service Details

### Backend (Spring Boot)

- **Container**: `saf-backend`
- **Port**: 8080
- **Build time**: ~2-3 minutes (first build with dependency download)
- **Startup time**: ~30-60 seconds
- **Health Check**: `/actuator/health`
- **Environment Variables**:
  - `SPRING_PROFILES_ACTIVE`: Spring profile (default: prod)
  - `SAF_SECURITY_API_KEY`: API key for authentication
  - `JAVA_OPTS`: JVM options (default: -Xmx512m -Xms256m)

### Runtime (Spring Boot)

- **Container**: `saf-runtime`
- **Port**: 8081
- **Build time**: ~2-3 minutes (first build with dependency download)
- **Startup time**: ~30-60 seconds
- **Health Check**: `/actuator/health`
- **Environment Variables**:
  - `SPRING_PROFILES_ACTIVE`: Spring profile (default: prod)
  - `JAVA_OPTS`: JVM options (default: -Xmx512m -Xms256m)

### Client Service (Spring Boot)

- **Container**: `client-service`
- **Port**: 8082
- **Health Check**: `/actuator/health`

### Ville Service (Spring Boot)

- **Container**: `ville-service`
- **Port**: 8083
- **Health Check**: `/actuator/health`

### Capteur Service (Spring Boot)

- **Container**: `capteur-service`
- **Port**: 8084
- **Health Check**: `/actuator/health`

### Frontend (React + Nginx)

- **Container**: `saf-frontend`
- **Port**: 80
- **Build time**: ~2-3 minutes (first build with npm install)
- **Proxy**: API requests to `/api/*` are proxied to backend at `http://backend:8080`
- **Health Check**: Root endpoint `/`
- **Features**:
  - Gzip compression enabled
  - Static asset caching
  - SPA routing support
  - Security headers

## Platform Notes

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
  backend:
    ports:
      - "8081:8080"  # Change host port to 8081
  frontend:
    ports:
      - "8000:80"    # Change host port to 8000
```

### Backend Health Check Failing

The backend can take 30-60 seconds to fully initialize on first start. Wait and check:

```bash
# Watch backend logs
docker-compose logs -f backend

# Check health directly
curl http://localhost:8080/actuator/health
```

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
curl -H "X-API-KEY: your-api-key-here" http://localhost:8080/agents
```

Public endpoints (no authentication required):
- `/actuator/**` - Health and metrics
- `/swagger/**` - API documentation
- `/v3/api-docs/**` - OpenAPI specification

## Monitoring

### Health Checks

Check service health:

```bash
# Backend health
curl http://localhost:8080/actuator/health

# Runtime health
curl http://localhost:8081/actuator/health

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
- http://localhost:8081/actuator/prometheus

Prometheus UI (scraping saf-runtime):
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
# Control
cd backend/framework/saf-control
./mvnw spring-boot:run

# Runtime
cd backend/framework/saf-runtime
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

## Network Configuration

All services run in the `saf-network` bridge network, allowing them to communicate using service names as hostnames (e.g., `backend`, `frontend`).

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

# Restart service
docker-compose restart backend

# Rebuild and restart
docker-compose up -d --build

# Check status
docker-compose ps

# Execute command in container
docker-compose exec backend sh

# View backend logs
docker-compose logs -f backend

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
