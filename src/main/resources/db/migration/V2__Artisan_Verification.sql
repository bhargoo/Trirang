ALTER TABLE users ADD COLUMN verification_badge VARCHAR(50) DEFAULT 'NONE' NOT NULL;

CREATE TABLE artisan_verifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    artisan_id UUID NOT NULL,
    government_id_image_url VARCHAR(500) NOT NULL,
    selfie_image_url VARCHAR(500) NOT NULL,
    status VARCHAR(50) NOT NULL,
    rejection_reason VARCHAR(1000),
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_artisan_verifications_artisan FOREIGN KEY (artisan_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE artisan_verification_workspace_images (
    verification_id UUID NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    CONSTRAINT fk_workspace_images_verification FOREIGN KEY (verification_id) REFERENCES artisan_verifications(id) ON DELETE CASCADE
);
