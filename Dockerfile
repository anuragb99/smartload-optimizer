# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Cache Maven dependencies first
COPY pom.xml .
RUN apk add --no-cache maven && mvn dependency:go-offline -q

# Build the fat JAR
COPY src ./src
RUN mvn clean package -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

# Non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/target/smartload-optimizer-1.0.0.jar app.jar

# JVM flags tuned for a container: small heap, Z GC for low latency
ENV JAVA_OPTS="-Xms128m -Xmx512m -XX:+UseZGC -XX:+ZGenerational \
               -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
