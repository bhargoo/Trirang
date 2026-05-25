CREATE TABLE reviews (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reviewer_id UUID NOT NULL,
    reviewed_user_id UUID NOT NULL,
    related_match_id UUID NOT NULL,
    rating INTEGER NOT NULL,
    comment VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_reviews_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_reviewed_user FOREIGN KEY (reviewed_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_match FOREIGN KEY (related_match_id) REFERENCES matches(id) ON DELETE CASCADE,
    CONSTRAINT uc_reviews_reviewer_match UNIQUE (reviewer_id, related_match_id)
);

CREATE INDEX idx_reviews_reviewed_user ON reviews(reviewed_user_id);
CREATE INDEX idx_reviews_match ON reviews(related_match_id);
