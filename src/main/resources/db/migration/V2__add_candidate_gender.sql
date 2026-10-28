-- Migration V2: Add Candidate Gender for Diversity & Demographics Tracking

ALTER TABLE candidates ADD COLUMN IF NOT EXISTS gender VARCHAR(50) DEFAULT 'UNSPECIFIED';

CREATE INDEX IF NOT EXISTS idx_candidate_gender ON candidates(gender);

-- Update existing candidates with intelligent name mapping
UPDATE candidates 
SET gender = 'MALE' 
WHERE LOWER(full_name) LIKE '%amit%' 
   OR LOWER(full_name) LIKE '%rajesh%' 
   OR LOWER(full_name) LIKE '%rahul%'
   OR LOWER(full_name) LIKE '%john%'
   OR LOWER(full_name) LIKE '%david%';

UPDATE candidates 
SET gender = 'FEMALE' 
WHERE LOWER(full_name) LIKE '%jessy%' 
   OR LOWER(full_name) LIKE '%priya%' 
   OR LOWER(full_name) LIKE '%neha%'
   OR LOWER(full_name) LIKE '%sarah%'
   OR LOWER(full_name) LIKE '%emily%';
