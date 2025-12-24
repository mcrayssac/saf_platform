# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy framework modules
COPY backend/framework/pom.xml /app/framework/pom.xml
COPY backend/framework/saf-actor-core/pom.xml /app/framework/saf-actor-core/pom.xml
COPY backend/framework/saf-actor-core/src /app/framework/saf-actor-core/src
COPY backend/framework/saf-runtime/pom.xml /app/framework/saf-runtime/pom.xml
COPY backend/framework/saf-runtime/src /app/framework/saf-runtime/src

# Copy domain module
COPY backend/apps/iot-city/iot-city-domain/pom.xml /app/iot-city-domain/pom.xml
COPY backend/apps/iot-city/iot-city-domain/src /app/iot-city-domain/src

# Copy capteur-service
COPY backend/apps/iot-city/capteur-service/pom.xml /app/capteur-service/pom.xml
COPY backend/apps/iot-city/capteur-service/src /app/capteur-service/src

# Build framework parent pom
WORKDIR /app/framework
RUN mvn clean install -N -DskipTests

# Build saf-actor-core
WORKDIR /app/framework/saf-actor-core
RUN mvn clean install -DskipTests

# Build saf-runtime
WORKDIR /app/framework/saf-runtime
RUN mvn clean install -DskipTests

# Build iot-city-domain
WORKDIR /app/iot-city-domain
RUN mvn clean install -DskipTests

# Build capteur-service
WORKDIR /app/capteur-service
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the built jar from build stage
COPY --from=build /app/capteur-service/target/*.jar app.jar

# Expose port
EXPOSE 8086

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8086/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
