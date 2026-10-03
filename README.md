# Referral CRM

Referral CRM is a self-hosted job-search workspace for organizing companies, referral contacts, and manually recorded outreach. It prepares and tracks outreach; it does not send LinkedIn messages, automate LinkedIn actions, or scrape LinkedIn.

## Architecture

- **API:** Spring Boot 3.5, Java 25, Gradle, PostgreSQL, JPA/Hibernate, Flyway, REST, OpenAPI, and JWT authentication.
- **Web:** Next.js App Router, TypeScript, Tailwind CSS 4, and accessible ShadCN-inspired UI primitives.
- **Authentication:** The browser uses a Next.js backend-for-frontend proxy. The API JWT stays in an HttpOnly, SameSite=Lax cookie and is not exposed to client-side JavaScript or local storage.
- **Data:** UUIDs, owner-scoped records, audit timestamps, soft deletes, validation, and paginated/sortable list APIs.
- **Deployment:** Docker Compose runs PostgreSQL, the API, and the web app.

## Run the application with Docker

1. Copy `.env.example` to `.env`. Set a unique database password and a random `JWT_SECRET` of at least 32 characters.
2. Run `docker compose up --build`.
3. Open the app at `http://localhost:3000`. The API and Swagger UI are at `http://localhost:8080` and `http://localhost:8080/swagger-ui/index.html`.

## Run locally

Install Java 25, Gradle 9.1+, PostgreSQL, Node.js 20.9+, and npm. Start PostgreSQL and create a database/user, then run the API from the repository root:

```sh
export DB_URL=jdbc:postgresql://localhost:5432/referral_crm
export DB_USERNAME=referral
export DB_PASSWORD=your-local-password
export JWT_SECRET='use-a-random-secret-with-at-least-32-characters'
gradle bootRun
```

In another terminal, run the web application:

```sh
cd frontend
npm ci
npm run dev
```

Open `http://localhost:3000`. The web app proxies API calls to `http://localhost:8080/api/v1` by default. Set `CRM_API_URL` to override that URL. For production, configure `CRM_API_URL` to the API base URL, serve both apps over HTTPS, use strong secrets, and configure backups and monitoring.

## Current features (Phases 1–2)

- Local account registration/login with JWT, BCrypt password hashing, and authenticated session handling.
- Dashboard counts, response/referral rates, company activity, and a seven-day follow-up queue.
- Company and contact CRUD, search/filtering, pagination, status tracking, and contact/company linking.
- Global search across contacts, companies, notes, and saved outreach messages.
- Responsive layout, dark mode, and manual outreach guidance.
- Company, contact, resume metadata, outreach history, and interview CRUD REST APIs.

The follow-up queue highlights contacts that have not replied seven days after their latest saved outreach. A reply status removes them from the queue. Outreach history is created only when the user records that they manually sent a message.

## REST API

All routes are under `/api/v1`. Authentication routes are `/auth/register`, `/auth/login`, and `/auth/me`. CRM resources support create, paginated list, read, update, and soft-delete operations:

- `/companies`
- `/contacts` (supports `search` and `status` filters)
- `/resumes` (metadata only)
- `/outreach` (manually recorded sent messages)
- `/interviews`
- `/dashboard`
- `/search?q=...` (contacts, companies, notes, and outreach message text)

List APIs accept `page` (zero-based), `size` (1–100), `sort`, and `direction` (`asc` or `desc`). Swagger UI is available at `/swagger-ui/index.html`; health checks are at `/actuator/health`.

Contact statuses: `NOT_CONTACTED`, `MESSAGE_READY`, `CONTACTED`, `REPLIED`, `REFERRED`, `INTERVIEW`, `REJECTED`, and `NO_RESPONSE`. Outreach channels are caller-provided strings such as `LINKEDIN`, `EMAIL`, `REFERRAL_PORTAL`, or `OTHER`.

## Development status

Phase 1 provides the database foundation, authentication, and core CRUD APIs. Phase 2 adds the web app, dashboard, contacts/companies workflows, session proxy, and global search. Resume binary uploads, AI message generation and job-description assistance, export, a dedicated interview UI, drag-and-drop Kanban, and integration interfaces remain for later phases. No LinkedIn messaging or scraping integration is planned.

The API test suite can be run with `gradle test`; the frontend checks use `npm run typecheck` and `npm run build` from `frontend/`.
