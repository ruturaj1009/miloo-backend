# =========================================================================
# Stage 1: Build the Spring Boot application using Maven & Java 17
# =========================================================================
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Copy pom.xml first to optimize Docker layer caching for dependencies
COPY pom.xml .

# Download dependencies in a separate layer
RUN mvn dependency:go-offline -B || mvn dependency:resolve -B

# Copy project source code
COPY src ./src

# Build production executable JAR (skipping test execution during image build)
RUN mvn clean package -DskipTests -B

# =========================================================================
# Stage 2: Lightweight Production Runtime Image (Eclipse Temurin JRE 17)
# =========================================================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Run as non-root user for security best practices
RUN addgroup -S spring && adduser -S spring -G spring

# Copy compiled JAR from builder stage with spring user ownership
COPY --from=builder --chown=spring:spring /app/target/miloo-backend-*.jar /app/app.jar

USER spring:spring

# Render dynamically passes $PORT (typically 10000 or 8080).
# Default to 8080 if PORT is not set.
ENV PORT=8080
EXPOSE 8080

# JVM memory management tailored for cloud container environments (Render free/starter tiers ~512MB RAM)
# 65% MaxRAM leaves ~180MB headroom for Metaspace, threads, JIT cache, and OS overhead to prevent OOM kills
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=65.0 -XX:InitialRAMPercentage=40.0 -XX:+ExitOnOutOfMemoryError"

# Container healthcheck targeting Miloo Health endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD wget -qO- http://localhost:${PORT:-8080}/api/v1/health/check || exit 1

# Start Spring Boot application using exec for proper OS signal forwarding (SIGTERM / graceful shutdown)
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dserver.port=${PORT} -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]
