ALTER TABLE rot_predmeti ALTER COLUMN profesor_id DROP NOT NULL;
ALTER TABLE rot_predmeti ADD COLUMN profesor_label VARCHAR(255);

UPDATE rot_predmeti rp
SET profesor_label = (SELECT k.ime || ' ' || k.prezime FROM korisnici k WHERE k.id = rp.profesor_id)
WHERE profesor_label IS NULL AND profesor_id IS NOT NULL;

ALTER TABLE rot_predmeti ALTER COLUMN profesor_label SET NOT NULL;
CREATE INDEX idx_rot_predmeti_label ON rot_predmeti (profesor_label);

ALTER TABLE rot_dodele ALTER COLUMN profesor_id DROP NOT NULL;
ALTER TABLE rot_dodele ADD COLUMN profesor_label VARCHAR(255);

UPDATE rot_dodele rd
SET profesor_label = (SELECT k.ime || ' ' || k.prezime FROM korisnici k WHERE k.id = rd.profesor_id)
WHERE profesor_label IS NULL AND profesor_id IS NOT NULL;

ALTER TABLE rot_dodele ALTER COLUMN profesor_label SET NOT NULL;
CREATE INDEX idx_rot_dodele_label ON rot_dodele (profesor_label);
