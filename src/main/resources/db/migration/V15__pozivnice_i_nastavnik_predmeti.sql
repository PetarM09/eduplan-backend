-- Pozivnice za nastavnike: nalog je POZVAN (login blokiran) sve dok korisnik
-- ne klikne magic-link i postavi sifru. Tada se pozivnica_token brise.
ALTER TABLE korisnici
    ALTER COLUMN lozinka_hash DROP NOT NULL;

ALTER TABLE korisnici
    ADD COLUMN pozivnica_token UUID UNIQUE,
    ADD COLUMN pozivnica_istice TIMESTAMP WITH TIME ZONE,
    ADD COLUMN poreklo VARCHAR(20) NOT NULL DEFAULT 'RUCNO';

CREATE INDEX idx_korisnici_pozivnica ON korisnici(pozivnica_token);

-- Veza nastavnik <-> predmet (koje predmete predaje). Postavlja koordinator
-- na ekranu Pozivnice (XML/Excel ne daje ovo automatski).
CREATE TABLE nastavnik_predmeti (
    nastavnik_id UUID NOT NULL REFERENCES korisnici(id) ON DELETE CASCADE,
    predmet_id UUID NOT NULL REFERENCES predmeti(id) ON DELETE CASCADE,
    PRIMARY KEY (nastavnik_id, predmet_id)
);
CREATE INDEX idx_nastavnik_predmeti_predmet ON nastavnik_predmeti(predmet_id);
