CREATE TABLE job_opportunities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_users(id),
    company_id UUID NOT NULL REFERENCES companies(id),
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    location VARCHAR(200),
    employment_type VARCHAR(32),
    experience_level VARCHAR(32),
    source VARCHAR(120),
    career_url VARCHAR(2048),
    salary_range VARCHAR(120),
    status VARCHAR(32) NOT NULL DEFAULT 'FOUND',
    priority VARCHAR(16) NOT NULL DEFAULT 'MEDIUM',
    match_score SMALLINT NOT NULL DEFAULT 0 CHECK (match_score BETWEEN 0 AND 100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    CHECK (status IN ('FOUND','INTERESTED','REFERRAL_REQUIRED','APPLIED','OA','INTERVIEW','OFFER','REJECTED')),
    CHECK (priority IN ('LOW','MEDIUM','HIGH','DREAM'))
);
CREATE INDEX idx_job_opportunities_user_created ON job_opportunities(user_id, created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_job_opportunities_company ON job_opportunities(company_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_job_opportunities_user_status ON job_opportunities(user_id, status) WHERE deleted_at IS NULL;

CREATE TABLE job_skills (
    job_id UUID NOT NULL REFERENCES job_opportunities(id) ON DELETE CASCADE,
    skill VARCHAR(100) NOT NULL,
    PRIMARY KEY(job_id, skill)
);
CREATE INDEX idx_job_skills_skill ON job_skills(lower(skill));

CREATE TABLE resume_skills (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resume_id UUID NOT NULL REFERENCES resumes(id),
    skill VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_resume_skills_resume ON resume_skills(resume_id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_resume_skills_active ON resume_skills(resume_id, lower(skill)) WHERE deleted_at IS NULL;
