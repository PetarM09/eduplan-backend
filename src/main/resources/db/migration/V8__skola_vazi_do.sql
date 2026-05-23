-- Datum isteka aktivacije skole. NULL = bez ogranicenja (default za postojece zapise).
ALTER TABLE skole ADD COLUMN vazi_do DATE;

COMMENT ON COLUMN skole.vazi_do IS
    'Datum nakon kog se skola automatski tretira kao deaktivirana. NULL = bez vremenskog ogranicenja.';
