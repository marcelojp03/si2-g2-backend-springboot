# ── Etapa 1: compilar ──────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-23-alpine AS builder

WORKDIR /app

# Copiar solo el pom primero para aprovechar el cache de capas
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

# Copiar fuentes y empaquetar
COPY src ./src
RUN mvn clean package -DskipTests -B -q

# ── Etapa 2: imagen final ──────────────────────────────────────────────────────
FROM eclipse-temurin:23-jre-alpine

WORKDIR /app

# Usuario sin privilegios
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /app/target/*.jar app.jar

# Credenciales Firebase (fuera del repo por .gitignore; debe existir en contexto build)
COPY gestion-academica-firebase.json .

RUN chown appuser:appgroup app.jar gestion-academica-firebase.json

USER appuser

EXPOSE 2026

ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]
