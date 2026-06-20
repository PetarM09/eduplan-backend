-- ============================================================================
-- V16: Blob storage za generisane planove (.docx/.pdf) u bazi.
-- Heroku ima efemeran fajl-sistem (disk se brise na restart dina), pa se
-- fajlovi cuvaju u bazi umesto na disku. Zasebna lean tabela da list-upiti
-- nad planovima ne povlace bajtove.
-- ============================================================================

CREATE TABLE plan_fajl (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    skola_id    UUID NOT NULL REFERENCES skole(id) ON DELETE CASCADE,
    plan_tip    VARCHAR(20) NOT NULL,
    plan_id     UUID NOT NULL,
    fajl_tip    VARCHAR(10) NOT NULL,
    sadrzaj     BYTEA NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255),
    CONSTRAINT chk_plan_fajl_plan_tip CHECK (plan_tip IN ('GODISNJI','OPERATIVNI')),
    CONSTRAINT chk_plan_fajl_fajl_tip CHECK (fajl_tip IN ('WORD','PDF')),
    CONSTRAINT uq_plan_fajl UNIQUE (plan_tip, plan_id, fajl_tip)
);

CREATE INDEX idx_plan_fajl_lookup ON plan_fajl (plan_tip, plan_id);

UPDATE godisnji_planovi   SET word_fajl_putanja = NULL, pdf_fajl_putanja = NULL;
UPDATE operativni_planovi SET word_fajl_putanja = NULL, pdf_fajl_putanja = NULL;
