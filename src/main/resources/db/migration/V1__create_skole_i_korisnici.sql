-- ============================================================================
-- V1: Multi-tenant osnovni entiteti — skole, korisnici, refresh tokeni
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE skole (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    naziv           VARCHAR(255) NOT NULL,
    grad            VARCHAR(100),
    adresa          VARCHAR(255),
    mail_planovi    VARCHAR(255),
    aktivan         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255)
);

CREATE UNIQUE INDEX uq_skole_naziv_grad ON skole (LOWER(naziv), LOWER(COALESCE(grad, '')));

CREATE TABLE korisnici (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    skola_id        UUID REFERENCES skole(id) ON DELETE RESTRICT,
    username        VARCHAR(100) NOT NULL,
    email           VARCHAR(255) NOT NULL,
    lozinka_hash    VARCHAR(255) NOT NULL,
    ime             VARCHAR(100) NOT NULL,
    prezime         VARCHAR(100) NOT NULL,
    uloga           VARCHAR(30)  NOT NULL,
    aktivan         BOOLEAN NOT NULL DEFAULT TRUE,
    poslednji_login TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    CONSTRAINT chk_korisnici_uloga CHECK (uloga IN
        ('SUPER_ADMIN','KOORDINATOR','DIREKTOR','ADMIN','PP_SLUZBA','NASTAVNIK')),
    CONSTRAINT chk_super_admin_skola CHECK (
        (uloga = 'SUPER_ADMIN' AND skola_id IS NULL) OR
        (uloga <> 'SUPER_ADMIN' AND skola_id IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uq_korisnici_username ON korisnici (LOWER(username));
CREATE UNIQUE INDEX uq_korisnici_email    ON korisnici (LOWER(email));
CREATE INDEX        idx_korisnici_skola   ON korisnici (skola_id);
CREATE INDEX        idx_korisnici_uloga   ON korisnici (skola_id, uloga);

CREATE TABLE refresh_tokens (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    korisnik_id     UUID NOT NULL REFERENCES korisnici(id) ON DELETE CASCADE,
    token_hash      VARCHAR(255) NOT NULL UNIQUE,
    expires_at      TIMESTAMPTZ  NOT NULL,
    revoked         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_korisnik ON refresh_tokens (korisnik_id);
CREATE INDEX idx_refresh_tokens_expires  ON refresh_tokens (expires_at) WHERE revoked = FALSE;
