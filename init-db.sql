-- Create schemas
CREATE SCHEMA IF NOT EXISTS public;

-- Transaction table (Service A)
CREATE TABLE IF NOT EXISTS transactions (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(36) UNIQUE NOT NULL,
    sender_id VARCHAR(100) NOT NULL,
    receiver_id VARCHAR(100) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    idempotency_key VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    error_reason TEXT
);

CREATE INDEX idx_transaction_id ON transactions(transaction_id);
CREATE INDEX idx_sender_id ON transactions(sender_id);
CREATE INDEX idx_receiver_id ON transactions(receiver_id);
CREATE INDEX idx_status ON transactions(status);
CREATE INDEX idx_created_at ON transactions(created_at);

-- Accounts table (Service B - Ledger)
CREATE TABLE IF NOT EXISTS accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(100) UNIQUE NOT NULL,
    balance NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    version BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX idx_user_id ON accounts(user_id);

-- Ledger entries table (Service B - Ledger)
CREATE TABLE IF NOT EXISTS ledger_entries (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(36) UNIQUE NOT NULL,
    user_id VARCHAR(100) NOT NULL,
    account_id BIGINT NOT NULL,
    debit NUMERIC(19, 2),
    credit NUMERIC(19, 2),
    balance_after NUMERIC(19, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_transaction_id_ledger ON ledger_entries(transaction_id);
CREATE INDEX idx_user_id_ledger ON ledger_entries(user_id);
CREATE INDEX idx_created_at_ledger ON ledger_entries(created_at);

-- Analytics table (Service C)
CREATE TABLE IF NOT EXISTS payment_analytics (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(36) UNIQUE NOT NULL,
    sender_id VARCHAR(100) NOT NULL,
    receiver_id VARCHAR(100) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    completed_at TIMESTAMP NOT NULL,
    ingested_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_transaction_id_analytics ON payment_analytics(transaction_id);
CREATE INDEX idx_completed_at ON payment_analytics(completed_at);

-- Initial user accounts
INSERT INTO accounts (user_id, balance, currency, version, created_at, updated_at)
VALUES
    ('user001', 10000.00, 'USD', 0, NOW(), NOW()),
    ('user002', 5000.00, 'USD', 0, NOW(), NOW()),
    ('user003', 15000.00, 'USD', 0, NOW(), NOW()),
    ('user004', 8000.00, 'USD', 0, NOW(), NOW()),
    ('user005', 12000.00, 'USD', 0, NOW(), NOW())
ON CONFLICT (user_id) DO NOTHING;

-- Kafka topics will be auto-created
COMMIT;
