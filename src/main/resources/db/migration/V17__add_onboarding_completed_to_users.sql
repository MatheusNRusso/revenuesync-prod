-- V17__add_onboarding_completed_to_users.sql
--
-- Persists the user's onboarding intent so the frontend knows whether to
-- show the /onboarding choice screen or go directly to the correct dashboard.
--
-- false (default) = user has not yet chosen their intent (buyer vs merchant)
-- true            = user has completed onboarding and chosen a path

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN users.onboarding_completed IS
    'True once the user has completed the onboarding intent screen '
    '(chose buyer or merchant). Prevents the screen from reappearing on '
    'subsequent logins.';