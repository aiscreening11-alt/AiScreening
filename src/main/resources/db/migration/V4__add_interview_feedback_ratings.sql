-- Migration V4: Add Interviewer Ratings, Criteria Breakdown, and Portal Access Token

ALTER TABLE interviews ADD COLUMN IF NOT EXISTS technical_rating INTEGER;
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS communication_rating INTEGER;
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS confidence_rating INTEGER;
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS body_language_rating INTEGER;
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS overall_rating NUMERIC(3, 2);
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS recommendation VARCHAR(50);
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS comments TEXT;
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS evaluated_at TIMESTAMP;
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS access_token VARCHAR(100);

-- Generate access tokens for existing interviews if null
UPDATE interviews SET access_token = md5(random()::text || id::text) WHERE access_token IS NULL;

CREATE INDEX IF NOT EXISTS idx_interview_access_token ON interviews(access_token);
