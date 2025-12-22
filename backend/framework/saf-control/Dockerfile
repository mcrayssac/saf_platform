# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy framework parent pom
COPY backend/framework/pom.xml /app/framework/pom.xml

# Copy saf-control application
COPY backend/framework/saf-control/pom.xml /app/saf-control/pom.xml
COPY backend/framework/saf-control/src /app/saf-control/src

# Build framework parent pom first
WORKDIR /app/framework
RUN mvn clean install -N -DskipTests

# Build saf-control
WORKDIR /app/saf-control
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the built jar from build stage
COPY --from=build /app/saf-control/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
