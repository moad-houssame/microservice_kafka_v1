# ==========================================
# 🛒 DOCKERFILE - Order Service
# ==========================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY target/*.jar app.jar
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.3.0/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar

EXPOSE 8083

ENTRYPOINT ["java", "-javaagent:/app/opentelemetry-javaagent.jar", "-jar", "app.jar"]
