# Backup and restore

## What Azure provides (Flexible Server, Burstable B1ms)

- **Backups:** automated daily full backups plus continuous transaction log backup. Point-in-time restore (PITR) to any second within the retention window.
- **Retention:** 14 days (PROD), 7 days (TEST).
- **Encryption:** backups are encrypted at rest with service-managed keys, like the data files.
- **Redundancy:** locally redundant. Geo-redundant backup is not enabled; it costs extra and protects against loss of a whole region.

## Targets

| | Target | Basis |
|---|---|---|
| RPO (data loss) | ≤ 5 minutes | Continuous WAL backup used by PITR |
| RTO (downtime) | ≤ 1 hour | Restore to a new server (typically 10–30 minutes for a small DB) plus repointing the app |

These are targets. **They are validated only once a restore test has been executed** (see below).

## Restore procedure

PITR always creates a **new** server; the original is never overwritten.

```bash
az postgres flexible-server restore -g rg-ksm-prod \
  --name ksm-prod-pg-restored-$(date +%Y%m%d%H%M) \
  --source-server <current-server-name> \
  --restore-time "2026-12-20T18:30:00Z"
```

The restored server inherits the VNet and private DNS configuration. Then:

1. Point the app at the new server (rebuild the JDBC URL with the new FQDN):

   ```bash
   az containerapp update -g rg-ksm-prod -n ksm-prod-app \
     --set-env-vars "DATABASE_URL=jdbc:postgresql://<new-fqdn>:5432/ksm_connect?sslmode=require"
   ```

2. Run the smoke test.
3. Keep the old server, stopped, until the incident is closed, then delete it.

Remember that `DATABASE_URL` is also set by Bicep. Update `infra/` (or pass the new server name) before the next infrastructure deployment so it does not revert.

## Restore test log

| Date | Environment | Restore point | Duration | Result |
|---|---|---|---|---|
| – | TEST | – | – | **Not yet executed.** Required before go-live (production-deployment.md §8). |
