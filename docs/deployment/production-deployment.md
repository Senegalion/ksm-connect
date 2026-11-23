# First-time provisioning and production deployment

This runbook takes an empty Azure subscription to a running TEST and PROD. Later releases only need a merge to `main` and an approval (see [ci-cd.md](ci-cd.md)).

## 0. Prerequisites

- An Azure subscription where you are **Owner**. Owner is needed because the templates create role assignments.
- Azure CLI ≥ 2.60 and `az login`.
- A clean git working tree on the commit you want to deploy.

Register the resource providers once. A new subscription has most of them unregistered:

```bash
for ns in Microsoft.App Microsoft.ContainerRegistry Microsoft.DBforPostgreSQL Microsoft.KeyVault \
          Microsoft.ManagedIdentity Microsoft.Network Microsoft.OperationalInsights Microsoft.Insights \
          Microsoft.Communication; do
  az provider register --namespace "$ns" --wait
done
```

## 1. Registry and GitHub identities

```bash
export ALERT_EMAIL=you@example.com
./infra/scripts/deploy-infrastructure.sh registry what-if
./infra/scripts/deploy-infrastructure.sh registry apply
```

Note the outputs: `registryName`, `registryLoginServer` and the three `github*ClientId` values.

## 2. Build the first image

```bash
image=$(./infra/scripts/build-image.sh <registryName>)
echo "$image"   # <registry>.azurecr.io/ksm-connect@sha256:...
```

This uses ACR Tasks (a cloud build), so no Docker credentials are needed on the laptop.

## 3. TEST and PROD environments

```bash
./infra/scripts/deploy-infrastructure.sh environments what-if "$image"
./infra/scripts/deploy-infrastructure.sh environments apply "$image"
```

The script:

- generates the database passwords, token peppers and the Mailpit password on the first run;
- reuses them from Key Vault on later runs;
- passes them through a temporary `600`-mode file that is deleted on exit;
- never prints them.

If the first `apply` fails with a Key Vault or registry authorization error, it is usually Azure RBAC propagation delay. Wait two minutes and run the same `apply` again; it is idempotent.

Outputs include `testAppUrl`, `testMailpitUrl`, `prodAppUrl` and `prodEmailManagedDomain`.

## 4. Configure GitHub

```bash
repo=Senegalion/ksm-connect
gh variable set AZURE_TENANT_ID --repo $repo --body "$(az account show --query tenantId -o tsv)"
gh variable set AZURE_SUBSCRIPTION_ID --repo $repo --body "$(az account show --query id -o tsv)"
gh variable set AZURE_BUILD_CLIENT_ID --repo $repo --body "<githubBuildClientId>"
gh variable set ACR_NAME --repo $repo --body "<registryName>"
gh variable set ACR_LOGIN_SERVER --repo $repo --body "<registryLoginServer>"

gh api -X PUT repos/$repo/environments/test >/dev/null
gh variable set AZURE_CLIENT_ID --repo $repo --env test --body "<githubTestClientId>"
gh variable set RESOURCE_GROUP --repo $repo --env test --body rg-ksm-test
gh variable set APP_NAME --repo $repo --env test --body ksm-test-app
gh variable set APP_URL --repo $repo --env test --body "<testAppUrl>"
gh variable set MAILPIT_URL --repo $repo --env test --body "<testMailpitUrl>"
az keyvault secret show --vault-name <test-vault> -n mailpit-ui-auth --query value -o tsv | cut -d: -f2- \
  | gh secret set MAILPIT_PASSWORD --repo $repo --env test

gh api -X PUT repos/$repo/environments/production \
  -F "reviewers[][type]=User" -F "reviewers[][id]=$(gh api user --jq .id)" \
  -F "deployment_branch_policy[protected_branches]=true" -F "deployment_branch_policy[custom_branch_policies]=false" >/dev/null
gh variable set AZURE_CLIENT_ID --repo $repo --env production --body "<githubProdClientId>"
gh variable set RESOURCE_GROUP --repo $repo --env production --body rg-ksm-prod
gh variable set APP_NAME --repo $repo --env production --body ksm-prod-app
gh variable set APP_URL --repo $repo --env production --body "<prodAppUrl>"
```

## 5. Administrator accounts

Neither environment has a default administrator.

### TEST E2E administrator

1. Register `e2e-admin@e2e.ksm.local` with a generated password in the TEST UI.
2. Open its verification email in Mailpit (`testMailpitUrl`, user `ksm`).
3. Promote the account (same command as PROD, with `rg-ksm-test` / `ksm-test-app`).
4. Store `E2E_ADMIN_EMAIL` and `E2E_ADMIN_PASSWORD` as `test` environment secrets.

### PROD organizer

Register in the PROD UI with a real address, confirm the email, then run:

```bash
az containerapp exec -g rg-ksm-prod -n ksm-prod-app --command \
  "env JAVA_TOOL_OPTIONS=-Xmx192m java -jar /app/ksm-connect.jar \
   --spring.main.web-application-type=none --ksm.scheduling.enabled=false \
   --ksm.tasks.promote-admin=organizer@example.com"
```

The command starts a short-lived second JVM inside the running container. That JVM uses the same configuration and managed identity, grants `ADMIN`, signs out the account's sessions and exits.

## 6. Production email domain

PROD starts with an Azure-managed sender domain (`DoNotReply@<guid>.azurecomm.net`). It works without DNS, but Azure throttles managed domains heavily and messages may land in spam. **Before inviting participants, switch to a custom domain:**

1. In the Azure portal, open the Email Communication Service in `rg-ksm-prod` → *Provision domains* → *Add domain* → *Custom domain* → `ksm-connect.pl` (or a subdomain such as `mail.ksm-connect.pl`).
2. Add the TXT (verification), SPF and DKIM (two CNAME) records it shows in Cloudflare as **DNS only**.
3. Add DMARC: TXT `_dmarc` = `v=DMARC1; p=quarantine; rua=mailto:<you>`.
4. After verification, connect the domain to the Communication Service (*Domains → Connect*).
5. Redeploy with `PROD_EMAIL_SENDER_ADDRESS=DoNotReply@ksm-connect.pl ./infra/scripts/deploy-infrastructure.sh environments apply "$image"`.
6. Send yourself a password reset and check the headers: SPF, DKIM and DMARC should all show `pass`.

## 7. Custom domain and Cloudflare

1. Add the domain to Cloudflare and switch the registrar's nameservers.
2. Bind the hostname to the Container App:

   ```bash
   az containerapp hostname add -g rg-ksm-prod -n ksm-prod-app --hostname ksm-connect.pl
   # create the TXT asuid record and the CNAME/A record it prints, DNS only (grey cloud)
   az containerapp hostname bind -g rg-ksm-prod -n ksm-prod-app --hostname ksm-connect.pl \
     --environment ksm-prod-env --validation-method CNAME
   ```

3. Redeploy infrastructure with `PROD_CUSTOM_HOSTNAME=ksm-connect.pl`, so email links and CORS use the new origin.
4. Switch the record to *Proxied*, set SSL mode to **Full (strict)**, then enable origin protection ([network-security.md](../architecture/network-security.md#enabling-cloudflare-origin-protection-after-the-domain-is-live)).
5. **Cloudflare security rules:**
   - managed WAF ruleset on;
   - rate limit `/api/auth/*` (for example 20 requests per minute per IP);
   - Bot Fight Mode on;
   - "Always use HTTPS" on.

## 8. Go-live checklist

- `./infra/scripts/smoke-test.sh <prodAppUrl>` passes.
- Application Insights *Live metrics* shows the app, and the availability test is green.
- The alert email arrives (*Test action group* in the portal).
- PostgreSQL backup retention is 14 days (`az postgres flexible-server show -g rg-ksm-prod -n <server> --query backup`).
- A restore has been tested on TEST ([backup-and-restore.md](../operations/backup-and-restore.md)).
- The email sender uses the custom domain and SPF/DKIM/DMARC pass.
- The privacy notice has been reviewed by the KSM data controller.
