CREATE UNIQUE INDEX uq_generated_message_version ON generated_messages(contact_id, variant, version);

INSERT INTO recommendation_rules(user_id, category, keyword, weight) VALUES
(NULL, 'CONTEXT', 'mutual interests', 6),
(NULL, 'HIRING', 'hiring', 5);

UPDATE prompt_templates SET active = FALSE
WHERE user_id IS NULL AND version = 1 AND category IN ('LINKEDIN_REFERRAL','SHORT','EMAIL','FOLLOW_UP','COLD_MESSAGE');

INSERT INTO prompt_templates(user_id, category, version, prompt_text) VALUES
(NULL, 'LINKEDIN_REFERRAL', 2, 'Hi {contactName}, I came across your work at {companyName} and noticed you are a {designation}. Based on my background in {profileSummary}, I am exploring {role} opportunities and would appreciate any advice you are comfortable sharing. I would be happy to share my {resumeLabel} resume if useful. Thank you for your time.'),
(NULL, 'SHORT', 2, 'Hi {contactName}, I am exploring {role} roles at {companyName}. My background is in {profileSummary}. If you are open to it, I would appreciate any advice on the team or application process. Thank you!'),
(NULL, 'EMAIL', 2, 'Subject: Exploring {role} opportunities at {companyName}\n\nHi {contactName},\n\nI noticed your work as {designation} at {companyName}. Based on my background in {profileSummary}, I am exploring {role} opportunities and would value any guidance you are comfortable sharing. I would be happy to share my {resumeLabel} resume for context.\n\nThank you for your time,\n{userName}'),
(NULL, 'FOLLOW_UP', 2, 'Hi {contactName}, I wanted to gently follow up on my note about {role} opportunities at {companyName}. I understand you may be busy; any guidance you are comfortable sharing would be appreciated. Thank you, and no worries if now is not a good time.'),
(NULL, 'COLD_MESSAGE', 2, 'Hi {contactName}, I found your profile while learning about {companyName} and your work as {designation}. My background is in {profileSummary} and I am exploring {role} roles. I would be grateful for any perspective you are comfortable sharing. Thank you for considering my note.');
