CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       name VARCHAR(255) NOT NULL,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       role VARCHAR(50) NOT NULL,
                       created_at TIMESTAMP DEFAULT NOW(),
                       updated_at TIMESTAMP DEFAULT NOW()
);

ALTER TABLE merchants ADD COLUMN user_id BIGINT;
ALTER TABLE leads ADD COLUMN user_id BIGINT;

ALTER TABLE merchants ADD CONSTRAINT fk_merchants_user FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE leads ADD CONSTRAINT fk_leads_user FOREIGN KEY (user_id) REFERENCES users(id);

CREATE INDEX idx_merchants_user_id ON merchants(user_id);
CREATE INDEX idx_leads_user_id ON leads(user_id);
CREATE INDEX idx_users_email ON users(email);
