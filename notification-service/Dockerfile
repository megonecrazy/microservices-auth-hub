# ---------- STAGE 1: BUILD ----------
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests


# ---------- STAGE 2: RUN ----------
FROM eclipse-temurin:21-jre-jammy

RUN groupadd -r appuser && useradd -r -g appuser appuser

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

USER appuser

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
