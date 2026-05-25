ALTER TABLE artisan_requirements ADD COLUMN material VARCHAR(255) NOT NULL DEFAULT 'Material';
ALTER TABLE artisan_requirements ALTER COLUMN material DROP DEFAULT;

ALTER TABLE artisan_requirements ADD COLUMN purpose VARCHAR(1000) NOT NULL DEFAULT 'Purpose';
ALTER TABLE artisan_requirements ALTER COLUMN purpose DROP DEFAULT;

ALTER TABLE artisan_requirements ADD COLUMN urgency INT NOT NULL DEFAULT 3;
ALTER TABLE artisan_requirements ALTER COLUMN urgency DROP DEFAULT;

ALTER TABLE artisan_requirements ADD COLUMN radius_km DOUBLE PRECISION NOT NULL DEFAULT 10.0;
ALTER TABLE artisan_requirements ALTER COLUMN radius_km DROP DEFAULT;

ALTER TABLE artisan_requirements ADD COLUMN latitude DECIMAL(12, 9) NOT NULL DEFAULT 0.0;
ALTER TABLE artisan_requirements ALTER COLUMN latitude DROP DEFAULT;

ALTER TABLE artisan_requirements ADD COLUMN longitude DECIMAL(12, 9) NOT NULL DEFAULT 0.0;
ALTER TABLE artisan_requirements ALTER COLUMN longitude DROP DEFAULT;

ALTER TABLE artisan_requirements ALTER COLUMN title DROP NOT NULL;
ALTER TABLE artisan_requirements ALTER COLUMN category DROP NOT NULL;
ALTER TABLE artisan_requirements ALTER COLUMN fabric_type DROP NOT NULL;
