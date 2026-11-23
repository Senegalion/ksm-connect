# Incident response

## First five minutes

1. Is the app up? Run `./infra/scripts/smoke-test.sh <prodAppUrl>`.
2. Check the latest revision and its health:

   ```bash
   az containerapp revision list -g rg-ksm-prod -n ksm-prod-app -o table
   ```

3. Check recent logs:

   ```bash
   az containerapp logs show -g rg-ksm-prod -n ksm-prod-app --tail 200
   ```

4. If the problem started with a deployment, **roll back first** ([rollback.md](../deployment/rollback.md)) and investigate afterwards.

## Common scenarios

| Symptom | Likely cause | Action |
|---|---|---|
| Readiness DOWN, logs show connection errors | Database stopped or unreachable | `az postgres flexible-server show … --query state`; start it if it is `Stopped` |
| New revision Unhealthy after deploy | Startup failure, often a Flyway migration | Old revision keeps serving. Fix forward or redeploy the previous image. |
| Emails not arriving | ACS throttling (managed domain), domain not verified, identity role missing | Organizer panel shows error codes; check `EMAIL_ACS_*` in logs; requeue after fixing |
| Login attempts rejected with 429 | Rate limiting (per IP / per account) | Expected under attack; legitimate users wait ≤ 15 minutes |
| Scheduled draw did not run | Fewer than 2 participants, or app was down | Panel shows "Losowanie automatyczne wymaga uwagi"; draw manually |

## Inspecting failed notifications

The organizer panel lists every message with status and error code. Retrying does not change the drawing: *Ponów nieudane*, or *Ponów* per message.

## Rotating secrets

| Secret | How | Side effects |
|---|---|---|
| Database password | `az postgres flexible-server update … --admin-password <new>`, then `az keyvault secret set … database-password`, then restart the revision | None if done back to back |
| Token pepper | `az keyvault secret set … token-pepper`, then restart | Outstanding verification/reset links and the active invitation code stop working; regenerate the invitation |
| Mailpit password (TEST) | Update the Key Vault secret `mailpit-ui-auth` (`ksm:<password>`) and the GitHub secret, then restart Mailpit | – |

Restart a revision:

```bash
az containerapp revision restart -g <rg> -n <app> --revision <name>
```

## After the incident

Write down a short timeline, the cause and the follow-up, and add a regression test if the cause was a bug.
