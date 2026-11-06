ALTER TABLE accounts
ADD CONSTRAINT chk_accounts_balance_non_negative
CHECK ( balance >= 0 );