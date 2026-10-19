CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT chk_accounts_currency
                      CHECK ( currency IN ('COP', 'USD') ),

    CONSTRAINT chk_accounts_status
                      CHECK ( status IN ('ACTIVE', 'SUSPENDED', 'CLOSED') )
);

CREATE INDEX idx_accounts_customer_id
    ON accounts (customer_id);