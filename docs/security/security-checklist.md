# Security and acceptance checklist

## Before the first production release

| Item | Status |
|---|---|
| All CI gates green on `main` (tests, Trivy, CodeQL, Dependency Review, E2E) | Pending first run on Azure |
| TEST deploy, smoke, E2E and ZAP baseline green | Pending |
| ZAP findings triaged and recorded below | Pending |
| Restore test executed on TEST and logged in backup-and-restore.md | Pending |
| Custom email domain with SPF, DKIM, DMARC passing | Pending (needs domain) |
| Alert email received from the PROD action group | Pending |
| Privacy notice reviewed by the KSM data controller | Pending |
| `production` environment requires manual approval; `main` requires PRs and checks | Pending GitHub configuration |

## Acceptance test in TEST (manual, synthetic accounts)

Use a phone and a laptop. Every account is `@e2e.ksm.local` or a personal test alias; emails appear in Mailpit.

- [ ] The red "ŚRODOWISKO TESTOWE" banner is visible on every page.
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
