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
# Debian baza umesto Alpine — potreban je LibreOffice za Word->PDF konverziju
# planova; LO se na Alpine-u ponasa nestabilno (fonts, runtime libs).
FROM eclipse-temurin:21-jre AS runtime

# LibreOffice headless za PDF konverziju + fontovi sa cirilicom.
# --no-install-recommends drzi sliku malom (oko 600MB ukupno).
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        libreoffice-core \
        libreoffice-writer \
        fonts-dejavu \
        fonts-liberation \
        ca-certificates \
        wget \
    && rm -rf /var/lib/apt/lists/* \
    && apt-get clean

# Non-root user — najbolji bezbednosni default
RUN groupadd --system app && useradd --system --gid app --shell /bin/bash app

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
