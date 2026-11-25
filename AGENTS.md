# AGENTS.md

Guidance for AI coding agents (Claude Code, Codex, Copilot, Cursor and others) working in this repository.

## Project

KSM Connect is a web application for the Catholic Youth Association (KSM) used to run events with a Secret Santa draw. Organizers create an event, share an invitation code or QR, participants register, add up to three gift suggestions, the system draws pairs (derangement: everyone gives and receives exactly once, nobody draws themselves) and notifies everyone by email.

Production is live at https://ksmconnect.pl with real users and real data. Treat every production action as high risk.

## Hard rules

1. **Never commit, push, merge, tag or create releases.** The owner does all git and GitHub write operations. Prepare changes on a feature branch, verify them, then give the exact commands to run.
2. **No comments in source code or configuration files** (Java, TypeScript, SQL, YAML, Bicep, shell, Dockerfile). Use expressive names instead. Markdown docs are fine.
3. **UI text is Polish, code is English** (identifiers, logs, error codes, commits, docs in `docs/`).
4. **Talk to the owner in Polish**, concisely, with step-by-step instructions when they have to click something.
5. **Protect production data.** Database migrations must be additive and backward compatible with the previous app version (rolling deployment runs both briefly). Never edit an applied Flyway migration; add `V<n+1>__*.sql`. Never drop or rewrite user data without explicit approval.
6. **Never print, log, commit or store secrets** (Key Vault values, tokens, passwords, invitation tokens). Raw invitation tokens are shown once and only their HMAC hash is stored.
7. Do not send personal data of the owner or users to external services.
8. Do not deploy to production yourself. Production changes go through the `Deploy` workflow with manual approval by the owner. Read-only inspection of Azure (`az ... show/list`, logs, App Insights queries) is fine.

## Stack

- Backend: `backend/`, Spring Boot 4.1 on Java 25, Spring Security (session cookie `KSM_SESSION`, Spring Session JDBC, CSRF via `/api/auth/csrf`), Argon2id passwords, Flyway, PostgreSQL 17, transactional outbox for email and SMS, Bucket4j rate limiting.
- Frontend: `frontend/`, React 19, Vite, TypeScript, TanStack Query, React Hook Form + Zod, Tailwind 4 with KSM brand tokens (blue, yellow, red, white). Built into the backend image and served by Spring (`SpaForwardingController`).
- Packaging: one Docker image built from the root `Dockerfile` (includes the Application Insights Java agent, checksum verified).
- Infrastructure: `infra/` Bicep at subscription scope, Azure Container Apps, PostgreSQL Flexible Server (private VNet), Key Vault, Azure Communication Services Email, Log Analytics and Application Insights. DNS in Cloudflare (DNS only, not proxied), registrar OVH.

Backend packages in `com.ksmconnect`: `admin`, `auth`, `common`, `config`, `draw`, `event`, `invitation`, `notification`, `participant`, `privacy`, `scheduler`, `security`, `user`, `web`, `wishlist`.

## Domain invariants

- The draw is a shuffled derangement validated in code and enforced by PostgreSQL triggers (`V2__integrity_triggers.sql`): one assignment per giver and per receiver, no self assignment, complete coverage, immutable after completion, single draw per event.
- Event registration states: `NOT_STARTED`, `OPEN`, `CLOSED`. Reopening moves a future `opensAt` to now; a passed deadline cannot be reopened.
- Wishlists stay editable after registration closes and after the draw, until the optional wishlist deadline.
- A revealed scratch card stays revealed (`assignment_revealed_at`).
- Gift links: up to 8192 characters; advertising tracking parameters are stripped on save (`TrackingParameters`).
- An admin can also be a participant.
- The organizer cannot see the full list of pairs.
- SMS is implemented (SMSAPI) but disabled in production (`SMS_PROVIDER=none`). Outbox entries are created at draw time, so SMS must be enabled before a draw to be sent.

## Commands

```bash
make up                 # local stack: postgres, mailpit (http://localhost:8025), app on http://localhost:8080
make down
make logs
make backend-test       # mvn -B verify, unit + integration tests with Testcontainers
make frontend-check     # typecheck, lint, unit tests, build
make e2e                # Playwright, needs the stack running
make promote-admin EMAIL=someone@example.com   # local only
```

Before handing over any change run the relevant checks: `cd backend && mvn -B verify`, `cd frontend && npx tsc -b && npm run lint && npm test`, and for user-facing flows `docker compose up -d --build backend` plus `E2E_BASE_URL=http://localhost:8080 npx playwright test` and `./infra/scripts/smoke-test.sh http://localhost:8080`. For migrations, test against the populated local database and compare row counts before and after.

## CI/CD

- `.github/workflows/ci.yml` on pull requests: backend, frontend, dependency review, Trivy scans, container build, E2E. SonarCloud quality gate is attached. CodeQL in `codeql.yml`.
- `.github/workflows/deploy.yml` on push to `main`: verify, build the image once, push to ACR by digest, wait for approval in the `production` environment, roll out, run smoke tests.
- Third-party actions are pinned to commit SHAs. Keep it that way.
- Dependabot is grouped weekly; Spring Boot major or minor, springdoc, TypeScript and `@types/node` majors are ignored on purpose.
- GitHub OIDC federated credentials use the immutable subject form `repo:Senegalion@133698826/ksm-connect@1468039431:...`.
- Releases are informational only (`v1.0.0` launch, `v1.0.1` long gift links). They do not deploy anything.

## Production reference

| Item | Value |
|---|---|
| Subscription | Azure subscription 1, region `polandcentral` |
| Resource groups | `rg-ksm-prod`, `rg-ksm-shared` (ACR `ksmacr3q3gxu2h5zqms`) |
| Container app | `ksm-prod-app` in environment `ksm-prod-env` |
| Admin task job | `ksm-prod-admin-task` (run with env vars, for example `KSM_TASKS_PROMOTE_ADMIN`, because `az containerapp exec` needs a TTY) |
| Database | `ksm-prod-pg-x6zegozkxqpng`, private network only, point in time restore 14 days |
| Key Vault | `ksm-prod-kv-x6zegozkxqpn` |
| Monitoring | `ksm-prod-insights`, `ksm-prod-logs`, alerts to the owner by email |
| Email | ACS, sender `DoNotReply@ksmconnect.pl`, SPF, DKIM and DMARC verified |
| Domains | `ksmconnect.pl` (A `134.112.140.159`) and `www` (CNAME), managed certificates |
| Config | `infra/prod.env` (no secrets), secrets in Key Vault |

TEST environment is disabled (`deployTestEnvironment=false`) because the subscription allows one Container Apps environment per region.

Useful read-only checks:

```bash
az containerapp revision list -g rg-ksm-prod -n ksm-prod-app --query "[?properties.active]" -o table
curl -s https://ksmconnect.pl/actuator/health
gh run list --branch main --limit 5
```

Logs: query `ContainerAppConsoleLogs_CL` in the Log Analytics workspace `ksm-prod-logs`; requests and exceptions in Application Insights `ksm-prod-insights`.

## Documentation

- `docs/architecture/`: system, infrastructure, network security
- `docs/deployment/`: environments, CI/CD, production deployment, rollback
- `docs/operations/`: monitoring, backup and restore, incident response
- `docs/security/`: overview and checklist

## Current state (update when it changes)

- 2026-11-24: production live, v1.0.1 deployed (long gift links). Planned first real event "Wigilia KSM 2026": registration closes 2026-11-27 23:59, draw 2026-11-28 10:00 (Europe/Warsaw); confirm the actual settings with the owner. Avoid risky deployments around the draw.
- Backlog: Cloudflare proxy with origin certificate and ingress restricted to Cloudflare IPs (`CLIENT_IP_HEADER=CF-Connecting-IP`), backup restore drill, SMS via SMSAPI if the owner wants it, formal privacy notice review by the KSM board, TEST environment after a quota increase.
