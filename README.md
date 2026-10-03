# Referral CRM — Phase 1 API

A self-hosted foundation for managing job-search referral outreach. This phase provides PostgreSQL persistence, JWT local authentication, and owner-scoped CRUD for companies, contacts, resumes, manually recorded outreach, and interviews. The backend only prepares and stores outreach records. It has no LinkedIn integration, message-sending capability, or scraping behavior.

## Architecture

- Spring Boot 3.5 / Java 25 REST API with DTO validation, owner-scoped access, and a centralized error response.
- PostgreSQL schema managed by Flyway; Hibernate runs in `validate` mode and never creates production tables.
- UUID primary keys and audit timestamps on every entity. Delete endpoints set `deleted_at` (soft delete).
- Stateless JWT authentication. Passwords are stored as BCrypt hashes. All CRM endpoints are scoped to the authenticated account.
- OpenAPI UI at `/swagger-ui/index.html`; liveness/readiness health at `/actuator/health`.

Companies, contacts, resumes, outreach events, and interviews are normalized into separate tables. Outreach stores a snapshot of the message version/text and references the resume used, so later edits do not change the historical sent record. Company and resume links are validated against the authenticated user.

## Run with Docker

1. Copy `.env.example` to `.env` and set a unique database password and a random JWT secret of at least 32 bytes.
2. Run `docker compose up --build`.
3. Open `http://localhost:8080/swagger-ui/index.html`.

For local development, use Java 25 and Gradle 9.1 or newer, then run `gradle bootRun`. Start PostgreSQL first and set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` as needed. The compiler targets Java 25. Gradle 9.1 or newer is needed to run Gradle on Java 25; see the [Gradle Java compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html).

## Authentication

`POST /api/v1/auth/register`

```json
{"fullName":"Alex Example","email":"alex@example.com","password":"a-long-password-12"}
```

`POST /api/v1/auth/login` accepts `email` and `password`. Both return `accessToken`, `tokenType`, `expiresInSeconds`, and a basic user object. Send subsequent requests using `Authorization: Bearer <accessToken>`.

Passwords must be 12–72 characters. Store the token securely in the eventual browser client; production deployment should use HTTPS and a managed secret store.

## CRUD routes

All resources support `POST`, paginated `GET`, `GET /{id}`, `PUT /{id}`, and soft-delete `DELETE /{id}`:

- `/api/v1/companies`
- `/api/v1/contacts` (supports `search` and `status` query parameters)
- `/api/v1/resumes`
- `/api/v1/outreach`
- `/api/v1/interviews`

List requests accept `page` (zero-based), `size` (1–100), `sort`, and `direction` (`asc` or `desc`). Responses use Spring's `Page` shape. `search` on companies and contacts searches names and notes. Resume metadata is supported in Phase 1; binary upload storage is intentionally a later phase, so `storageKey` must refer to storage provisioned by the deployment.

Contact statuses: `NOT_CONTACTED`, `MESSAGE_READY`, `CONTACTED`, `REPLIED`, `REFERRED`, `INTERVIEW`, `REJECTED`, `NO_RESPONSE`.

Outreach channels are stored as caller-provided strings (recommended: `LINKEDIN`, `EMAIL`, `REFERRAL_PORTAL`, `OTHER`). Creating outreach means the user has manually sent it and recorded it; the API does not transmit the message.

## Phase plan

1. **Foundation (this deliverable):** schema, auth, core REST CRUD.
2. Frontend dashboard, contact/company workflows, responsive layout and dark mode.
3. Resume upload/storage, message drafts and AI-assisted personalization (manual copy/send only).
4. Follow-up queue, analytics, interview workflows, global search, export and Kanban.
5. Operational hardening: integration interfaces, expanded tests, deployment configuration, backup/monitoring guidance.

## Current implementation limits

This is the runnable API foundation, not yet the complete feature set. Dashboard aggregates, AI/OpenAI integration, binary upload, exports, follow-up scheduling, frontend, and expanded automated tests are reserved for subsequent phases. Review `JWT_SECRET`, database credentials, CORS origins, HTTPS, rate limits, backups, and storage before public deployment.
