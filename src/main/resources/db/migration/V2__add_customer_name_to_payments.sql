-- V2__add_customer_name_to_payments.sql
-- Adds customer_name column to payments table

ALTER TABLE payments
ADD COLUMN IF NOT EXISTS customer_name VARCHAR(255);

-- Optional: Add index for customer_name if you'll query by it
-- CREATE INDEX IF NOT EXISTS idx_payments_customer_name ON payments(customer_name);

-- Optional: Update existing records with customer_name from raw_payload
-- This is a complex JSON extraction that might be better done in application code
