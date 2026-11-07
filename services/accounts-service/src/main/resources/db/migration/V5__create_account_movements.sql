CREATE TABLE account_movements (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    operation_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_accounts_movements_account
                               FOREIGN KEY (account_id)
                               REFERENCES accounts(id),

    CONSTRAINT uq_accounts_movements_operation
                               UNIQUE (operation_id),

    CONSTRAINT chk_accounts_movements_operation
                               CHECK ( amount > 0 ),

    CONSTRAINT chk_accounts_movements_type
                               CHECK ( type IN ('CREDIT', 'DEBIT'))
);