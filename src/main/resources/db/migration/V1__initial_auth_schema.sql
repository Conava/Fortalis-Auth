-- =============================================================================
-- Fortalis - Global Auth DB (V1)
-- One DB for the entire game across all regions and platforms.
-- Stores accounts, MFA, and external identities (Google/Apple).
-- =============================================================================

-- Accounts (email + optional local password)
CREATE TABLE IF NOT EXISTS account
(
    id             UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    email          VARCHAR(255) UNIQUE,
    password_hash  TEXT,
    email_verified BOOLEAN     NOT NULL DEFAULT FALSE,
    created_ts     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    display_name   VARCHAR(32)
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

-- Add backup codes table
CREATE TABLE IF NOT EXISTS mfa_backup_code
(
    id         UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    account_id UUID        NOT NULL REFERENCES account (id) ON DELETE CASCADE,
    code_hash  TEXT        NOT NULL,
    used       BOOLEAN     NOT NULL DEFAULT FALSE,
    created_ts TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_mfa_backup_account ON mfa_backup_code (account_id);

-- (Optional) Account preferences
CREATE TABLE IF NOT EXISTS account_settings
(
    account_id       UUID PRIMARY KEY REFERENCES account (id) ON DELETE CASCADE,
    lang             VARCHAR(8) NOT NULL DEFAULT 'en',
    marketing_opt_in BOOLEAN    NOT NULL DEFAULT FALSE,
    newsletter_opt_in BOOLEAN   NOT NULL DEFAULT FALSE,
    last_server      VARCHAR(32)
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

CREATE TABLE IF NOT EXISTS player_server
(
    id             UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    account_id     UUID        NOT NULL REFERENCES account (id) ON DELETE CASCADE,
    server_id      VARCHAR(32) NOT NULL REFERENCES server (id),
    created_ts     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_login_ts  TIMESTAMPTZ,
    CONSTRAINT uq_account_server UNIQUE (account_id, server_id)
    );
CREATE INDEX idx_player_server_account ON server_player (account_id);
CREATE INDEX idx_player_server_server ON server_player (server_id);

CREATE TABLE IF NOT EXISTS server
(
    id            VARCHAR(32) PRIMARY KEY, -- 'EU-DE', 'NA-US-EA', etc.
    region        VARCHAR(16) NOT NULL,    -- 'EU', 'NA', 'AP', etc.
    display_name  VARCHAR(64) NOT NULL,    -- 'Europe - Germany', 'North America - East'
    status        VARCHAR(16) NOT NULL DEFAULT 'ACTIVE', -- 'ACTIVE', 'MAINTENANCE', 'RETIRED'
    player_count  INT,
    created_ts    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
