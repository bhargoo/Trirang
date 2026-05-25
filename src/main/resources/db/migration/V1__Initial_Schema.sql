CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(50),
    role VARCHAR(50) NOT NULL,
    address VARCHAR(500),
    latitude DECIMAL(12, 9),
    longitude DECIMAL(12, 9),
    trust_score INT DEFAULT 100,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE donations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    donor_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    fabric_type VARCHAR(50) NOT NULL,
    classification VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    qr_code_path VARCHAR(500),
    image_path VARCHAR(500),
    latitude DECIMAL(12, 9),
    longitude DECIMAL(12, 9),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_donations_donor FOREIGN KEY (donor_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE artisan_requirements (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    artisan_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    fabric_type VARCHAR(50) NOT NULL,
    quantity_needed INT NOT NULL DEFAULT 1,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_artisan_requirements_artisan FOREIGN KEY (artisan_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE matches (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    donation_id UUID NOT NULL,
    requirement_id UUID NOT NULL,
    match_score DECIMAL(5, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_matches_donation FOREIGN KEY (donation_id) REFERENCES donations(id) ON DELETE CASCADE,
    CONSTRAINT fk_matches_requirement FOREIGN KEY (requirement_id) REFERENCES artisan_requirements(id) ON DELETE CASCADE
);
