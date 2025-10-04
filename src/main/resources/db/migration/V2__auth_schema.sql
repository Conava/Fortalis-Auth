-- =============================================================================
-- Fortalis - Global Auth DB (V1)
-- One DB for the entire game across all regions and platforms.
-- Stores accounts, MFA, and external identities (Google/Apple).
-- =============================================================================

-- Accounts (email + optional local password)
CREATE TABLE IF NOT EXISTS account
(
    id             UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    email          CITEXT UNIQUE, -- may be NULL for pure social
    password_hash  TEXT,          -- Argon2id/bcrypt; NULL if social-only
    email_verified BOOLEAN     NOT NULL DEFAULT FALSE,
    created_ts     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    display_name   VARCHAR(32)    -- preferred username (not unique globally)
);

-- External or local identities mapped to one account
-- provider: 'password' | 'google' | 'apple'
CREATE TABLE IF NOT EXISTS account_identity
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID         NOT NULL REFERENCES account (id) ON DELETE CASCADE,
    provider   VARCHAR(16)  NOT NULL,
    subject    VARCHAR(255) NOT NULL, -- OIDC sub or local username id
    UNIQUE (provider, subject)
);

-- MFA: either TOTP or SMS (one row per account; extend if you need multiples)
CREATE TABLE IF NOT EXISTS account_mfa
(
    account_id UUID PRIMARY KEY REFERENCES account (id) ON DELETE CASCADE,
    type       VARCHAR(8) NOT NULL, -- 'TOTP' | 'SMS'
    secret     TEXT,                -- TOTP secret (encrypt at rest)
    phone_e164 VARCHAR(20),         -- +49170...
    enabled    BOOLEAN    NOT NULL DEFAULT FALSE
);

-- (Optional) Account preferences
CREATE TABLE IF NOT EXISTS account_settings
(
    account_id       UUID PRIMARY KEY REFERENCES account (id) ON DELETE CASCADE,
    lang             VARCHAR(8) NOT NULL DEFAULT 'en',
    marketing_opt_in BOOLEAN    NOT NULL DEFAULT FALSE
);
CREATE TABLE IF NOT EXISTS refresh_token
(
    id           UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    account_id   UUID        NOT NULL REFERENCES account (id) ON DELETE CASCADE,
    token_hash   TEXT        NOT NULL, -- hash of refresh token
    issued_ts    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_ts   TIMESTAMPTZ NOT NULL,
    user_agent   TEXT,
    device_label TEXT,
    revoked      BOOLEAN     NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_refresh_account ON refresh_token (account_id);

