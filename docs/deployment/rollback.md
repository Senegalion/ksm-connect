# Rollback

## Application rollback

Every deployment creates a new Container App revision. Up to 5 inactive revisions are kept, and every image digest stays locked in the registry.

**Fastest option: re-deploy the previous image.** `rollout.sh` prints `Previous image:` in the deploy log:

```bash
./infra/scripts/rollout.sh rg-ksm-prod ksm-prod-app "<previous image@sha256:...>"
./infra/scripts/smoke-test.sh "<prodAppUrl>"
```

Alternatively, re-run the *Deploy* workflow for the last good commit with *Re-run all jobs*. It rebuilds nothing new, because the locked tag already exists.

If a new revision fails its health checks, `rollout.sh` exits with an error. In single-revision mode Azure keeps traffic on the last healthy revision, so a broken release does not take PROD down.

## Database and migrations

Application rollback does **not** roll back Flyway migrations. To keep rollbacks safe:

- **Additive migrations only:** new nullable columns, new tables, new indexes. All migrations so far (V1–V3) are additive.
- **Removing or renaming (expand and contract):** release N adds the new structure and writes both; release N+1 stops using the old one; release N+2 drops it. The previous application version therefore always works with the current schema.
- **Never edit an applied migration.** Flyway validates checksums and would refuse to start.

If a migration itself corrupts data, restore the database with point-in-time restore to a new server ([backup-and-restore.md](../operations/backup-and-restore.md)), point the app at it, and redeploy the previous image.

## Notifications

Retries are idempotent with respect to the drawing: requeuing a notification never touches assignments. Failed messages can be retried from the organizer panel (*Powiadomienia → Ponów nieudane*). Messages stuck in `PROCESSING` after a crash are reclaimed automatically after 5 minutes.

## Scheduler

Scheduled draws and registration closing are stored in PostgreSQL and evaluated every 30 seconds. After any outage, the next tick processes everything that became due. A draw is never executed twice, because the event row lock, the status check and the database triggers prevent it.
