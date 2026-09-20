# Stage 1: Build application
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Pre-cache Maven dependencies layer
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source code and build jar
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime environment
FROM eclipse-temurin:21-jre
WORKDIR /app

# Create non-privileged user and group
RUN groupadd -r appgroup && useradd -r -g appgroup -d /app -s /sbin/nologin appuser

# Ensure upload directory exists and set permissions
RUN mkdir -p /app/uploads && chown -R appuser:appgroup /app

# Copy built artifact from build stage
COPY --from=builder --chown=appuser:appgroup /build/target/*.jar app.jar

USER appuser

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
