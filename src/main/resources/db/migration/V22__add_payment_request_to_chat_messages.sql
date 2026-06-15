ALTER TABLE chat_messages ADD COLUMN message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT';
ALTER TABLE chat_messages ADD COLUMN payment_token VARCHAR(100) NULL;
ALTER TABLE chat_messages ADD COLUMN payment_amount_sol DECIMAL(19,9) NULL;
ALTER TABLE chat_messages ADD COLUMN payment_status VARCHAR(20) NULL DEFAULT 'PENDING';

CREATE UNIQUE INDEX idx_chat_messages_payment_token 
  ON chat_messages(payment_token) 
  WHERE payment_token IS NOT NULL;
