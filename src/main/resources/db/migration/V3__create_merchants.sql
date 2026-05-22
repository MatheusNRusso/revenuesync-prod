CREATE TABLE merchants (
                           id         BIGSERIAL PRIMARY KEY,
                           name       VARCHAR(100)        NOT NULL,
                           email      VARCHAR(100)        NOT NULL UNIQUE,
                           password   VARCHAR(255)        NOT NULL,
                           role       VARCHAR(20)         NOT NULL DEFAULT 'MERCHANT',
                           active     BOOLEAN             NOT NULL DEFAULT TRUE,
                           created_at TIMESTAMP           NOT NULL DEFAULT NOW(),
                           updated_at TIMESTAMP
);