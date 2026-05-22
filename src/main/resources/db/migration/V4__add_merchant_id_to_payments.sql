ALTER TABLE payments
    ADD COLUMN merchant_id BIGINT NOT NULL REFERENCES merchants(id);

CREATE INDEX idx_payments_merchant_id ON payments(merchant_id);