# Network and transport security

| Layer | Control | Status |
|---|---|---|
| Client → app | HTTPS only (`allowInsecure: false`, HTTP is redirected), HSTS 1 year, TLS ≥ 1.2 | Implemented; smoke test checks HSTS, redirect and TLS |
| App → database | Private VNet (delegated subnet), `publicNetworkAccess: Disabled`, private DNS zone, `sslmode=require` | Implemented |
| App → secrets | Key Vault references resolved by the user-assigned managed identity; no secret values in the template outputs, image or repository | Implemented |
| App → registry | `AcrPull` via managed identity; registry admin user disabled; anonymous pull disabled | Implemented |
| App → email (PROD) | ACS over HTTPS with managed identity (no SMTP password) | Implemented, needs first live send to verify |
| TEST email | All mail goes to an internal Mailpit; SMTP port is internal to the Container Apps environment | Implemented |
| GitHub → Azure | OIDC federated credentials bound to `ref:refs/heads/main` (build) and to the `test` / `production` GitHub environments (deploy); no stored Azure secrets | Implemented |
| Edge | Cloudflare (DNS, WAF, rate limiting) | **Not yet**: requires a domain |
| Origin protection | Restrict Container App ingress to Cloudflare IP ranges after Cloudflare is active | **Not yet**: see below |

## Client IP and rate limiting

- **Without Cloudflare** (current state), the app trusts only the **right-most** `X-Forwarded-For` entry, which the Azure ingress appends. A client cannot spoof its IP by sending its own header. This is covered by a unit test.
- **With Cloudflare,** set `CLIENT_IP_HEADER=CF-Connecting-IP` only **after** restricting ingress to Cloudflare ranges. Otherwise anyone could forge that header.

## Enabling Cloudflare origin protection (after the domain is live)

1. Proxy the DNS record through Cloudflare (orange cloud) and set SSL mode to **Full (strict)**.
2. Restrict the Container App ingress to Cloudflare ranges (https://www.cloudflare.com/ips-v4 and /ips-v6):

   ```bash
   for cidr in $(curl -s https://www.cloudflare.com/ips-v4); do
     az containerapp ingress access-restriction set -g rg-ksm-prod -n ksm-prod-app \
       --rule-name "cf-${cidr//[\/.]/-}" --ip-address "$cidr" --action Allow
   done
   ```

3. Set `CLIENT_IP_HEADER=CF-Connecting-IP` on the app.
4. Confirm that the `*.azurecontainerapps.io` hostname now returns 403 from a non-Cloudflare IP.

## Known limitations

- `sslmode=require` encrypts the database connection but does not pin the server certificate. Traffic never leaves the private VNet. Switching to `verify-full` needs the Azure root CA bundle in the image and is tracked as a hardening item.
- The application connects to PostgreSQL as the server administrator, which also runs Flyway migrations. A separate least-privilege runtime role (DML only) is the next hardening step (see the security checklist).
- The PROD Key Vault and registry are reachable over their public endpoints, protected by Entra ID RBAC. Private endpoints were not added because each one costs ≈ 7 USD/month and adds DNS complexity, with little benefit at this scale.
