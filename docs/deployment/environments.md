# Environments

| | LOCAL | TEST | PROD |
|---|---|---|---|
| Purpose | Development | Automated E2E, ZAP scan, acceptance testing | Real KSM participants |
| Runs on | Docker Compose on the laptop | `rg-ksm-test` | `rg-ksm-prod` |
| Database | Postgres container with a named volume | Own Flexible Server | Own Flexible Server |
| Secrets | `.env` (git-ignored, random local values) | Own Key Vault | Own Key Vault |
| Email | Mailpit (`localhost:8025`) | Mailpit Container App (basic auth) | Azure Communication Services |
| Data | Synthetic | Synthetic only (`@e2e.ksm.local` accounts) | Real |
| Deploy identity | – | `id-github-deploy-test`, Contributor on `rg-ksm-test` only | `id-github-deploy-prod`, Contributor on `rg-ksm-prod` only |
| Approval | – | Automatic after CI | Manual approval in the `production` GitHub environment |

## Isolation guarantees

- **Identity and network:** each environment has its own resource group, VNet, database server, Key Vault, managed identity and log workspace. The TEST deploy identity has no role on the PROD resource group, and the reverse is also true.
- **Mail:** TEST cannot send real email. Its SMTP host is the internal Mailpit app and it has no Communication Services resource. SMS is disabled in every environment.
- **Database:** LOCAL cannot reach either cloud database, because both have public network access disabled.
- **Secrets:** they are generated per environment by `deploy-infrastructure.sh` and never shared.

## LOCAL quick reference

```bash
cp .env.example .env          # then set random DATABASE_PASSWORD and TOKEN_PEPPER
docker compose up -d --build  # app on :8080 (also serves the built SPA), Mailpit on :8025
cd frontend && npm run dev    # optional hot-reload UI on :5173, proxies /api to :8080
make promote-admin EMAIL=you@example.com
```
