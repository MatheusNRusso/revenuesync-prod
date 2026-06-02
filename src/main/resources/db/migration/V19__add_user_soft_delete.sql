-- Add soft-delete support to users table.
-- active = false prevents login via Spring Security's isEnabled() check.
-- deleted_at records the exact moment the account was deactivated.

ALTER TABLE users
    ADD COLUMN active     BOOLEAN                  NOT NULL DEFAULT TRUE,
    ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE NULL;
