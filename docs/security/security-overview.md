# Security overview

Application controls are described in the [README security section](../../README.md#security). This document covers the delivery and runtime controls added for production.

| Area | Control |
|---|---|
| Identity | No passwords for Azure anywhere. GitHub uses OIDC with three narrowly scoped identities; the app uses a managed identity for Key Vault, registry pulls and email. |
| Secrets | Generated per environment, stored only in Key Vault, injected as Container App secret references, never in images, workflows, outputs or logs. |
| Supply chain | Locked image tags, deployment by digest, Trivy scans before push, CodeQL, Dependency Review, Dependabot, App Insights agent verified by SHA-256. |
| Isolation | Separate resource groups, networks, databases, vaults and identities for TEST and PROD; TEST cannot send real email. |
| Transport | HTTPS only with HSTS; database private and TLS-only. |
| Runtime | Non-root container, read-only image layers, 1 GiB memory limit, liveness/readiness/startup probes, `ExitOnOutOfMemoryError`. |
| Verification | Smoke tests check auth boundaries, CSRF, headers, cookie flags and TLS on every deploy; ZAP baseline scans TEST on every deploy. |

## Accepted residual risks

| Risk | Reason accepted | Mitigation / plan |
|---|---|---|
| Database operators can read assignments | Inherent to server-side storage | Private network, RBAC, documented in README; encryption at rest of assignments is a future improvement |
| App connects to PostgreSQL as admin | Time constraint for first release | Least-privilege runtime role planned (checklist) |
| No Cloudflare/WAF until a domain exists | Domain not yet registered | App-level rate limiting, CSRF, lockout; enable Cloudflare per network-security.md |
| Key Vault and ACR reachable on public endpoints | Cost/complexity of private endpoints | Entra ID RBAC only, no keys/admin users |
| Admin has no MFA | Not yet implemented | Strong password policy, lockout, rate limit; TOTP planned |
