CREATE EXTENSION IF NOT EXISTS postgis;

ALTER TABLE users ADD COLUMN location geometry(Point, 4326);

UPDATE users 
SET location = ST_SetSRID(ST_MakePoint(longitude, latitude), 4326) 
WHERE longitude IS NOT NULL AND latitude IS NOT NULL;

CREATE INDEX idx_users_location ON users USING GIST (location);
