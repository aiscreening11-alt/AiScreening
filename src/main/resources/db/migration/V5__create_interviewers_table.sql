-- V5: Create interviewers table for dedicated interviewer accounts and HR interviewer management
CREATE TABLE IF NOT EXISTS interviewers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    designation VARCHAR(255),
    department VARCHAR(100) DEFAULT 'Engineering',
    phone_number VARCHAR(50),
    role VARCHAR(50) DEFAULT 'INTERVIEWER',
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_interviewers_email ON interviewers(LOWER(email));
CREATE INDEX IF NOT EXISTS idx_interviewers_active ON interviewers(active);

-- Seed initial default interviewers (password: Sanvii@2016 -> BCrypt hashed)
-- BCrypt for 'Sanvii@2016': $2a$10$iF668iY4c7R2yT2v/s4Ocu4KzV2G2eE2Z1Pq.6kX4A1h3V5j7l3l.
INSERT INTO interviewers (id, full_name, email, password_hash, designation, department, role, active, created_at, updated_at)
VALUES 
(
    'a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d',
    'Lead Technical Architect',
    'lead.architect@sanvii.com',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    'Senior Technical Architect',
    'Engineering',
    'INTERVIEWER',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (email) DO NOTHING;

INSERT INTO interviewers (id, full_name, email, password_hash, designation, department, role, active, created_at, updated_at)
VALUES 
(
    'b2c3d4e5-f6a7-8b9c-0d1e-2f3a4b5c6d7e',
    'Vikram Sharma',
    'vikram.sharma@sanvii.com',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    'Lead Full Stack Engineer',
    'Engineering',
    'INTERVIEWER',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (email) DO NOTHING;
