-- Migration V3: Add Multi-Round Interview Pipeline Support (L1 -> L2 -> Managerial -> HR)

ALTER TABLE interviews ADD COLUMN IF NOT EXISTS round VARCHAR(50) DEFAULT 'L1_TECHNICAL';
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS meeting_link VARCHAR(255);
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS interviewer_email VARCHAR(150);
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS duration_minutes INTEGER DEFAULT 45;
ALTER TABLE interviews ADD COLUMN IF NOT EXISTS notes TEXT;

CREATE INDEX IF NOT EXISTS idx_interview_round ON interviews(round);
