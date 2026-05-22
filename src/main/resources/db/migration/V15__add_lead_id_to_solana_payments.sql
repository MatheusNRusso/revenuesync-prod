ALTER TABLE solana_payments
    ADD COLUMN IF NOT EXISTS lead_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_solana_payments_lead_id
    ON solana_payments(lead_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_solana_payments_lead'
          AND table_name = 'solana_payments'
    ) THEN
ALTER TABLE solana_payments
    ADD CONSTRAINT fk_solana_payments_lead
        FOREIGN KEY (lead_id)
            REFERENCES leads(id);
END IF;
END $$;