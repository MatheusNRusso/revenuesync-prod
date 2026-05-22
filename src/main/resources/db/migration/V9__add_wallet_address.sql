ALTER TABLE merchants
    ADD COLUMN wallet_address VARCHAR(64);

ALTER TABLE leads
    ADD COLUMN wallet_address VARCHAR(64);

CREATE UNIQUE INDEX IF NOT EXISTS uk_merchants_wallet_address
    ON merchants (wallet_address);

CREATE UNIQUE INDEX IF NOT EXISTS uk_leads_wallet_address
    ON leads (wallet_address);
