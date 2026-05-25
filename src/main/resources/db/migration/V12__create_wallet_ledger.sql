CREATE TABLE tricoin_ledger (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    amount INTEGER NOT NULL,
    balance_after_transaction INTEGER NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    reason TEXT,
    reference_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_tricoin_ledger_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_tricoin_ledger_user_id ON tricoin_ledger(user_id);
CREATE INDEX idx_tricoin_ledger_created_at ON tricoin_ledger(created_at DESC);
