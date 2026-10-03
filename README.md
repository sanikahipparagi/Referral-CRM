# Referral CRM

Referral CRM is a self-hosted job-search workspace for organizing companies, contacts, resumes, interviews, and referral outreach. It helps prepare and track outreach while the user stays in control of every action.

> **LinkedIn safety:** The application does not send LinkedIn messages, click LinkedIn controls, connect with people, scrape LinkedIn, or automate browser actions. Use **Open profile** to visit a contact's profile yourself, copy/edit a draft, send it manually, and then record it with **Mark as Sent**.

## Features

- **Dashboard:** outreach activity, response/referral statistics, company summaries, and follow-ups due today.
- **Job intelligence:** save job opportunities, required skills, status, priority, salary, source, and career URL; search and filter the job list.
- **Job matching and ranking:** compare required skills with your saved profile and resume skills, explain missing skills, recommend a resume, and rank roles using the documented weighted score.
- **Companies and contacts:** owner-scoped records, contact status tracking, filtering, search, and company links.
- **Today's Opportunities:** filter contacts by company, role, status, location, and priority; review recommendations and suggested companies.
- **Outreach review:** generate LinkedIn, short, email, and follow-up drafts; edit/version, approve, copy, open a profile, and record a message after manually sending it.
- **Recommendation rules:** editable per-user keywords and weights for contact scores, with a short explanation for each recommendation.
- **Resume intelligence:** upload multiple PDF resumes, keep files in private local storage, extract selectable text and catalog skills, and review heuristically detected projects and experience.
- **Job analysis:** compare saved resumes with a job's required skills, show evidence, missing skills, match score, resume recommendation, and truthful improvement suggestions. Results are saved for later review.
- **Interview preparation:** create job-specific technical topics, system design topics, and practice questions; save personal notes.
- **Prompts:** edit and version database-backed prompt templates in the UI.
- **Networking analytics:** response, referral, interview, and offer rates; reply time; top companies and roles; resume, message, and contact-type performance.
- **CRM records:** outreach history, interviews, and global search.
- **Interface:** responsive pages, dark mode, and a Next.js session proxy that keeps the API JWT in an HttpOnly cookie.

## Architecture

- **Backend:** Spring Boot 3.5, Java 25, Gradle, PostgreSQL, Spring Data JPA/Hibernate, Flyway, REST APIs, OpenAPI, and JWT authentication.
- **Frontend:** Next.js App Router, TypeScript, Tailwind CSS 4, and ShadCN-inspired UI primitives.
- **Persistence:** UUID identifiers, owner-scoped records, audit timestamps, soft deletes, validation, and pagination/sorting on list endpoints. Schema changes are additive Flyway migrations; Hibernate does not manage production schema changes.
- **Service boundaries:** `ResumeParserService` extracts text, `ResumeProfileService` structures skills/projects/experience, `ResumeJobMatchService` compares owned resumes to jobs, and `JobAnalysisService`, `ResumeImprovementService`, and `InterviewPreparationService` persist reviewable analysis and preparation. Controllers remain thin.
- **Opportunity score:** skill match 40%, company priority 20%, role fit 20%, location 10%, and experience fit 10%. Company scoring reuses the existing 1–5 priority scale and `dreamCompany` flag as the target-company marker.
- **Provider extension points:** `LLMProvider`, `JobProvider`, and `ContactProvider` are interfaces only. No provider implementation, LinkedIn integration, or contact scraping is included.
- **Manual-send workflow:** a draft is stored independently; the app records an outreach event only after the user approves the draft and explicitly marks it sent. The backend has no message-send operation.
- **Deployment:** Docker Compose runs PostgreSQL, the API, and the web application.
- **Resume storage:** PDF binaries use a mounted `resume_files` Docker volume (or `RESUME_STORAGE_PATH` locally); metadata and extracted text are stored in PostgreSQL. Upload size defaults to 10 MB and page count to 200.

## Requirements

For local development, install:

- Java 25
- Gradle 9.1 or later
- PostgreSQL 16 (or use the Docker Compose database)
- Node.js 20.9 or later and npm
- Docker Desktop, if running the full stack with Docker

## Quick start with Docker

1. Copy `.env.example` to `.env` in the repository root.
2. Set `DB_PASSWORD` to a unique password and `JWT_SECRET` to a random secret of at least 32 bytes.
3. Start the stack:

   ```sh
   docker compose up --build -d
   ```

4. Open [http://localhost:3000](http://localhost:3000), then register an account.

The API is available at [http://localhost:8080](http://localhost:8080), Swagger UI at [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html), and health at [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health).

Useful Docker commands:

```sh
docker compose logs -f api      # Follow API logs
docker compose logs -f web      # Follow frontend logs
docker compose ps               # Show service status
docker compose down             # Stop services; keep database volume
```

To remove the local database volume as well as stop the services, use `docker compose down -v`. This permanently deletes the local database contents.

## Local development

Start PostgreSQL and create a database and user, then start the backend from the repository root. Set environment variables for your local database credentials and a secure JWT secret:

```sh
export DB_URL=jdbc:postgresql://localhost:5432/referral_crm
export DB_USERNAME=referral
export DB_PASSWORD=your-local-password
export JWT_SECRET='use-a-random-secret-with-at-least-32-bytes'
export APP_FOLLOW_UP_DAYS=7
gradle bootRun
```

In another terminal, install frontend dependencies and run Next.js:

```sh
cd frontend
npm ci
npm run dev
```

Open [http://localhost:3000](http://localhost:3000). The web server proxies API calls to `http://localhost:8080/api/v1` by default. Set `CRM_API_URL` to override the API base URL. `APP_FOLLOW_UP_DAYS` configures when unanswered outreach becomes due for follow-up; it defaults to 7 days.

## Configuration

| Variable | Used by | Description |
| --- | --- | --- |
| `DB_PASSWORD` | Docker Compose | PostgreSQL password; set in `.env`. |
| `DB_URL` | API | JDBC URL for PostgreSQL. |
| `DB_USERNAME` | API | PostgreSQL username. |
| `JWT_SECRET` | API | Secret used to sign JWTs; must be at least 32 bytes. |
| `APP_FOLLOW_UP_DAYS` | API | Days without a reply before follow-up is due; defaults to `7`. |
| `CORS_ORIGINS` | API | Allowed browser origins; defaults to `http://localhost:3000`. |
| `CRM_API_URL` | Frontend | API base URL used by the Next.js proxy; defaults to `http://localhost:8080/api/v1`. |
| `RESUME_STORAGE_PATH` | API | Private directory for resume PDFs; defaults to `./data/resumes`. Docker uses `/app/storage/resumes` on the `resume_files` volume. |
| `RESUME_MAX_FILE_SIZE` | API | Multipart upload/request limit; defaults to `10MB`. |
| `RESUME_MAX_PAGES` | API | Maximum parsed PDF page count; defaults to `200` (bounded to 1–1000). |

For a production deployment, use HTTPS, strong secrets, restricted network access, regular database backups, and monitoring. Do not expose PostgreSQL publicly.

## Assistant behavior and current scope

The recommendation engine scores contacts from editable database rules, such as role and skills keywords. Resume recommendations for job analysis compare required job skills against extracted and saved resume skills, project technologies, and experience evidence. Catalog/heading parsing is heuristic and should be reviewed by the user; it is not a substitute for checking the source resume. Only text-based PDFs are supported: encrypted, damaged, and image-only/scanned PDFs are rejected (OCR is not implemented). The current catalog covers common technology and domain terms rather than every skill.

There is no configured LLM provider in this phase. Job matching, profile extraction, improvement suggestions, and interview-preparation content use deterministic local rules. `LLMProvider`, `OpenAIProvider`, and `OllamaProvider` are extension interfaces only; no external model calls or credentials are configured. The analysis prompt treats resume/job text as data and prohibits invented experience, skills, metrics, or credentials when a provider is added later. Review all extracted profile details and suggestions before using them.

## REST API

All API routes are under `/api/v1`. Authenticated CRM data is scoped to the signed-in user. Registration, login, and current-session routes are `/auth/register`, `/auth/login`, and `/auth/me`.

| Route | Purpose |
| --- | --- |
| `/companies` | Company CRUD and list/search. |
| `/contacts` | Contact CRUD and list/search/status filtering. |
| `/resumes` | Resume metadata CRUD. |
| `/outreach` | Outreach records, created when the user records a manually sent message. |
| `/interviews` | Interview CRUD. |
| `/jobs` | Job CRUD with pagination, sorting, search, and `status`, `companyId`, and `priority` filters. |
| `/jobs/{id}/match` | Recalculate skill/profile match, return matched/missing skills and a recommended resume, and save the match score. |
| `/jobs/top?limit=10` | Return top ranked jobs with score factors and resume recommendations. |
| `/resumes/{resumeId}/skills` | List or replace manually maintained resume skills. |
| `/resumes/upload` | Upload a PDF with multipart fields `label` and `file`; returns resume/document metadata and the extracted profile. |
| `/resumes/{resumeId}/documents` | List uploaded PDF versions. |
| `/resumes/{resumeId}/intelligence` | View detected skills, projects, experience, and the latest extracted text. |
| `/resumes/{resumeId}/documents/{documentId}/text` | Read text extracted from a specific resume PDF. |
| `/resumes/{resumeId}/documents/{documentId}/download` | Download the stored PDF. |
| `/jobs/{jobId}/analysis` | `POST` to analyze saved resumes for the job and persist a result; `GET` to list analysis history. |
| `/jobs/{jobId}/suggestions?resumeId=...` | List stored job-specific suggestions; `PATCH /{suggestionId}` with `PENDING`, `APPLIED`, or `IGNORED` to update review status. |
| `/jobs/{jobId}/interview-prep` | `POST` to generate preparation topics/questions; `GET` for saved history; `PUT /{noteId}` to save notes. |
| `/dashboard` | Dashboard counts and summaries. |
| `/assistant/opportunities` | Filtered opportunities and recommendations. |
| `/assistant/messages/generate` | Generate and save message drafts. |
| `/assistant/messages` | List the latest drafts; `/{id}` edits into a new version, `/{id}/approve` approves, and `/{id}/sent` records a manually sent message. |
| `/assistant/follow-up/generate` | Generate a follow-up draft only when it is due and the contact has not replied. |
| `/assistant/prompts` | List templates; `/{category}` saves a new version and `/{category}/versions` lists history. |
| `/assistant/recommendation-rules` | Read and replace the user's recommendation rules. |
| `/assistant/resume-recommendation` | Recommend saved resume metadata for a contact and role. |
| `/assistant/analytics` | Networking metrics and performance summaries. |
| `/assistant/profile` | Read and update the profile summary used in drafts. |
| `/search?q=...` | Global search across CRM records, jobs and required skills, notes, and saved messages. |

Job statuses are `FOUND`, `INTERESTED`, `REFERRAL_REQUIRED`, `APPLIED`, `OA`, `INTERVIEW`, `OFFER`, and `REJECTED`; priorities are `LOW`, `MEDIUM`, `HIGH`, and `DREAM`. The jobs list accepts `search`, `status`, `companyId`, and `priority` alongside pagination/sorting. Resume skills can be entered through the Jobs page or `/resumes/{resumeId}/skills`. Contact statuses are `NOT_CONTACTED`, `MESSAGE_READY`, `CONTACTED`, `REPLIED`, `REFERRED`, `INTERVIEW`, `REJECTED`, and `NO_RESPONSE`. Outreach channels are `LINKEDIN`, `EMAIL`, `REFERRAL_PORTAL`, and `OTHER`.

List endpoints support `page` (zero-based), `size` (up to 100), `sort`, and `direction` (`asc` or `desc`) where applicable.

## Development checks

Run backend tests from the repository root and frontend checks from `frontend/`:

```sh
gradle test
cd frontend
npm run typecheck
npm run build
```

## Project status

Phase 1 established the database, authentication, and CRM APIs. Phase 2 added the dashboard and web application. Phase 3 added the networking assistant, opportunity and review queues, recommendations, versioned prompts, and analytics. Phase 4 added job tracking and deterministic matching/ranking. Phase 5 adds PDF resume storage and text extraction, structured resume intelligence, job-specific analysis and improvement suggestions, interview preparation, and review screens. Job listings are still entered manually; job imports and LLM/job/contact providers are interfaces only. OCR, external job discovery, configured AI providers, export to Excel/CSV/PDF, and drag-and-drop Kanban remain future work. Automated LinkedIn activity, scraping, browser automation, and auto applications are explicitly out of scope.
