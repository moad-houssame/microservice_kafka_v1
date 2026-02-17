# ==========================================
# 💳 DOCKERFILE - Payment Service
# ==========================================
FROM eclipse-temurin:21-jdk-alpine as build
WORKDIR /app

# Copier les fichiers Maven
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Télécharger les dépendances
RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline -B

# Copier le code source et construire
COPY src src
RUN ./mvnw package -DskipTests

# ==========================================
# Runtime stage
# ==========================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
