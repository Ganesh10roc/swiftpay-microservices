# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy entire project
COPY . .

RUN apk add --no-cache maven && \
    mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /build/analytics-worker/target/analytics-worker-1.0.0.jar app.jar

EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8082/health || exit 1

ENTRYPOINT ["java", "-Xmx256m", "-Xms128m", "-jar", "app.jar"]
