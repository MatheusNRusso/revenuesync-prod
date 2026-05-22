-- src/main/resources/db/migration/V7__add_currency_to_conversions.sql

ALTER TABLE conversions ADD COLUMN currency VARCHAR(10);

UPDATE conversions c
SET currency = p.currency
    FROM payments p
WHERE c.payment_id = p.id;