-- Add github_user column to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS github_user BOOLEAN NOT NULL DEFAULT FALSE;

-- Mark existing GitHub OAuth users based on public profile github_username
UPDATE users u
SET github_user = TRUE
WHERE EXISTS (
    SELECT 1 FROM user_public_profiles p
    WHERE p.user_id = u.id
    AND p.github_username IS NOT NULL
    AND p.github_username != ''
);
