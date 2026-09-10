FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/ledger-service-1.0.0.jar app.jar

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8081/health || exit 1

ENTRYPOINT ["java", "-Xmx512m", "-Xms256m", "-jar", "app.jar"]
