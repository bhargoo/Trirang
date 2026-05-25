CREATE TABLE transformation_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    donor_id UUID NOT NULL,
    artisan_id UUID NOT NULL,
    donation_id UUID NOT NULL,
    customization_request TEXT,
    quoted_price DECIMAL(10, 2),
    progress VARCHAR(50) NOT NULL,
    before_images JSONB DEFAULT '[]'::jsonb,
    after_images JSONB DEFAULT '[]'::jsonb,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_transformation_requests_donor FOREIGN KEY (donor_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_transformation_requests_artisan FOREIGN KEY (artisan_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_transformation_requests_donation FOREIGN KEY (donation_id) REFERENCES donations(id) ON DELETE CASCADE
);

CREATE INDEX idx_transformation_requests_donor ON transformation_requests(donor_id);
CREATE INDEX idx_transformation_requests_artisan ON transformation_requests(artisan_id);
CREATE INDEX idx_transformation_requests_progress ON transformation_requests(progress);
