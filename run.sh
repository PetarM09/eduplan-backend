#!/usr/bin/env bash
# Pokrece Spring Boot backend sa env varijablama iz .env (ako postoji).
# Spring Boot ne cita .env fajl out-of-the-box — ovaj wrapper ucitava sve
# vrednosti u shell environment pre starta da bi ${MAIL_HOST}, ${JWT_SECRET}
# itd. iz application.yml dobile vrednost.
#
# Koristi: ./run.sh           — startuje aplikaciju (mvn spring-boot:run)
#         ./run.sh -Dx=y      — prosledi dodatne JVM args

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

if [ -f .env ]; then
    echo "[run.sh] Ucitavam .env iz $SCRIPT_DIR"
    set -a
    # shellcheck disable=SC1091
    source .env
    set +a
    # Brza provera kljucnih vrednosti
    echo "[run.sh] MAIL_HOST=${MAIL_HOST:-(prazno)}  MAIL_USER=${MAIL_USER:-(prazno)}  MAIL_FROM=${MAIL_FROM:-(prazno)}"
else
    echo "[run.sh] UPOZORENJE: .env ne postoji u $SCRIPT_DIR — koriste se default vrednosti iz application.yml"
fi

./mvnw spring-boot:run "$@"
