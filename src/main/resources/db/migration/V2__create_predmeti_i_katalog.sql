-- ============================================================================
-- V2: Predmeti, odeljenja-veze, katalog tema/nastavnih jedinica/ishoda,
--     padajuci meniji za tipove casa i metode rada
-- ============================================================================

CREATE TABLE predmeti (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    skola_id        UUID NOT NULL REFERENCES skole(id) ON DELETE CASCADE,
    naziv           VARCHAR(255) NOT NULL,
    razred          SMALLINT,
    fond_casova     SMALLINT,
    aktivan         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    CONSTRAINT uq_predmeti_naziv_razred UNIQUE (skola_id, naziv, razred)
);

CREATE INDEX idx_predmeti_skola ON predmeti (skola_id);

-- Veza N:M predmet <-> odeljenje (odeljenja se kreiraju u V3)
CREATE TABLE predmet_odeljenja (
    predmet_id      UUID NOT NULL REFERENCES predmeti(id) ON DELETE CASCADE,
    odeljenje_id    UUID NOT NULL,
    PRIMARY KEY (predmet_id, odeljenje_id)
);

-- ----------------------------------------------------------------------------
-- KATALOG: teme, nastavne jedinice, ishodi
-- Kreiraju se pri godisnjim/operativnim planovima i ostaju za buducu upotrebu.
-- ----------------------------------------------------------------------------

CREATE TABLE teme (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    skola_id        UUID NOT NULL REFERENCES skole(id) ON DELETE CASCADE,
    predmet_id      UUID NOT NULL REFERENCES predmeti(id) ON DELETE CASCADE,
    redni_broj      SMALLINT NOT NULL DEFAULT 0,
    naziv           VARCHAR(500) NOT NULL,
    cas_obrada      SMALLINT NOT NULL DEFAULT 0,
    cas_utvrd       SMALLINT NOT NULL DEFAULT 0,
    cas_ostalo      SMALLINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    CONSTRAINT uq_teme_naziv UNIQUE (skola_id, predmet_id, naziv)
);

CREATE INDEX idx_teme_predmet      ON teme (predmet_id);
CREATE INDEX idx_teme_naziv_search ON teme (skola_id, predmet_id, LOWER(naziv));

CREATE TABLE nastavne_jedinice (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    skola_id        UUID NOT NULL REFERENCES skole(id) ON DELETE CASCADE,
    tema_id         UUID NOT NULL REFERENCES teme(id) ON DELETE CASCADE,
    redni_broj      SMALLINT,
    naziv           VARCHAR(500) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    CONSTRAINT uq_jedinice_naziv UNIQUE (skola_id, tema_id, naziv)
);

CREATE INDEX idx_jedinice_tema         ON nastavne_jedinice (tema_id);
CREATE INDEX idx_jedinice_naziv_search ON nastavne_jedinice (skola_id, tema_id, LOWER(naziv));

CREATE TABLE ishodi (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    skola_id        UUID NOT NULL REFERENCES skole(id) ON DELETE CASCADE,
    tema_id         UUID NOT NULL REFERENCES teme(id) ON DELETE CASCADE,
    opis            TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255)
);

CREATE INDEX idx_ishodi_tema ON ishodi (tema_id);

-- ----------------------------------------------------------------------------
-- PADAJUCI MENIJI: skola_id IS NULL znaci sistemska, dostupna svim skolama
-- ----------------------------------------------------------------------------

CREATE TABLE tipovi_casa (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    skola_id        UUID REFERENCES skole(id) ON DELETE CASCADE,
    naziv           VARCHAR(100) NOT NULL,
    aktivan         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_tipovi_casa_naziv ON tipovi_casa (COALESCE(skola_id::text, 'SISTEM'), LOWER(naziv));

CREATE TABLE metode_rada (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    skola_id        UUID REFERENCES skole(id) ON DELETE CASCADE,
    naziv           VARCHAR(100) NOT NULL,
    aktivan         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_metode_rada_naziv ON metode_rada (COALESCE(skola_id::text, 'SISTEM'), LOWER(naziv));
