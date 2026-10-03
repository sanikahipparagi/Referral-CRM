ALTER TABLE contacts ADD COLUMN replied_at TIMESTAMPTZ;
ALTER TABLE outreach ADD COLUMN generated_message_id UUID;

CREATE TABLE prompt_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES app_users(id),
    category VARCHAR(32) NOT NULL,
    version INTEGER NOT NULL CHECK (version > 0),
    prompt_text TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    UNIQUE (user_id, category, version)
);
CREATE UNIQUE INDEX uq_prompt_system_version ON prompt_templates(category, version) WHERE user_id IS NULL;
CREATE INDEX idx_prompt_template_lookup ON prompt_templates(user_id, category, active) WHERE deleted_at IS NULL;

CREATE TABLE recommendation_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES app_users(id),
    category VARCHAR(32) NOT NULL,
    keyword VARCHAR(100) NOT NULL,
    weight SMALLINT NOT NULL CHECK (weight BETWEEN 1 AND 40),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_recommendation_rules_lookup ON recommendation_rules(user_id, enabled) WHERE deleted_at IS NULL;

CREATE TABLE generated_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_users(id),
    contact_id UUID NOT NULL REFERENCES contacts(id),
    company_id UUID REFERENCES companies(id),
    resume_id UUID REFERENCES resumes(id),
    role VARCHAR(200),
    variant VARCHAR(32) NOT NULL,
    version INTEGER NOT NULL CHECK (version > 0),
    channel VARCHAR(32) NOT NULL,
    message_text TEXT NOT NULL,
    recommendation_score SMALLINT NOT NULL DEFAULT 0 CHECK (recommendation_score BETWEEN 0 AND 100),
    recommendation_reason TEXT,
    resume_reason TEXT,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','APPROVED','SENT')),
    approved_at TIMESTAMPTZ,
    sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_generated_messages_user_status ON generated_messages(user_id, status, created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_generated_messages_contact_variant ON generated_messages(contact_id, variant, version DESC) WHERE deleted_at IS NULL;
ALTER TABLE outreach ADD CONSTRAINT fk_outreach_generated_message FOREIGN KEY (generated_message_id) REFERENCES generated_messages(id);

INSERT INTO prompt_templates(user_id, category, version, prompt_text) VALUES
(NULL, 'LINKEDIN_REFERRAL', 1, 'Hi {contactName}, I came across your work at {companyName} and noticed you are a {designation}. Based on my background in {profileSummary}, I am exploring {role} opportunities and would appreciate any advice you are comfortable sharing. I have attached my {resumeLabel} resume for context. Thank you for your time.'),
(NULL, 'SHORT', 1, 'Hi {contactName}, I am exploring {role} roles at {companyName}. My background is in {profileSummary}. If you are open to it, I would appreciate any advice on the team or application process. Thank you!'),
(NULL, 'EMAIL', 1, 'Subject: Exploring {role} opportunities at {companyName}\n\nHi {contactName},\n\nI noticed your work as {designation} at {companyName}. Based on my background in {profileSummary}, I am exploring {role} opportunities and would value any guidance you are comfortable sharing. I have included my {resumeLabel} resume for context.\n\nThank you for your time,\n{userName}'),
(NULL, 'FOLLOW_UP', 1, 'Hi {contactName}, I wanted to gently follow up on my note about {role} opportunities at {companyName}. I understand you may be busy; any guidance you are comfortable sharing would be appreciated. Thank you, and no worries if now is not a good time.'),
(NULL, 'COLD_MESSAGE', 1, 'Hi {contactName}, I found your profile while learning about {companyName} and your work as {designation}. My background is in {profileSummary} and I am exploring {role} roles. I would be grateful for any perspective you are comfortable sharing. Thank you for considering my note.');

INSERT INTO recommendation_rules(user_id, category, keyword, weight) VALUES
(NULL, 'ROLE', 'senior engineer', 12),
(NULL, 'ROLE', 'staff engineer', 18),
(NULL, 'ROLE', 'engineering manager', 16),
(NULL, 'ROLE', 'recruiter', 10),
(NULL, 'SKILL', 'backend', 8),
(NULL, 'SKILL', 'java', 10),
(NULL, 'SKILL', 'spring', 8),
(NULL, 'SKILL', 'aws', 6),
(NULL, 'CONTEXT', 'referral', 8),
(NULL, 'CONTEXT', 'mentor', 5);
