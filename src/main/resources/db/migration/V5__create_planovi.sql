-- ============================================================================
-- V5: Godisnji (globalni) i operativni planovi rada
-- ============================================================================

-- ----------------------------------------------------------------------------
-- GODISNJI (GLOBALNI) PLAN RADA
-- ----------------------------------------------------------------------------
CREATE TABLE godisnji_planovi (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    skola_id            UUID NOT NULL REFERENCES skole(id) ON DELETE CASCADE,
    nastavnik_id        UUID NOT NULL REFERENCES korisnici(id) ON DELETE CASCADE,
    predmet_id          UUID NOT NULL REFERENCES predmeti(id) ON DELETE RESTRICT,
    skolska_godina      VARCHAR(9) NOT NULL,
    razred              SMALLINT,
    odeljenja_json      JSONB,
    ciljevi_zadaci      TEXT,
    udzebenik           VARCHAR(500),
    autori              VARCHAR(500),
    literatura          TEXT,
    godisnji_fond       SMALLINT,
    nedeljni_fond       SMALLINT,
    dopunski_rad        TEXT,
    dodatni_rad         TEXT,
    napomene            TEXT,
    status              VARCHAR(20) NOT NULL DEFAULT 'NACRT',
    word_fajl_putanja   VARCHAR(500),
    pdf_fajl_putanja    VARCHAR(500),
    podnet_at           TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),
    CONSTRAINT chk_godisnji_status CHECK (status IN ('NACRT','PODNET','VRACENO_NA_DORADU','ARHIVIRAN')),
    CONSTRAINT uq_godisnji_plan UNIQUE (skola_id, nastavnik_id, predmet_id, skolska_godina)
);

CREATE INDEX idx_godisnji_skola_godina ON godisnji_planovi (skola_id, skolska_godina);
CREATE INDEX idx_godisnji_nastavnik    ON godisnji_planovi (nastavnik_id);
CREATE INDEX idx_godisnji_status       ON godisnji_planovi (skola_id, status);

CREATE TABLE godisnji_plan_teme (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    godisnji_plan_id    UUID NOT NULL REFERENCES godisnji_planovi(id) ON DELETE CASCADE,
    tema_id             UUID NOT NULL REFERENCES teme(id) ON DELETE RESTRICT,
    redni_broj          SMALLINT NOT NULL DEFAULT 0,
    meseci_json         JSONB,
    cas_obrada          SMALLINT DEFAULT 0,
    cas_utvrd           SMALLINT DEFAULT 0,
    cas_ostalo          SMALLINT DEFAULT 0,
    ukupno_casova       SMALLINT DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),
    CONSTRAINT uq_god_plan_tema UNIQUE (godisnji_plan_id, tema_id)
);

CREATE INDEX idx_god_plan_teme_plan ON godisnji_plan_teme (godisnji_plan_id);

-- ----------------------------------------------------------------------------
-- OPERATIVNI PLAN RADA
-- ----------------------------------------------------------------------------
CREATE TABLE operativni_planovi (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    skola_id            UUID NOT NULL REFERENCES skole(id) ON DELETE CASCADE,
    nastavnik_id        UUID NOT NULL REFERENCES korisnici(id) ON DELETE CASCADE,
    predmet_id          UUID NOT NULL REFERENCES predmeti(id) ON DELETE RESTRICT,
    odeljenje_id        UUID NOT NULL REFERENCES odeljenja(id) ON DELETE RESTRICT,
    mesec               SMALLINT NOT NULL CHECK (mesec BETWEEN 1 AND 12),
    skolska_godina      VARCHAR(9) NOT NULL,
    nedeljni_fond       SMALLINT,
    samoprocena_ishoda  TEXT,
    napomene            TEXT,
    status              VARCHAR(20) NOT NULL DEFAULT 'NACRT',
    word_fajl_putanja   VARCHAR(500),
    pdf_fajl_putanja    VARCHAR(500),
    podnet_at           TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),
    CONSTRAINT chk_operativni_status CHECK (status IN ('NACRT','PODNET','VRACENO_NA_DORADU','ARHIVIRAN')),
    CONSTRAINT uq_operativni_plan UNIQUE (skola_id, nastavnik_id, predmet_id, odeljenje_id, mesec, skolska_godina)
);

CREATE INDEX idx_operativni_skola_godina ON operativni_planovi (skola_id, skolska_godina);
CREATE INDEX idx_operativni_nastavnik    ON operativni_planovi (nastavnik_id);
CREATE INDEX idx_operativni_mesec        ON operativni_planovi (skola_id, mesec, skolska_godina);

CREATE TABLE op_stavke (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    operativni_plan_id      UUID NOT NULL REFERENCES operativni_planovi(id) ON DELETE CASCADE,
    redni_broj_casa         SMALLINT NOT NULL,
    tema_id                 UUID REFERENCES teme(id) ON DELETE SET NULL,
    nastavna_jedinica_id    UUID REFERENCES nastavne_jedinice(id) ON DELETE SET NULL,
    tip_casa_id             UUID REFERENCES tipovi_casa(id) ON DELETE SET NULL,
    metoda_rada_id          UUID REFERENCES metode_rada(id) ON DELETE SET NULL,
    evaluacija              TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ,
    created_by              VARCHAR(255),
    updated_by              VARCHAR(255),
    CONSTRAINT uq_op_stavke_red UNIQUE (operativni_plan_id, redni_broj_casa)
);

CREATE INDEX idx_op_stavke_plan ON op_stavke (operativni_plan_id);

CREATE TABLE op_stavka_ishodi (
    op_stavka_id    UUID NOT NULL REFERENCES op_stavke(id) ON DELETE CASCADE,
    ishod_id        UUID NOT NULL REFERENCES ishodi(id) ON DELETE CASCADE,
    PRIMARY KEY (op_stavka_id, ishod_id)
);

CREATE TABLE op_stavka_medjupredmetno (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    op_stavka_id        UUID NOT NULL REFERENCES op_stavke(id) ON DELETE CASCADE,
    predmet_id          UUID NOT NULL REFERENCES predmeti(id) ON DELETE CASCADE,
    opis_kompetencije   TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255)
);

CREATE INDEX idx_op_medjupredmetno_stavka ON op_stavka_medjupredmetno (op_stavka_id);
