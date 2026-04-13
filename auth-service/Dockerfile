# ---------- STAGE 1: BUILD ----------
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy only pom first (better caching)
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests


# ---------- STAGE 2: RUN ----------
FROM eclipse-temurin:21-jre-jammy

# Create a non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser

WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Switch to non-root user
USER appuser

# Expose correct port
EXPOSE 8081

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]