# Monitoring and alerting

## Telemetry sources

| Source | What it contains | Where |
|---|---|---|
| Application Insights Java agent (3.7.10, baked into the image) | Requests, dependencies (PostgreSQL, ACS), exceptions, JVM metrics (heap, GC), logs | `ksm-<env>-insights` |
| Container Apps system and console logs | Revision lifecycle, restarts, structured app logs (ECS JSON) | `ksm-<env>-logs` (Log Analytics) |
| Azure Monitor platform metrics | Container CPU/memory/restarts/requests; PostgreSQL CPU, storage, connections | Metrics blade |
| Availability test (PROD) | `GET /actuator/health/readiness` from 3 EU locations every 5 minutes | App Insights → Availability |

Log Analytics keeps data for 30 days. Daily caps (TEST 0.2 GB, PROD 1 GB) bound the cost.

## Alerts (PROD, email action group)

| Alert | Condition | Severity |
|---|---|---|
| Availability | Readiness check fails from ≥ 2 locations | 1 |
| Restarts | > 2 container restarts in 15 minutes | 2 |
| Server errors | > 10 HTTP 5xx responses in 15 minutes | 2 |
| Database storage | > 80 % used | 2 |
| Database CPU | > 85 % average over 15 minutes | 3 |

## Useful queries (Log Analytics)

Failed notifications. Only IDs and error codes are logged, never message content:

```kusto
ContainerAppConsoleLogs_CL
| where Log_s has "Notification" and Log_s has "failed"
| project TimeGenerated, Log_s
| order by TimeGenerated desc
```

Draw executions (audit, no assignment data):

```kusto
ContainerAppConsoleLogs_CL
| where Log_s has "Draw completed"
| project TimeGenerated, Log_s
```

Exceptions by type:

```kusto
AppExceptions | summarize count() by ProblemId | order by count_ desc
```

## Notification health

The organizer panel (*Powiadomienia*) shows each message with status, attempt count and last error code. Outbox backlog can also be checked with a database query during an incident (see [incident-response.md](incident-response.md)).

## What is never logged

Passwords, session IDs, invitation codes, verification/reset tokens, message bodies, recipients' names and assignment pairs. Request records redact sensitive fields in `toString()`, tokens travel in URL fragments (never sent to the server), and the error handler logs only exception types.
