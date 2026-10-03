CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE app_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    full_name VARCHAR(160) NOT NULL,
    profile_text TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);
CREATE TABLE companies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_users(id),
    name VARCHAR(200) NOT NULL,
    career_page VARCHAR(2048),
    priority SMALLINT NOT NULL DEFAULT 3 CHECK (priority BETWEEN 1 AND 5),
    dream_company BOOLEAN NOT NULL DEFAULT FALSE,
    application_status VARCHAR(32) NOT NULL DEFAULT 'NOT_APPLIED',
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    UNIQUE(user_id, name)
);
CREATE TABLE resumes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_users(id),
    label VARCHAR(120) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    storage_key VARCHAR(1024) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    file_size BIGINT NOT NULL CHECK (file_size > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);
CREATE TABLE contacts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_users(id),
    company_id UUID REFERENCES companies(id),
    name VARCHAR(160) NOT NULL,
    linkedin_url VARCHAR(2048),
    designation VARCHAR(200),
    location VARCHAR(200),
    email VARCHAR(320),
    source VARCHAR(120),
    date_added DATE NOT NULL DEFAULT CURRENT_DATE,
    notes TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'NOT_CONTACTED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);
CREATE TABLE outreach (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_users(id),
    contact_id UUID NOT NULL REFERENCES contacts(id),
    resume_id UUID REFERENCES resumes(id),
    channel VARCHAR(32) NOT NULL,
    message_version VARCHAR(32) NOT NULL,
    message_text TEXT NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);
CREATE TABLE interviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_users(id),
    company_id UUID NOT NULL REFERENCES companies(id),
    round VARCHAR(160) NOT NULL,
    interview_at TIMESTAMPTZ,
    result VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    feedback TEXT,
    package_amount NUMERIC(14,2),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_contacts_user_status ON contacts(user_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_contacts_user_company ON contacts(user_id, company_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_outreach_user_contact_sent ON outreach(user_id, contact_id, sent_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_interviews_user_date ON interviews(user_id, interview_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_companies_user_name ON companies(user_id, name) WHERE deleted_at IS NULL;
