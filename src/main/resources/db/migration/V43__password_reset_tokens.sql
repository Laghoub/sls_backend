-- Jetons de réinitialisation : hash SHA-256 uniquement, expiration et usage unique.
CREATE TABLE password_reset_token (
    id BIGSERIAL PRIMARY KEY,
    user_account_id BIGINT NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ
);
CREATE INDEX idx_password_reset_token_user_created ON password_reset_token(user_account_id,created_at DESC);
