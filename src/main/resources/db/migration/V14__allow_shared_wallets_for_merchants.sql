ALTER TABLE merchants
DROP CONSTRAINT IF EXISTS uk_merchants_wallet_address;

DROP INDEX IF EXISTS uk_merchants_wallet_address;