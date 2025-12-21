# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy framework dependencies
COPY backend/framework/saf-actor-core/pom.xml /app/saf-actor-core/pom.xml
COPY backend/framework/saf-actor-core/src /app/saf-actor-core/src

# Copy domain
COPY backend/apps/iot-city/iot-city-domain/pom.xml /app/iot-city-domain/pom.xml
COPY backend/apps/iot-city/iot-city-domain/src /app/iot-city-domain/src

# Copy client-service
COPY backend/apps/iot-city/client-service/pom.xml /app/client-service/pom.xml
COPY backend/apps/iot-city/client-service/src /app/client-service/src

# Build dependencies
WORKDIR /app/saf-actor-core
RUN mvn clean install -DskipTests

WORKDIR /app/iot-city-domain
RUN mvn clean install -DskipTests

# Build service
WORKDIR /app/client-service
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/client-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
