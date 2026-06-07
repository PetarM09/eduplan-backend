CREATE TABLE tip_skole (
    id UUID PRIMARY KEY,
    kod VARCHAR(40) NOT NULL UNIQUE,
    naziv VARCHAR(100) NOT NULL,
    ukupno_razreda SMALLINT NOT NULL CHECK (ukupno_razreda BETWEEN 1 AND 12),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE TABLE obrazovni_profil (
    id UUID PRIMARY KEY,
    tip_skole_id UUID NOT NULL REFERENCES tip_skole(id) ON DELETE RESTRICT,
    kod VARCHAR(120) NOT NULL UNIQUE,
    naziv VARCHAR(200) NOT NULL,
    opis TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);
CREATE INDEX idx_obrazovni_profil_tip ON obrazovni_profil(tip_skole_id);

CREATE TABLE master_predmet (
    id UUID PRIMARY KEY,
    obrazovni_profil_id UUID NOT NULL REFERENCES obrazovni_profil(id) ON DELETE CASCADE,
    razred SMALLINT NOT NULL CHECK (razred BETWEEN 1 AND 12),
    naziv VARCHAR(200) NOT NULL,
    fond_teorija SMALLINT NOT NULL DEFAULT 0 CHECK (fond_teorija >= 0),
    fond_vezbe SMALLINT NOT NULL DEFAULT 0 CHECK (fond_vezbe >= 0),
    fond_blok SMALLINT NOT NULL DEFAULT 0 CHECK (fond_blok >= 0),
    obavezan BOOLEAN NOT NULL DEFAULT TRUE,
    redosled SMALLINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT master_predmet_fond_check CHECK (fond_teorija + fond_vezbe + fond_blok > 0),
    CONSTRAINT master_predmet_uniq UNIQUE (obrazovni_profil_id, razred, naziv)
);
CREATE INDEX idx_master_predmet_profil ON master_predmet(obrazovni_profil_id);

-- Seed: tipovi skole
INSERT INTO tip_skole (id, kod, naziv, ukupno_razreda, created_at) VALUES
    (gen_random_uuid(), 'OSNOVNA', 'Osnovna skola', 8, now()),
    (gen_random_uuid(), 'GIMNAZIJA', 'Gimnazija', 4, now()),
    (gen_random_uuid(), 'SREDNJA_STRUCNA', 'Srednja strucna skola', 4, now()),
    (gen_random_uuid(), 'UMETNICKA', 'Umetnicka skola', 4, now());

-- Seed: demo profil "Gimnazija - opsti smer" sa nekoliko predmeta po razredu.
-- Sluzi samo kao primer; pravi obrazovni profili se dodaju kroz SUPER_ADMIN UI
-- na osnovu Prosvetnog glasnika.
DO $$
DECLARE
    v_tip_id UUID;
    v_profil_id UUID;
BEGIN
    SELECT id INTO v_tip_id FROM tip_skole WHERE kod = 'GIMNAZIJA';
    v_profil_id := gen_random_uuid();
    INSERT INTO obrazovni_profil (id, tip_skole_id, kod, naziv, opis, created_at)
    VALUES (v_profil_id, v_tip_id, 'GIMNAZIJA_OPSTI', 'Gimnazija — opsti smer',
            'Demo profil. Predmeti su uneti kao primer i moraju se zameniti realnim podacima iz Prosvetnog glasnika.', now());

    INSERT INTO master_predmet (id, obrazovni_profil_id, razred, naziv,
        fond_teorija, fond_vezbe, fond_blok, obavezan, redosled, created_at)
    VALUES
        (gen_random_uuid(), v_profil_id, 1, 'Srpski jezik i knjizevnost', 3, 1, 0, true, 1, now()),
        (gen_random_uuid(), v_profil_id, 1, 'Engleski jezik',              2, 1, 0, true, 2, now()),
        (gen_random_uuid(), v_profil_id, 1, 'Matematika',                  3, 1, 0, true, 3, now()),
        (gen_random_uuid(), v_profil_id, 1, 'Fizika',                      2, 0, 0, true, 4, now()),
        (gen_random_uuid(), v_profil_id, 1, 'Biologija',                   2, 0, 0, true, 5, now()),
        (gen_random_uuid(), v_profil_id, 1, 'Fizicko vaspitanje',          0, 2, 0, true, 6, now()),
        (gen_random_uuid(), v_profil_id, 2, 'Srpski jezik i knjizevnost', 3, 1, 0, true, 1, now()),
        (gen_random_uuid(), v_profil_id, 2, 'Engleski jezik',              2, 1, 0, true, 2, now()),
        (gen_random_uuid(), v_profil_id, 2, 'Matematika',                  3, 1, 0, true, 3, now()),
        (gen_random_uuid(), v_profil_id, 2, 'Fizika',                      2, 0, 0, true, 4, now()),
        (gen_random_uuid(), v_profil_id, 2, 'Hemija',                      2, 0, 0, true, 5, now()),
        (gen_random_uuid(), v_profil_id, 2, 'Fizicko vaspitanje',          0, 2, 0, true, 6, now()),
        (gen_random_uuid(), v_profil_id, 3, 'Srpski jezik i knjizevnost', 3, 1, 0, true, 1, now()),
        (gen_random_uuid(), v_profil_id, 3, 'Engleski jezik',              2, 1, 0, true, 2, now()),
        (gen_random_uuid(), v_profil_id, 3, 'Matematika',                  3, 1, 0, true, 3, now()),
        (gen_random_uuid(), v_profil_id, 3, 'Sociologija',                 2, 0, 0, true, 4, now()),
        (gen_random_uuid(), v_profil_id, 3, 'Fizicko vaspitanje',          0, 2, 0, true, 5, now()),
        (gen_random_uuid(), v_profil_id, 4, 'Srpski jezik i knjizevnost', 4, 0, 0, true, 1, now()),
        (gen_random_uuid(), v_profil_id, 4, 'Engleski jezik',              2, 1, 0, true, 2, now()),
        (gen_random_uuid(), v_profil_id, 4, 'Matematika',                  4, 0, 0, true, 3, now()),
        (gen_random_uuid(), v_profil_id, 4, 'Filozofija',                  2, 0, 0, true, 4, now()),
        (gen_random_uuid(), v_profil_id, 4, 'Fizicko vaspitanje',          0, 2, 0, true, 5, now());
END $$;
