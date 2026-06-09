# Multi-stage build for Spring Boot application
FROM maven:3.9.9-eclipse-temurin-25 AS build

WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/target/StockasticApplicationBackend-0.0.1-SNAPSHOT.jar app.jar

# Create directories for file uploads
RUN mkdir -p /app/uploads/profiles /app/uploads/kyc /app/uploads/stocks

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
