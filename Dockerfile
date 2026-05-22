# syntax=docker/dockerfile:1.7
# =============================================================================
# Multi-stage build za skolska-platforma:
#   1) build stage:   Maven + JDK 21 → uzima zavisnosti, kompajlira, gradi jar
#   2) runtime stage: JRE 21 alpine → samo jar + non-root user
# Krajnja slika je manja od 250MB.
# =============================================================================

# ---- Stage 1: BUILD --------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Prvo kopiramo pom.xml da Docker layer cache zadrzi dependency download
# izmedju build-ova (dok god se pom.xml ne menja).
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw mvnw
RUN ./mvnw -B -q dependency:go-offline

# Onda kopiramo izvorne fajlove i pakujemo
COPY src src
RUN ./mvnw -B -q package -DskipTests \
    && cp target/skolska-platforma-*.jar /workspace/app.jar

# ---- Stage 2: RUNTIME ------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

# Non-root user — najbolji bezbednosni default
RUN addgroup -S app && adduser -S -G app app

# Storage direktorijum za Word/PDF planove (volume u produkciji)
RUN mkdir -p /app/storage && chown -R app:app /app

USER app
WORKDIR /app

COPY --from=build --chown=app:app /workspace/app.jar app.jar

# Konfiguracija ide kroz env varijable — pogledaj application.yml
ENV STORAGE_DIR=/app/storage \
    SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

# Container-friendly healthcheck (sa actuator-om)
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
