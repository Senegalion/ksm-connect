# KSM Connect

A secure event management platform for KSM communities, featuring participant registration, Secret Santa drawings, and personalized gift wishlists.

KSM Connect was built for *Katolickie Stowarzyszenie Młodzieży* (Catholic Youth Association). Its first use case is the Christmas Eve gathering (Wigilia KSM) with a Secret Santa gift exchange. The data model already supports many independent events, so the platform can grow into general event management.

The user interface is in Polish. Code and technical documentation are in English.

## Features

- **Accounts:** email and password registration, email verification, password reset, password change, profile editing and account deletion.
- **Events:** create and configure events with an explicit lifecycle (`DRAFT → REGISTRATION_OPEN ⇄ REGISTRATION_CLOSED → DRAW_COMPLETED`, plus `CANCELLED`). Optional settings include a registration window, a participant limit and a wishlist deadline.
- **Invitations:** high-entropy invitation codes (`KSM-XXXXX-XXXXX`, about 49 bits) and a QR code with a join link. The raw code is shown once and only an HMAC is stored. Regenerating a code revokes the previous one immediately.
- **Joining:** participants join via the QR link or by typing the code. If a visitor is not logged in, the code is remembered through registration and login. Joining is idempotent and duplicate joins are blocked by the database.
- **Wishlist:** up to three suggestions per participant per event. Each suggestion is either an http(s) product link or a text description.
- **Secret Santa drawing:**
  - Generated globally: Fisher–Yates shuffle with `SecureRandom` and rejection sampling until a derangement is found.
  - Validated before persisting, then written atomically together with the notification outbox.
  - Immutable once completed.
  - Manual or scheduled; scheduled draws are persistent and recoverable.
- **Private reveal:** each participant sees only their own recipient (full name) and that person's wishlist after the drawing.
- **Notifications:** Polish email and SMS messages are sent through a transactional outbox. Delivery has exponential backoff, a retry limit, crash recovery and admin retries that never touch the drawing.
- **Organizer panel:** participant management, invitation and QR handling, drawing control, notification delivery monitoring and an audit history.
- **Data retention:** event personal data is purged automatically 90 days after the event. Unverified accounts are deleted after 30 days.
- **Mobile-first design:** responsive layout tested in Playwright at 360px and 390px widths as well as desktop.

## Architecture

KSM Connect is a modular monolith: one Spring Boot service, one PostgreSQL database and one React single-page app. Microservices and a message broker would add operational cost without benefit at this scale.

```
Browser (React SPA, Azure Static Web Apps)
        │ HTTPS, cookie session + CSRF header
        ▼
Cloudflare (DNS, TLS, WAF, rate limiting)
        ▼
Spring Boot API (Azure Container Apps, single replica)
  ├─ HTTP API: auth, events, invitations, wishlists, assignments, admin
  ├─ Scheduler: registration close, scheduled draws, retention
  └─ Outbox worker: email (SMTP), SMS (SMSAPI)
        ▼
PostgreSQL (private VNet access, TLS required)
```

### Backend modules (`backend/src/main/java/com/ksmconnect`)

| Module | Responsibility |
|---|---|
| `auth` | Registration, verification, login, password reset, admin provisioning task |
| `user` | User aggregate, profile, phone normalization, account deletion |
| `security` | Spring Security setup, sessions, CSRF, CORS, headers, rate limiting |
| `event` | Event aggregate, lifecycle, admin event service |
| `invitation` | Invitation code generation, hashing, resolution |
| `participant` | Event participation, join/withdraw/remove, participant queries |
| `wishlist` | Gift suggestions and URL/description validation |
| `draw` | Derangement generator, invariants, transactional draw, assignment queries |
| `notification` | Outbox, worker, retry policy, email/SMS provider abstractions, templates |
| `admin` | Organizer-facing REST API composed from the domain modules |
| `scheduler` | Lifecycle, outbox and retention schedules |
| `privacy` | Personal data retention and purge |
| `common`, `config` | Base entity, errors, tokens, audit log, configuration |

Business rules live in the domain objects (`Event`, `NotificationOutbox`, `SuggestionContent`, `DerangementGenerator`) and services, not in controllers or React components.

### Frontend (`frontend/src`)

| Folder | Contents |
|---|---|
| `app/` | Router and query client |
| `components/` | UI kit, layout and form controls |
| `features/` | Auth, events, registration, wishlist, draw, admin, account |
| `services/` | Typed API clients |
| `lib/` | API client with CSRF handling, error messages, time-zone helpers, safe URL helpers |
| `pages/` | Route components |

## Technology stack

- **Backend:** Java 25, Spring Boot 4.1.3 (Web MVC, Security, Data JPA/Hibernate 7, Validation, Actuator, Scheduling, Mail, Session JDBC), Flyway, Thymeleaf (email templates), springdoc-openapi, Bucket4j, libphonenumber, BouncyCastle (Argon2), Maven.
- **Tests:** JUnit 5, Mockito, AssertJ, Testcontainers (PostgreSQL 17), Spring Boot Test, Awaitility, JaCoCo; Vitest; Playwright.
- **Frontend:** React 19, TypeScript 6, Vite 8, Tailwind CSS 4, React Router 8, TanStack Query 5, React Hook Form 7, Zod 4, qrcode, IBM Plex Sans and Mono (self-hosted via Fontsource).
- **Database:** PostgreSQL 17.
- **Infrastructure:** Docker, Docker Compose, Azure Container Apps, Azure Database for PostgreSQL Flexible Server, Azure Container Registry, Azure Key Vault, Azure Static Web Apps, Application Insights and Log Analytics, Cloudflare, GitHub Actions.

## Security

### Authentication

- Server-side sessions stored in PostgreSQL (Spring Session JDBC).
- The `KSM_SESSION` cookie is `HttpOnly`, `SameSite=Lax`, and `Secure` in production. The session ID rotates at login, and the idle timeout is 12 hours.
- Passwords are hashed with Argon2id. The password policy requires 10–128 characters and rejects very common passwords and the user's own email.
- Brute-force protection:
  - Per-account lockout: 5 failures cause a 15-minute lock.
  - Per-IP and per-account rate limits.
  - Generic error messages.
  - Registration and password reset do not reveal whether an account exists.
- Verification and reset tokens are 256-bit random values. They are single use and expire after 24 hours and 1 hour respectively. Only an HMAC-SHA256 with a server-side pepper is stored.
- Links put tokens in the URL fragment, so they never reach server or proxy logs. The frontend removes the fragment immediately.
- A password reset or change terminates all of the user's sessions.

### Authorization

- Spring Security role checks: `/api/admin/**` requires `ADMIN`; every other `/api/**` endpoint requires authentication.
- Every participant-facing query is scoped to the authenticated user's own participation in the requested event. Any other event returns `404`.
- No endpoint accepts a recipient ID: the assignment endpoint derives the recipient from the caller's own assignment, which prevents IDOR/BOLA.

### Assignment privacy

- There is no API endpoint, admin view or generic repository method that returns the assignment mapping. `AssignmentRepository` deliberately extends Spring Data's plain `Repository` interface, so it has no `findAll`.
- Outbox rows store only the participant ID. The message content is built at send time, and logs and audit entries contain only counts and IDs.

### CSRF, CORS and headers

- **CSRF:** a session-bound synchronizer token. The SPA fetches it from `/api/auth/csrf` and sends it as `X-CSRF-TOKEN`.
- **CORS:** a single configured origin, with credentials allowed.
- **Backend headers:** CSP, HSTS, `X-Frame-Options: DENY`, `nosniff`, `Referrer-Policy: no-referrer` and a Permissions-Policy.
- **Frontend headers:** Static Web Apps sends a strict CSP and the same headers, generated by `frontend/scripts/write-swa-config.mjs`.

### Input safety

- Bean Validation on every request.
- Phone numbers are normalized to E.164.
- Wishlist URLs must be absolute `http(s)` URLs without credentials or whitespace. The rule is enforced in Java, in the frontend schema and by a PostgreSQL `CHECK` constraint.
- URLs are never fetched server-side, so there is no SSRF surface.
- React escapes all output, and `dangerouslySetInnerHTML` is banned by an ESLint rule.
- External links open with `rel="noopener noreferrer nofollow"`.

### Errors and logs

- Responses use RFC 7807 problem details with a stable `code` and no stack traces or messages.
- Request records override `toString()` to redact passwords, tokens and codes.

### Admin provisioning

There is no default administrator and no public way to become one. An operator promotes an existing, verified account with a one-off command (see [Local development](#local-development)).

### Privileged-access limitation

Hiding assignments in the application does **not** protect them from someone with direct database access, infrastructure access or access to the email/SMS provider's logs. Anyone with database credentials can join `assignments` to `users`. This risk is reduced by:

- a private database network;
- secrets kept in Key Vault;
- least-privilege access to the Azure resource group.

Encrypting assignments so that operators cannot read them is listed under future improvements.

## Database design

Flyway migrations live in `backend/src/main/resources/db/migration`.

| Table | Purpose and key constraints |
|---|---|
| `users` | Lower-cased unique email, E.164 phone check, Argon2 hash, lockout counters, privacy notice acceptance |
| `user_roles` | `ADMIN` / `PARTICIPANT` per user (one account can hold both) |
| `auth_tokens` | Hashed verification and reset tokens, expiry, single use |
| `events` | Lifecycle status with consistency checks; draw mode, times (UTC) and IANA time zone |
| `invitation_tokens` | HMAC of the code; partial unique index allowing one active (non-revoked) invitation per event |
| `event_participants` | `UNIQUE(event_id, user_id)` and `UNIQUE(event_id, id)` for composite foreign keys; `ACTIVE` / `WITHDRAWN` |
| `gift_suggestions` | Position 1–3, unique per participant; the type/content `CHECK` only allows `http(s)` URLs |
| `assignments` | Composite foreign keys keep giver and recipient in the same event; `UNIQUE(event, giver)`, `UNIQUE(event, recipient)`, `CHECK(giver <> recipient)` |
| `notification_outbox` | Status machine, unique idempotency key, attempts, backoff, processing lease |
| `audit_events` | Non-sensitive audit trail |
| `spring_session*` | Server-side sessions |

### Database-level guarantees (migration V2)

- **Status transitions:** a trigger rejects any status transition outside the lifecycle.
- **Assignment immutability:** a trigger rejects every `UPDATE` and `DELETE` on `assignments`. The only exception is a `DELETE` inside the retention purge transaction, which sets `ksm.allow_assignment_purge`.
- **Assignment inserts:** allowed only while the event's registration is closed, and only between active participants.
- **Draw completeness:** a deferred constraint trigger checks, at commit time, that a `DRAW_COMPLETED` event has exactly one assignment per active participant as giver and as recipient.
- **Frozen participants:** participants cannot be added or changed after the draw or after cancellation.

### Concurrency

- The draw, join, withdrawal and admin removal all take a `SELECT … FOR UPDATE` lock on the event row. This serializes draws against each other and against participant changes.
- A second draw request waits for the lock, then sees `DRAW_COMPLETED` and gets `409`.
- The outbox claim uses `FOR UPDATE SKIP LOCKED` with a processing lease, so a crashed worker's messages are retried after 5 minutes.

## Getting started

### Prerequisites

- Docker with Docker Compose
- Java 25 and Maven 3.9+ (for running backend tests outside Docker)
- Node.js 24

### Run locally

```bash
cp .env.example .env
# Set DATABASE_PASSWORD and TOKEN_PEPPER (e.g. `openssl rand -base64 48`)
docker compose up -d --build        # PostgreSQL, Mailpit, backend on :8080
cd frontend && npm ci && npm run dev  # http://localhost:5173 (proxies /api to :8080)
```

- Mailpit (captured emails): http://localhost:8025
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## Local development

To create the first administrator, register through the UI, confirm the email in Mailpit, then run:

```bash
make promote-admin EMAIL=organizer@example.com
```

The command runs the backend image in non-web mode. It grants `ADMIN` to the existing verified account and signs out that account's sessions. Log in again to see the "Organizator" tab.

Useful targets:

```bash
make up            # build and start the Docker stack
make logs          # backend logs
make backend-test  # mvn verify (unit + Testcontainers integration tests)
make frontend-check
make e2e           # Playwright (requires the stack and the Vite dev server)
```

## Docker setup

- `backend/Dockerfile` is a multi-stage build. It uses a layered Spring Boot jar on `eclipse-temurin:25-jre-alpine`, runs as a non-root user, and includes a health check on `/actuator/health/liveness`.
- `compose.yaml` runs:
  - PostgreSQL 17 with a persistent volume and a health check;
  - Mailpit as the local SMTP server and inbox;
  - the backend.
- All ports bind to `127.0.0.1` only. Flyway migrates the schema on startup.

## Environment variables

| Variable | Description | Default |
|---|---|---|
| `DATABASE_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/ksm_connect` |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | Database credentials | `ksm` / — |
| `DATABASE_POOL_SIZE` | Hikari maximum pool size | `10` |
| `TOKEN_PEPPER` | **Required.** At least 32 bytes; HMAC key for all stored tokens | — |
| `FRONTEND_BASE_URL` | Used in email links and invitation URLs | `http://localhost:5173` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins | `http://localhost:5173` |
| `SESSION_COOKIE_SECURE` | Secure session cookie (forced `true` in the `prod` profile) | `false` |
| `CLIENT_IP_HEADER` | Trusted client-IP header (`CF-Connecting-IP` in `prod`) | empty |
| `RATE_LIMITING_ENABLED` | Application rate limits | `true` |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | SMTP settings | Mailpit |
| `SMS_PROVIDER` | `none` or `smsapi` | `none` |
| `SMSAPI_TOKEN`, `SMS_SENDER_NAME` | SMSAPI.pl OAuth token and registered sender name | — |
| `API_DOCS_ENABLED` | Swagger/OpenAPI (disabled by default in `prod`) | `true` |
| `SCHEDULING_ENABLED` | Scheduler and outbox worker | `true` |
| `SPRING_PROFILES_ACTIVE` | Use `prod` in production | — |
| `VITE_API_BASE_URL` (frontend build) | API origin, e.g. `https://api.example.pl` | same origin |

## API documentation

Interactive OpenAPI docs are served at `/swagger-ui.html`; they are disabled by default in production. Main endpoints:

| Area | Endpoints |
|---|---|
| Auth | `GET /api/auth/csrf`, `GET /api/auth/session`, `POST /api/auth/{register,login,logout,verify-email,resend-verification,change-password}`, `POST /api/auth/password-reset/{request,confirm}` |
| Profile | `GET/PUT/DELETE /api/me` |
| Participant | `GET /api/events`, `GET /api/events/{id}`, `POST /api/events/{id}/withdraw`, `POST /api/invitations/{preview,accept}` |
| Wishlist | `GET /api/events/{id}/wishlist`, `PUT/DELETE /api/events/{id}/wishlist/{position}` |
| Assignment | `GET /api/events/{id}/assignment` (own assignment only) |
| Admin | `GET/POST /api/admin/events`, `GET/PUT /api/admin/events/{id}`, `POST …/{open-registration,close-registration,cancel,invitation,draw}`, `GET …/participants`, `DELETE …/participants/{pid}`, `GET …/notifications`, `POST …/notifications/{nid}/retry`, `POST …/notifications/retry-failed`, `GET …/audit` |

Errors follow RFC 7807 with a machine-readable `code`, for example `DRAW_ALREADY_COMPLETED`. The frontend maps each code to a Polish message.

## Testing

```bash
cd backend && mvn verify     # 117 unit + 72 integration tests (Docker required)
cd frontend && npm test      # 39 unit tests (Vitest)
cd frontend && npx playwright test   # E2E: desktop, 360px and 390px projects
```

- **Drawing algorithm:** thousands of generated draws for 2, 3, 5, 10, 30, 100 and 1,000 participants, each checked against every invariant.
  - A chi-square check confirms uniformity across all nine derangements of 4 participants.
  - Other tests cover concurrent generation, a broken random source, and validator unit tests for each kind of violation.
  - These are statistical tests, not a formal proof. Correctness rests on the invariant validation that runs before anything is persisted, plus the database constraints.
- **Integration tests** run against real PostgreSQL (Testcontainers). They cover:
  - constraints and triggers (immutability, self-assignment, cross-event assignments, completeness);
  - rollback of a draw that fails before commit;
  - 8 concurrent draw requests producing exactly one draw;
  - concurrent joins;
  - outbox retry, backoff, permanent failure and admin retry, with assignments unchanged;
  - stale lease recovery and concurrent workers delivering exactly once;
  - the scheduled draw, retention purge and account deletion policy.
- **Security tests:**
  - anonymous access, role escalation and cross-event access (IDOR);
  - admin endpoints never containing assignment or wishlist data;
  - CSRF rejection, lockout, single-use tokens and session termination after a password reset;
  - security headers and input validation.
- **End-to-end tests** cover the full journey:
  - the admin creates an event and generates a code and QR;
  - participants register through the QR link and verify their email via Mailpit;
  - participants join, edit wishlists, and an unsafe URL is rejected;
  - the admin closes registration and runs the draw, and a second draw is rejected;
  - emails are delivered, and each participant reveals their own recipient and the recipient's wishlist.
  - They also cover invalid and revoked codes, blocked draws, concurrent draw requests, participants being denied admin access, and mobile layout (no horizontal scroll, touch targets of at least 44px).

To run E2E locally, set `RATE_LIMITING_ENABLED=false` in `.env`, run `docker compose up -d`, then run `npx playwright test` in `frontend/`. Playwright starts Vite itself.

## CI/CD

- **`.github/workflows/ci.yml`** runs on every pull request and push. All jobs must pass:
  - backend compile, unit and Testcontainers integration tests, with the JaCoCo report as an artifact;
  - frontend type-check, lint, unit tests, build and `npm audit`;
  - Trivy scans of the filesystem (vulnerabilities, secrets, IaC misconfigurations) and of the Docker image;
  - Playwright E2E against the Compose stack.
- **`.github/workflows/deploy.yml`** runs on `main`:
  1. Re-runs CI.
  2. Uses the protected `production` GitHub environment, where you configure required reviewers.
  3. Logs in to Azure with OIDC (no stored Azure password).
  4. Builds, pushes and scans the image in ACR.
  5. Updates the Container App and deploys the SPA to Static Web Apps.
  6. Runs smoke tests: readiness, anonymous session, admin endpoint returns `401`, frontend loads.
- **`.github/dependabot.yml`** keeps Maven, npm, Actions and Docker dependencies updated.

## Production deployment

See [docs/deployment.md](docs/deployment.md) for the step-by-step Azure and Cloudflare setup. The infrastructure is defined in `infra/main.bicep`:

- **Container Apps:** Consumption profile, VNet-integrated, pinned to exactly one replica so the scheduler and outbox always run. Ingress accepts only Cloudflare IP ranges. Secrets come from Key Vault through a managed identity.
- **PostgreSQL Flexible Server:** Burstable B1ms with private VNet access only. TLS is required and automated backups are on.
- **Other resources:** ACR Basic (admin user disabled), Key Vault (RBAC, purge protection), Log Analytics, Application Insights, Static Web Apps (Free) and a restart alert.

## Future improvements

- Admin MFA (TOTP). Admins currently rely on a strong password, lockout and rate limiting.
- Exclusion rules for the draw (e.g. couples or siblings), solved via constrained matching instead of plain derangements.
- Per-event notification templates (the current copy is written for Wigilia KSM).
- Encryption of assignments with keys unavailable to database operators.
- In-app QR scanning with the camera (today participants use the phone's native camera app or type the code).
- Budget limits, event history, richer gift preferences, analytics.
- Distributed rate limiting (e.g. Redis) if the backend is ever scaled beyond one replica.
