-- PostgreSQL Initial Schema Migration

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Jobs Table
CREATE TABLE IF NOT EXISTS jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_title VARCHAR(200) NOT NULL,
    department VARCHAR(100),
    description TEXT,
    minimum_experience_years DOUBLE PRECISION DEFAULT 0.0,
    education_requirement VARCHAR(200),
    location VARCHAR(150),
    employment_type VARCHAR(50) DEFAULT 'FULL_TIME',
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_jobs_status ON jobs(status);
CREATE INDEX IF NOT EXISTS idx_jobs_title ON jobs(job_title);

-- Job Required Skills
CREATE TABLE IF NOT EXISTS job_required_skills (
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    skill VARCHAR(100) NOT NULL,
    PRIMARY KEY (job_id, skill)
);

-- Job Preferred Skills
CREATE TABLE IF NOT EXISTS job_preferred_skills (
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    skill VARCHAR(100) NOT NULL,
    PRIMARY KEY (job_id, skill)
);

-- 2. Candidates Table
CREATE TABLE IF NOT EXISTS candidates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE RESTRICT,
    full_name VARCHAR(200),
    sender_email VARCHAR(255),
    resume_email VARCHAR(255),
    email_discrepancy_flag BOOLEAN DEFAULT FALSE,
    phone_number VARCHAR(50),
    total_experience_years DOUBLE PRECISION DEFAULT 0.0,
    current_company VARCHAR(200),
    previous_companies TEXT,
    education VARCHAR(200),
    degree VARCHAR(150),
    location VARCHAR(150),
    notice_period VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'RECEIVED',
    applied_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_candidate_email ON candidates(resume_email);
CREATE INDEX IF NOT EXISTS idx_candidate_sender_email ON candidates(sender_email);
CREATE INDEX IF NOT EXISTS idx_candidate_status ON candidates(status);
CREATE INDEX IF NOT EXISTS idx_candidate_job_id ON candidates(job_id);

-- 3. Resumes Table
CREATE TABLE IF NOT EXISTS resumes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id UUID NOT NULL UNIQUE REFERENCES candidates(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(50),
    storage_key VARCHAR(255) NOT NULL,
    resume_hash VARCHAR(64),
    file_size_bytes BIGINT,
    extracted_text TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_resume_candidate_id ON resumes(candidate_id);
CREATE INDEX IF NOT EXISTS idx_resume_hash ON resumes(resume_hash);

-- 4. Candidate Skills Table
CREATE TABLE IF NOT EXISTS candidate_skills (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id UUID NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    skill_name VARCHAR(100) NOT NULL,
    normalized_skill VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    is_matched_requirement BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_candidate_skills_candidate ON candidate_skills(candidate_id);
CREATE INDEX IF NOT EXISTS idx_candidate_skills_normalized ON candidate_skills(normalized_skill);

-- 5. ATS Results Table
CREATE TABLE IF NOT EXISTS ats_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id UUID NOT NULL UNIQUE REFERENCES candidates(id) ON DELETE CASCADE,
    rule_score DOUBLE PRECISION NOT NULL,
    ai_score DOUBLE PRECISION,
    final_score DOUBLE PRECISION NOT NULL,
    decision VARCHAR(50) NOT NULL,
    scoring_breakdown_json TEXT,
    ai_summary TEXT,
    is_ai_assisted BOOLEAN DEFAULT FALSE,
    evaluated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ats_candidate_id ON ats_results(candidate_id);
CREATE INDEX IF NOT EXISTS idx_ats_final_score ON ats_results(final_score);
CREATE INDEX IF NOT EXISTS idx_ats_decision ON ats_results(decision);

CREATE TABLE IF NOT EXISTS ats_matched_skills (
    ats_result_id UUID NOT NULL REFERENCES ats_results(id) ON DELETE CASCADE,
    skill VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS ats_missing_skills (
    ats_result_id UUID NOT NULL REFERENCES ats_results(id) ON DELETE CASCADE,
    skill VARCHAR(100) NOT NULL
);

-- 6. Email Messages Table (Gmail Ingestion Idempotency)
CREATE TABLE IF NOT EXISTS email_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gmail_message_id VARCHAR(100) NOT NULL UNIQUE,
    sender_email VARCHAR(255),
    subject VARCHAR(500),
    body_snippet TEXT,
    received_at TIMESTAMP WITHOUT TIME ZONE,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    processing_status VARCHAR(50) DEFAULT 'RECEIVED',
    error_message TEXT,
    candidate_id UUID REFERENCES candidates(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_gmail_msg_id ON email_messages(gmail_message_id);
CREATE INDEX IF NOT EXISTS idx_email_msg_processed ON email_messages(processed);

-- 7. Email Communications Table (Outbound Notifications History)
CREATE TABLE IF NOT EXISTS email_communications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id UUID NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    recipient_email VARCHAR(255) NOT NULL,
    email_type VARCHAR(50) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    body_content TEXT NOT NULL,
    sent_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    sent_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_email_comm_candidate ON email_communications(candidate_id);
CREATE INDEX IF NOT EXISTS idx_email_comm_status ON email_communications(sent_status);

-- 8. Interviews Table (Stage 2 Outcome)
CREATE TABLE IF NOT EXISTS interviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id UUID NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    interview_date TIMESTAMP WITHOUT TIME ZONE,
    interviewer_name VARCHAR(150),
    feedback TEXT,
    result VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_interview_candidate ON interviews(candidate_id);
CREATE INDEX IF NOT EXISTS idx_interview_result ON interviews(result);

-- 9. Audit Logs Table
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    action VARCHAR(100) NOT NULL,
    performed_by VARCHAR(100) DEFAULT 'SYSTEM',
    old_state TEXT,
    new_state TEXT,
    reason TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_created_at ON audit_logs(created_at);
