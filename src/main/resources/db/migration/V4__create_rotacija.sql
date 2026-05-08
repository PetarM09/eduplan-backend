-- ============================================================================
-- V4: Rotacioni raspored za grupe (vezbe)
-- ============================================================================

CREATE TABLE rot_konfiguracije (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    skola_id            UUID NOT NULL REFERENCES skole(id) ON DELETE CASCADE,
    nastavnik_id        UUID NOT NULL REFERENCES korisnici(id) ON DELETE CASCADE,
    predmet_id          UUID REFERENCES predmeti(id) ON DELETE SET NULL,
    naziv               VARCHAR(255) NOT NULL,
    odeljenja_json      JSONB NOT NULL,
    grupa_velicina      SMALLINT NOT NULL DEFAULT 2,
    casova_nedeljno     SMALLINT NOT NULL,
    skolska_godina      VARCHAR(9) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255)
);

CREATE INDEX idx_rot_konfig_skola_nastavnik ON rot_konfiguracije (skola_id, nastavnik_id);

CREATE TABLE rot_nedelje (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    konfiguracija_id    UUID NOT NULL REFERENCES rot_konfiguracije(id) ON DELETE CASCADE,
    skola_id            UUID NOT NULL REFERENCES skole(id) ON DELETE CASCADE,
    broj_nedelje        SMALLINT NOT NULL,
    odeljenja_json      JSONB NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),
    CONSTRAINT uq_rot_nedelje UNIQUE (konfiguracija_id, broj_nedelje)
);

CREATE INDEX idx_rot_nedelje_konfig ON rot_nedelje (konfiguracija_id);
