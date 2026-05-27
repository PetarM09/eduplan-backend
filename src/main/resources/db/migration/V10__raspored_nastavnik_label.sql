-- ============================================================================
-- V10: Raspored cuva ime nastavnika iz XML-a (label), bez obzira na to da li
-- postoji odgovarajuci korisnicki nalog u sistemu. To omogucava da rotacija
-- vidi sve profesore vezbi iz rasporeda i pre nego sto su uneti kao korisnici.
-- ============================================================================

-- Korisnik je sad opcionalan
ALTER TABLE raspored_stavke ALTER COLUMN korisnik_id DROP NOT NULL;

-- Nova kolona sa labelom iz XML-a (uvek prisutna)
ALTER TABLE raspored_stavke ADD COLUMN nastavnik_label VARCHAR(255);

-- Backfill: postojece stavke uzimaju ime sa povezanog korisnika
UPDATE raspored_stavke rs
SET nastavnik_label = (
    SELECT k.ime || ' ' || k.prezime
    FROM korisnici k
    WHERE k.id = rs.korisnik_id
)
WHERE nastavnik_label IS NULL AND korisnik_id IS NOT NULL;

-- Sad obavezna kolona za sve naredne upise
ALTER TABLE raspored_stavke ALTER COLUMN nastavnik_label SET NOT NULL;

CREATE INDEX idx_raspored_stavke_label ON raspored_stavke (nastavnik_label);
