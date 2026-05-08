-- ============================================================================
-- V3: Odeljenja, verzije rasporeda, raspored stavke, zamene
-- ============================================================================

CREATE TABLE odeljenja (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    skola_id        UUID NOT NULL REFERENCES skole(id) ON DELETE CASCADE,
    razred          SMALLINT NOT NULL,
    oznaka          VARCHAR(5) NOT NULL,
    skolska_godina  VARCHAR(9) NOT NULL,
    staresina_id    UUID REFERENCES korisnici(id) ON DELETE SET NULL,
    aktivan         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    CONSTRAINT uq_odeljenja UNIQUE (skola_id, razred, oznaka, skolska_godina)
);

CREATE INDEX idx_odeljenja_skola_godina ON odeljenja (skola_id, skolska_godina);

-- FK iz V2 (predmet_odeljenja) prema odeljenjima — sada kada tabela postoji
ALTER TABLE predmet_odeljenja
    ADD CONSTRAINT fk_predmet_odeljenja_odeljenje
    FOREIGN KEY (odeljenje_id) REFERENCES odeljenja(id) ON DELETE CASCADE;

CREATE TABLE verzije_rasporeda (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    skola_id        UUID NOT NULL REFERENCES skole(id) ON DELETE CASCADE,
    naziv           VARCHAR(255),
    skolska_godina  VARCHAR(9),
    datum_od        DATE,
    aktivan         BOOLEAN NOT NULL DEFAULT FALSE,
    xml_original    TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255)
);

CREATE INDEX idx_verzije_skola ON verzije_rasporeda (skola_id);
CREATE UNIQUE INDEX uq_verzije_aktivan
    ON verzije_rasporeda (skola_id) WHERE aktivan = TRUE;

CREATE TABLE raspored_stavke (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    skola_id        UUID NOT NULL REFERENCES skole(id) ON DELETE CASCADE,
    verzija_id      UUID NOT NULL REFERENCES verzije_rasporeda(id) ON DELETE CASCADE,
    korisnik_id     UUID NOT NULL REFERENCES korisnici(id) ON DELETE CASCADE,
    odeljenje_id    UUID REFERENCES odeljenja(id) ON DELETE SET NULL,
    predmet_label   VARCHAR(255),
    dan             VARCHAR(15) NOT NULL,
    cas             SMALLINT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_raspored_dan CHECK (dan IN
        ('PONEDELJAK','UTORAK','SREDA','CETVRTAK','PETAK','SUBOTA')),
    CONSTRAINT chk_raspored_cas CHECK (cas BETWEEN 1 AND 8)
);

CREATE INDEX idx_raspored_verzija_korisnik ON raspored_stavke (verzija_id, korisnik_id);
CREATE INDEX idx_raspored_verzija_odeljenje ON raspored_stavke (verzija_id, odeljenje_id);
CREATE INDEX idx_raspored_skola ON raspored_stavke (skola_id);

CREATE TABLE zamene (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    skola_id        UUID NOT NULL REFERENCES skole(id) ON DELETE CASCADE,
    odsutni_id      UUID NOT NULL REFERENCES korisnici(id) ON DELETE CASCADE,
    zamenik_id      UUID REFERENCES korisnici(id) ON DELETE SET NULL,
    datum           DATE NOT NULL,
    cas             SMALLINT NOT NULL,
    odeljenje_id    UUID REFERENCES odeljenja(id) ON DELETE SET NULL,
    predmet_label   VARCHAR(255),
    razlog          VARCHAR(500),
    napomena        TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'PREDLOZENA',
    odobrio_id      UUID REFERENCES korisnici(id) ON DELETE SET NULL,
    odobrio_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    CONSTRAINT chk_zamena_status CHECK (status IN
        ('PREDLOZENA','ODOBRENA','ODBIJENA','OTKAZANA'))
);

CREATE INDEX idx_zamene_skola_datum ON zamene (skola_id, datum);
CREATE INDEX idx_zamene_odsutni ON zamene (odsutni_id);
CREATE INDEX idx_zamene_zamenik ON zamene (zamenik_id);
CREATE INDEX idx_zamene_status ON zamene (skola_id, status);
