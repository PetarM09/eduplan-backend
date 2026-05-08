-- ============================================================================
-- V7: Sistemski (skola_id IS NULL) tipovi casa i metode rada — dostupni svim skolama.
-- Pocetni SUPER_ADMIN nalog se kreira programski iz CommandLineRunner-a
-- (SuperAdminBootstrap), tako da BCrypt hash zavisi od env-varijable a ne od SQL-a.
-- ============================================================================

INSERT INTO tipovi_casa (skola_id, naziv, aktivan) VALUES
    (NULL, 'Obrada',          TRUE),
    (NULL, 'Utvrdjivanje',    TRUE),
    (NULL, 'Ponavljanje',     TRUE),
    (NULL, 'Provera znanja',  TRUE),
    (NULL, 'Sistematizacija', TRUE),
    (NULL, 'Kombinovani',     TRUE);

INSERT INTO metode_rada (skola_id, naziv, aktivan) VALUES
    (NULL, 'Predavanje',                TRUE),
    (NULL, 'Demonstracija',             TRUE),
    (NULL, 'Grupni rad',                TRUE),
    (NULL, 'Rad u parovima',            TRUE),
    (NULL, 'Istrazivacki rad',          TRUE),
    (NULL, 'Diskusija',                 TRUE),
    (NULL, 'Usmeno izlaganje',          TRUE),
    (NULL, 'Prakticna vezba',           TRUE),
    (NULL, 'Problemska nastava',        TRUE),
    (NULL, 'Heuristicki razgovor',      TRUE);
