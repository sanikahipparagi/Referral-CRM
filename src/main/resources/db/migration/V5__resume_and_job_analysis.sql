CREATE TABLE resume_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resume_id UUID NOT NULL REFERENCES resumes(id),
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(120) NOT NULL,
    file_size BIGINT NOT NULL CHECK (file_size >= 0),
    storage_path VARCHAR(1024) NOT NULL,
    extracted_text TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_resume_documents_resume ON resume_documents(resume_id, created_at DESC) WHERE deleted_at IS NULL;

CREATE TABLE resume_projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resume_id UUID NOT NULL REFERENCES resumes(id),
    name VARCHAR(200) NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_resume_projects_resume ON resume_projects(resume_id) WHERE deleted_at IS NULL;
CREATE TABLE resume_project_technologies (
    project_id UUID NOT NULL REFERENCES resume_projects(id) ON DELETE CASCADE,
    technology VARCHAR(100) NOT NULL,
    PRIMARY KEY(project_id, technology)
);

CREATE TABLE resume_experiences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resume_id UUID NOT NULL REFERENCES resumes(id),
    company VARCHAR(200) NOT NULL DEFAULT '',
    role VARCHAR(200) NOT NULL DEFAULT '',
    description TEXT NOT NULL DEFAULT '',
    duration VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_resume_experiences_resume ON resume_experiences(resume_id) WHERE deleted_at IS NULL;

CREATE TABLE job_analysis_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES job_opportunities(id),
    resume_id UUID REFERENCES resumes(id),
    match_score SMALLINT NOT NULL CHECK (match_score BETWEEN 0 AND 100),
    result_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_job_analysis_job ON job_analysis_results(job_id, created_at DESC) WHERE deleted_at IS NULL;

CREATE TABLE resume_improvement_suggestions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resume_id UUID NOT NULL REFERENCES resumes(id),
    job_id UUID NOT NULL REFERENCES job_opportunities(id),
    suggestion TEXT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','APPLIED','IGNORED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_resume_suggestions_job ON resume_improvement_suggestions(job_id, status) WHERE deleted_at IS NULL;

CREATE TABLE interview_preparation_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES job_opportunities(id),
    resume_id UUID REFERENCES resumes(id),
    technical_topics TEXT NOT NULL DEFAULT '[]',
    system_design_topics TEXT NOT NULL DEFAULT '[]',
    possible_questions TEXT NOT NULL DEFAULT '[]',
    notes TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_interview_prep_job ON interview_preparation_notes(job_id, created_at DESC) WHERE deleted_at IS NULL;
