# ── Stage 1 : build Maven ────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Télécharger les dépendances en couche séparée (cache Docker)
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

COPY src ./src
RUN mvn clean package -DskipTests -B -q

# ── Stage 2 : image runtime légère ───────────────────────────────────────────
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
