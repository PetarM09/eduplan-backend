-- Skolski Predmet dobija isti T+V+B model kao Master Predmet.
-- Postojeci `fond_casova` ostaje (godisnji fond, slobodan za override).
ALTER TABLE predmeti
    ADD COLUMN fond_teorija SMALLINT NOT NULL DEFAULT 0,
    ADD COLUMN fond_vezbe SMALLINT NOT NULL DEFAULT 0,
    ADD COLUMN fond_blok SMALLINT NOT NULL DEFAULT 0,
    ADD COLUMN master_predmet_id UUID REFERENCES master_predmet(id) ON DELETE SET NULL;

CREATE INDEX idx_predmeti_master ON predmeti(master_predmet_id);

-- Skola pamti koje obrazovne profile je izabrala kroz onboarding wizard.
-- Lista UUID-ova MasterProfila — koristi se kasnije za "predloze nove predmete
-- iz tvojih profila" funkciju i pri promeni profila.
ALTER TABLE skole
    ADD COLUMN obrazovni_profili_ids JSONB NOT NULL DEFAULT '[]'::jsonb;
