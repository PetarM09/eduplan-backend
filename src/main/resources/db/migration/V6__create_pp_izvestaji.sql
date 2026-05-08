-- ============================================================================
-- V6: PP (Pedagosko-psiholoska) sluzba — izvestaji starešina i statistika
-- ============================================================================

CREATE TABLE pp_izvestaji (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    skola_id            UUID NOT NULL REFERENCES skole(id) ON DELETE CASCADE,
    staresina_id        UUID NOT NULL REFERENCES korisnici(id) ON DELETE RESTRICT,
    odeljenje_id        UUID NOT NULL REFERENCES odeljenja(id) ON DELETE RESTRICT,
    period              VARCHAR(30) NOT NULL,
    skolska_godina      VARCHAR(9) NOT NULL,
    podaci_json         JSONB,
    status              VARCHAR(20) NOT NULL DEFAULT 'NACRT',
    podnet_at           TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),
    CONSTRAINT chk_pp_status CHECK (status IN ('NACRT','PODNET','PRIHVACEN','VRACENO_NA_DORADU')),
    CONSTRAINT chk_pp_period CHECK (period IN
        ('PRVO_TROMESECJE','PRVO_POLUGODISTE','TRECE_TROMESECJE','KRAJ_GODINE')),
    CONSTRAINT uq_pp_izvestaj UNIQUE (skola_id, odeljenje_id, period, skolska_godina)
);

CREATE INDEX idx_pp_skola         ON pp_izvestaji (skola_id);
CREATE INDEX idx_pp_period        ON pp_izvestaji (skola_id, period, skolska_godina);
CREATE INDEX idx_pp_staresina     ON pp_izvestaji (staresina_id);
