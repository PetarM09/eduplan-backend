-- ============================================================================
-- V9: Redizajn rotacionog modula
-- Nova logika: rotacija se kreira za JEDNO odeljenje i odnosi se na vezbe
-- (termini gde 2+ profesora istovremeno predaju istom odeljenju). Korisnik
-- definise predmete po profesoru i broj casova nedeljno; algoritam mapira
-- grupe ucenika po terminima i nedeljama.
-- ============================================================================

DROP TABLE IF EXISTS rot_nedelje;
DROP TABLE IF EXISTS rot_konfiguracije;

-- Glavna tabela rotacije (jedna po odeljenju + nastavnik koji ju je napravio)
CREATE TABLE rotacije (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    skola_id            UUID NOT NULL REFERENCES skole(id) ON DELETE CASCADE,
    nastavnik_id        UUID NOT NULL REFERENCES korisnici(id) ON DELETE CASCADE,
    odeljenje_id        UUID NOT NULL REFERENCES odeljenja(id) ON DELETE CASCADE,
    naziv               VARCHAR(255) NOT NULL,
    broj_grupa          SMALLINT NOT NULL,
    broj_nedelja        SMALLINT NOT NULL,
    skolska_godina      VARCHAR(9) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),
    CONSTRAINT chk_rot_broj_grupa CHECK (broj_grupa BETWEEN 2 AND 12),
    CONSTRAINT chk_rot_broj_nedelja CHECK (broj_nedelja BETWEEN 1 AND 52)
);
CREATE INDEX idx_rotacije_skola_nastavnik ON rotacije (skola_id, nastavnik_id);
CREATE INDEX idx_rotacije_odeljenje ON rotacije (odeljenje_id);

-- Predmeti vezbi: po profesoru koji ima vezbe u odeljenju, jedan ili vise
-- predmeta sa brojem casova nedeljno. Suma casova mora == broj termina vezbi
-- tog profesora (proverava se u service-u, ne u DB).
CREATE TABLE rot_predmeti (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rotacija_id         UUID NOT NULL REFERENCES rotacije(id) ON DELETE CASCADE,
    profesor_id         UUID NOT NULL REFERENCES korisnici(id) ON DELETE CASCADE,
    naziv               VARCHAR(255) NOT NULL,
    casova_nedeljno     SMALLINT NOT NULL,
    redni_broj          SMALLINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_rot_predmeti_casova CHECK (casova_nedeljno BETWEEN 1 AND 20)
);
CREATE INDEX idx_rot_predmeti_rotacija ON rot_predmeti (rotacija_id);
CREATE INDEX idx_rot_predmeti_profesor ON rot_predmeti (profesor_id);

-- Generisane dodele: po nedelji, danu i casu, kod kog profesora/predmeta je
-- koja grupa. Algoritam popunjava prilikom kreiranja/regenerisanja rotacije.
CREATE TABLE rot_dodele (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rotacija_id         UUID NOT NULL REFERENCES rotacije(id) ON DELETE CASCADE,
    broj_nedelje        SMALLINT NOT NULL,
    dan                 VARCHAR(15) NOT NULL,
    cas                 SMALLINT NOT NULL,
    profesor_id         UUID NOT NULL REFERENCES korisnici(id) ON DELETE CASCADE,
    predmet_naziv       VARCHAR(255) NOT NULL,
    broj_grupe          SMALLINT NOT NULL,
    CONSTRAINT chk_rot_dodela_grupa CHECK (broj_grupe BETWEEN 1 AND 12)
);
CREATE INDEX idx_rot_dodele_rotacija_nedelja ON rot_dodele (rotacija_id, broj_nedelje);
CREATE INDEX idx_rot_dodele_termin ON rot_dodele (rotacija_id, broj_nedelje, dan, cas);
