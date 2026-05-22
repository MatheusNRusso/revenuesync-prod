ALTER TABLE merchants ADD COLUMN slug VARCHAR(100) UNIQUE;
ALTER TABLE merchants ADD COLUMN description VARCHAR(500);
ALTER TABLE merchants ADD COLUMN avatar_url VARCHAR(500);

UPDATE merchants
SET slug = LOWER(REGEXP_REPLACE(name, '[^a-zA-Z0-9]', '-', 'g'))
WHERE slug IS NULL;