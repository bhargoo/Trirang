ALTER TABLE donations ADD COLUMN item_condition VARCHAR(255);
ALTER TABLE donations ADD COLUMN ai_analysis_json JSONB DEFAULT '{}'::jsonb;
