# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy framework modules first (they're dependencies)
COPY backend/framework/saf-actor-core/pom.xml /app/saf-actor-core/pom.xml
COPY backend/framework/saf-actor-core/src /app/saf-actor-core/src

# Copy application domain
COPY backend/apps/iot-city/iot-city-domain/pom.xml /app/iot-city-domain/pom.xml
COPY backend/apps/iot-city/iot-city-domain/src /app/iot-city-domain/src

# Copy runtime application
COPY backend/apps/iot-city/iot-runtime/pom.xml /app/iot-runtime/pom.xml
COPY backend/apps/iot-city/iot-runtime/src /app/iot-runtime/src

# Build framework dependencies first
WORKDIR /app/saf-actor-core
RUN mvn clean install -DskipTests

# Build domain
WORKDIR /app/iot-city-domain
RUN mvn clean install -DskipTests

# Build runtime
WORKDIR /app/iot-runtime
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/iot-runtime/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
