CREATE TABLE recycler_claims (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    recycler_id UUID NOT NULL,
    donation_id UUID NOT NULL,
    claimed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(50) NOT NULL,
    CONSTRAINT fk_recycler_claims_recycler FOREIGN KEY (recycler_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_recycler_claims_donation FOREIGN KEY (donation_id) REFERENCES donations(id) ON DELETE CASCADE,
    CONSTRAINT uc_recycler_claims_donation UNIQUE (donation_id)
);
