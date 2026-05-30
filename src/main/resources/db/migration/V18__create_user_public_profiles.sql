-- V18: public developer profiles for the Discover marketplace
-- Stores GitHub-enriched public metadata. One profile per user, optional.
-- No GitHub tokens stored — public data only.

CREATE TABLE user_public_profiles (
    id                  BIGSERIAL    PRIMARY KEY,
    user_id             BIGINT       NOT NULL UNIQUE
                                     REFERENCES users(id) ON DELETE CASCADE,

    slug                VARCHAR(120) NOT NULL UNIQUE,
    display_name        VARCHAR(255),
    headline            VARCHAR(255),
    bio                 TEXT,
    location            VARCHAR(255),
    website_url         VARCHAR(500),

    category            VARCHAR(100),
    tags                TEXT,

    github_username     VARCHAR(255),
    github_avatar_url   VARCHAR(500),
    github_profile_url  VARCHAR(500),
    github_public_repos INTEGER      DEFAULT 0,
    github_followers    INTEGER      DEFAULT 0,

    is_public           BOOLEAN      NOT NULL DEFAULT false,

    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_upp_slug      ON user_public_profiles(slug);
CREATE INDEX        idx_upp_user_id   ON user_public_profiles(user_id);
CREATE INDEX        idx_upp_category  ON user_public_profiles(category);
CREATE INDEX        idx_upp_is_public ON user_public_profiles(is_public);
