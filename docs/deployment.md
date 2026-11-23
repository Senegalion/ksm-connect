# Production deployment (Azure + Cloudflare)

This guide assumes one production resource group. Repeat it with `environmentName=dev` for a development environment.

> `infra/main.bicep` has not yet been compiled or deployed against a real subscription. Run `az bicep build` and a `what-if` deployment first (step 2), and fix any API-version or schema issues the tooling reports.

## 1. Prerequisites

- An Azure subscription and the Azure CLI (`az login`).
- A domain managed in Cloudflare, e.g. `ksm-connect.pl`. The frontend runs on the apex or `app.` and the API on `api.`. Both must share one registrable domain so the session cookie stays same-site.
- An email sender:
  - **Recommended:** Azure Communication Services (ACS) Email with a verified custom domain. Create an Email Communication Service, add and verify your domain (SPF and DKIM records in Cloudflare), and connect it to a Communication Services resource.
  - **SMTP credentials:** create a Microsoft Entra app registration and grant it a role on the ACS resource. The SMTP username is `<acs-resource-name>.<entra-app-id>.<tenant-id>` and the password is the app's client secret. Follow the current Microsoft documentation for "ACS Email SMTP authentication".
  - Any other SMTP provider (Postmark, Mailgun, etc.) also works without code changes.
- **Optional SMS:** an SMSAPI.pl account with an OAuth token and a registered sender name. SMS messages contain emoji and Polish characters, so they are sent as Unicode and use several billable segments each. Check the price per message before enabling SMS.

## 2. Provision infrastructure

```bash
az group create --name ksm-connect-prod --location polandcentral
export POSTGRES_ADMIN_PASSWORD="$(openssl rand -base64 32)"
export TOKEN_PEPPER="$(openssl rand -base64 48)"
export MAIL_USERNAME="<acs-smtp-username>"
export MAIL_PASSWORD="<entra-client-secret>"
# edit infra/main.prod.bicepparam (frontendBaseUrl, mailFrom)
az deployment group what-if -g ksm-connect-prod -f infra/main.bicep -p infra/main.prod.bicepparam
az deployment group create  -g ksm-connect-prod -f infra/main.bicep -p infra/main.prod.bicepparam
```

Check the region's availability and the current prices of each tier with the Azure pricing calculator before deploying. The template uses the smallest sensible tiers:

- Burstable B1ms PostgreSQL;
- Basic ACR;
- Free Static Web Apps;
- Consumption Container Apps with one always-on replica.

Store the generated secrets in a password manager. They are also written to Key Vault.

### Secret rotation

| Secret | How to rotate | Effect |
|---|---|---|
| `TOKEN_PEPPER` | Update the Key Vault secret and restart the revision | Outstanding verification/reset links and the active invitation code stop working. Regenerate the invitation. |
| Database password | Change it on the server, update Key Vault, restart | — |
| SMTP / SMS secrets | Update Key Vault, restart | — |

## 3. GitHub configuration

1. Create an Entra app registration for GitHub Actions with a federated credential for `repo:<owner>/ksm-connect:environment:production`.
2. Grant it `AcrPush` on the registry and `Contributor` on the Container App. Do not grant subscription-wide roles.
3. In the repository, create the environment `production` with required reviewers.
4. Add environment **secrets**:
   - `AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, `AZURE_SUBSCRIPTION_ID`;
   - `AZURE_STATIC_WEB_APPS_API_TOKEN` (from the Static Web App's "Manage deployment token").
5. Add environment **variables**:
   - `ACR_LOGIN_SERVER`, `CONTAINER_APP_NAME`, `AZURE_RESOURCE_GROUP` (from the deployment outputs);
   - `API_BASE_URL` (e.g. `https://api.ksm-connect.pl`);
   - `FRONTEND_BASE_URL` (e.g. `https://ksm-connect.pl`).

## 4. Cloudflare

1. **DNS:**
   - `api` → CNAME to the Container App FQDN (output `backendFqdn`), proxied (orange cloud).
   - Apex or `app` → CNAME to the Static Web App default hostname, proxied.
2. **Custom domains:** add both hostnames in Azure (Container App custom domain with a managed certificate; Static Web App custom domain). Validation may require temporarily switching the record to DNS-only.
3. **SSL/TLS:** set the mode to **Full (strict)**, enable "Always Use HTTPS", and set the minimum TLS version to 1.2.
4. **WAF:** enable the managed rulesets.
5. **Rate limiting:** add rules, for example:
   - `/api/auth/login` and `/api/auth/register`: 20 requests per minute per IP;
   - `/api/invitations/*`: 30 requests per minute per IP.
6. **Origin protection:**
   - The Container App ingress only accepts Cloudflare IPv4 ranges (`allowedIngressCidrs`). Update the list when Cloudflare publishes changes (https://www.cloudflare.com/ips-v4).
   - The backend trusts `CF-Connecting-IP` for rate limiting only because of this restriction.
   - The Static Web App (Free tier) remains reachable directly on its `*.azurestaticapps.net` hostname. It serves only public static files, so this is acceptable.

## 5. First deployment and administrator

1. Push to `main` and approve the `production` deployment.
2. Open the site, register the organizer account and confirm the email.
3. Promote the account with a one-off command using the deployed image:

```bash
az containerapp job create -g ksm-connect-prod -n ksm-promote-admin \
  --environment ksm-prod-env --trigger-type Manual --replica-timeout 300 \
  --image <acr>/ksm-connect-backend:<sha> --registry-server <acr> --registry-identity <identity-id> \
  --secrets "db-password=keyvaultref:<vault-uri>/secrets/database-password,identityref:<identity-id>" \
            "pepper=keyvaultref:<vault-uri>/secrets/token-pepper,identityref:<identity-id>" \
  --env-vars SPRING_PROFILES_ACTIVE=prod DATABASE_URL=<jdbc-url> DATABASE_USERNAME=ksmadmin \
             DATABASE_PASSWORD=secretref:db-password TOKEN_PEPPER=secretref:pepper \
             FRONTEND_BASE_URL=<frontend-url> MAIL_FROM=<sender> \
  --args "--spring.main.web-application-type=none" "--ksm.scheduling.enabled=false" "--ksm.tasks.promote-admin=organizer@example.com"
az containerapp job start -g ksm-connect-prod -n ksm-promote-admin
```

4. Delete the job afterwards.

## 6. Monitoring

- Use Application Insights and Log Analytics for request failures and exceptions. Logs contain IDs and error codes only, never assignments, tokens or message content.
- An alert fires when the backend restarts more than twice in 15 minutes. Add an action group with your email address.
- Notification delivery problems are visible in the organizer panel ("Powiadomienia") and in logs as `Notification … failed with <code>`.

## 7. Before using it with real participants

- Have the KSM data controller review the privacy notice (`frontend/src/pages/PrivacyPage.tsx`) and confirm the legal basis. Likely candidates are legitimate interest or performance of the membership activity, not consent alone.
- Confirm the retention period (90 days after the event) with the data controller.
- Send a test email and SMS to yourself from production.
