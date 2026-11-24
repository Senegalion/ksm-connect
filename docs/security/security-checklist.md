# Security and acceptance checklist

## Before the first production release

| Item | Status |
|---|---|
| All CI gates green on `main` (tests, Trivy, CodeQL, Dependency Review, E2E) | Pending first run on Azure |
| TEST environment | Deferred: removed because of the Container Apps environment quota; CI E2E against the release image replaces it for now |
| ZAP findings triaged and recorded below | Pending |
| Restore test executed (restore PROD to a new temporary server, verify, delete) and logged in backup-and-restore.md | Pending |
| Custom email domain with SPF, DKIM, DMARC passing | Pending (needs domain) |
| Alert email received from the PROD action group | Pending |
| Privacy notice reviewed by the KSM data controller | Draft label removed at the owner's request on 2026-11-24; formal review by the KSM board still recommended |
| `production` environment requires manual approval; `main` requires PRs and checks | Pending GitHub configuration |

## Acceptance test (manual)

While there is no cloud TEST environment, run this checklist locally (`docker compose up`, emails in Mailpit at `localhost:8025`) on the release commit. After go-live, repeat the core flow once on PROD with a test event and personal aliases, then cancel the event.

- [ ] Register on a phone; the verification email is in Mailpit; the link works and login succeeds.
- [ ] Organizer creates an event, opens registration, generates the code and QR; the QR scanned with a phone opens the join page with the code filled in.
- [ ] A participant joins via QR while logged out: after registering and logging in, the participant lands back on the join page.
- [ ] The wishlist accepts a product link and a description, and rejects `javascript:` links; the counter shows 2/3.
- [ ] Organizer closes registration and runs the draw; the confirmation dialog explains it cannot be undone.
- [ ] Every participant gets an assignment email (check Mailpit), nobody drew themselves, no two people drew the same person.
- [ ] Scratch-off reveal works by finger on a phone; after reload the ticket stays revealed.
- [ ] The recipient's wishlist is visible; an edit made after the draw shows up for the giver.
- [ ] A different participant cannot open another participant's assignment URL (404).
- [ ] The organizer panel shows participants and delivery statuses but no pairs.
- [ ] Invalid and regenerated (old) invitation codes are rejected with a clear message.
- [ ] Desktop and 360 px phone layouts have no horizontal scrolling.

## Hardening backlog (after go-live)

1. Least-privilege database role for the runtime (Flyway keeps the owner role through `spring.flyway.user`).
2. `sslmode=verify-full` with the Azure CA bundle in the image.
3. Cloudflare in front with ingress restricted to Cloudflare ranges.
4. TOTP MFA for administrators.
5. ZAP findings at WARN level tightened to failing rules once triaged.

## Scan results

| Date | Scan | Scope | Result |
|---|---|---|---|
| 2026-11-23 | Trivy image (local) | `ksm-connect` image incl. App Insights agent and Azure SDK | 0 HIGH/CRITICAL (fixed-only) |
| 2026-11-23 | Trivy filesystem (local) | Maven, npm, secrets, Dockerfile | 0 HIGH/CRITICAL |
