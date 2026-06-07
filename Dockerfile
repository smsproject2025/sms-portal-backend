# Dockerfile
# Place this at: smsportal-backend/Dockerfile
# Replaces your existing Dockerfile.

# ── Stage 1: Build ───────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# Cache Maven dependencies separately (only re-downloads if pom.xml changes)
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

# Build the jar
COPY src ./src
RUN mvn clean package -DskipTests -q

# ── Stage 2: Runtime ─────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder /app/target/*.jar app.jar
RUN chown spring:spring app.jar

USER spring

# Render injects PORT env var; Spring reads server.port=${PORT:8080}
EXPOSE 8080

# JVM flags tuned for Render free tier (512MB RAM limit)
# -XX:MaxRAMPercentage=75  → cap heap at 75% of container memory
# -XX:+UseContainerSupport → respect container memory limits
# -XX:+UseG1GC             → best GC for low-latency web apps
ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseContainerSupport", \
  "-XX:+UseG1GC", \
  "-XX:MaxGCPauseMillis=200", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
