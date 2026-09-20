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

# Copy compiled JAR from the builder stage
COPY --from=builder /app/target/*.jar /app/app.jar

# Adjust ownership
RUN chown -R spring:spring /app

USER spring:spring

# Render dynamically passes $PORT (typically 10000 or 8080).
# We default to 8080 if PORT is not set.
ENV PORT=8080
EXPOSE 8080

# JVM memory management tailored for cloud container environments (Render free/starter tiers ~512MB RAM)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -XX:+ExitOnOutOfMemoryError"

# Start the Spring Boot application binding to the dynamic port
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT} -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]
