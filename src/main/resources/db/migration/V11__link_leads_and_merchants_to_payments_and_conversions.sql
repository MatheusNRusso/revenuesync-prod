ALTER TABLE payments ADD COLUMN IF NOT EXISTS lead_id BIGINT;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS merchant_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_payments_lead'
    ) THEN
ALTER TABLE payments
    ADD CONSTRAINT fk_payments_lead
        FOREIGN KEY (lead_id) REFERENCES leads(id);
END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_payments_merchant'
    ) THEN
ALTER TABLE payments
    ADD CONSTRAINT fk_payments_merchant
        FOREIGN KEY (merchant_id) REFERENCES merchants(id);
END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_payments_lead_id ON payments(lead_id);

ALTER TABLE conversions ADD COLUMN IF NOT EXISTS lead_id BIGINT;
ALTER TABLE conversions ADD COLUMN IF NOT EXISTS merchant_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_conversions_lead'
    ) THEN
ALTER TABLE conversions
    ADD CONSTRAINT fk_conversions_lead
        FOREIGN KEY (lead_id) REFERENCES leads(id);
END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_conversions_merchant'
    ) THEN
ALTER TABLE conversions
    ADD CONSTRAINT fk_conversions_merchant
        FOREIGN KEY (merchant_id) REFERENCES merchants(id);
END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_conversions_lead_id ON conversions(lead_id);