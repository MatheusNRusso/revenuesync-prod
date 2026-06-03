-- Add email verification support to users table.
-- email_verified = false until user confirms via token.
-- verification_token is a 6-digit code sent by email.
-- token_expires_at prevents token reuse after expiration.

ALTER TABLE users
    ADD COLUMN email_verified      BOOLEAN                  NOT NULL DEFAULT FALSE,
    ADD COLUMN verification_token  VARCHAR(6)               NULL,
    ADD COLUMN token_expires_at    TIMESTAMP WITH TIME ZONE NULL;

-- Existing users are considered verified (they registered before this feature).
UPDATE users SET email_verified = TRUE;
