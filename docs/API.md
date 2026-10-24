# API contract

All application routes use HTTP Basic authentication supplied explicitly in the Authorization header. The SPA holds credentials in memory. Only readiness/liveness/info endpoints are public. The API does not enable cross-origin requests. The frontend proxies API calls on the same origin.

| Method | Route | Access | Behavior |
| --- | --- | --- | --- |
| GET | `/api/me` | Viewer or admin | Username and demo-mode flag |
| GET | `/api/incidents` | Viewer or admin | Filtered, paginated list |
| POST | `/api/alerts` | Admin/responder | Ingest alert; 201 new, 200 replay/group |
| GET | `/api/incidents/{uuid}` | Viewer or admin | Incident, latest 30 logs and latest 100 timeline entries |
| POST | `/api/incidents/{uuid}/status` | Admin/responder | Guarded lifecycle transition |
| POST | `/api/incidents/{uuid}/assign` | Admin/responder | Assign responder/team |
| POST | `/api/incidents/{uuid}/notes` | Admin/responder | Append an audit note |
| POST | `/api/incidents/{uuid}/logs` | Admin/responder | Attach 1–30 logs; invalidate prior RCA |
| POST | `/api/incidents/{uuid}/rca` | Admin/responder | Queue analysis; demo completes synchronously |
| POST | `/api/internal/incidents/{uuid}/rca` | Worker only | Revision/request checked callback |
| GET | `/api/summary` | Viewer or admin | Counts, MTTA, MTTR and daily volume |
| GET | `/api/services` | Viewer or admin | Catalog and computed health |
| POST | `/api/services` | Admin | Register service; 201, empty body |
| POST | `/api/services/{slug}/telemetry` | Admin/responder | Server-timestamped heartbeat; 204 |
| GET | `/actuator/health/readiness` | Public | API readiness |
| GET | `/actuator/prometheus` | Admin | Prometheus scrape |

The three configured identities are `admin` (ADMIN/RESPONDER/VIEWER), `viewer` (VIEWER), and `rca-worker` (WORKER). The worker cannot use normal application routes. It only needs `WORKER_PASSWORD`.

List query: `search`, `status`, `severity`, `service`, zero-based `page` and `size` (1–100, default 20). Search checks title and assignee. Response: `{items,total,page,size}`. Lists omit detailed logs/timeline; use the detail endpoint.

## Alert payload

See [examples/payment-alert.json](../examples/payment-alert.json). Required: source (≤64), eventId (≤128), fingerprint (≤200), title (≤200), service (registered slug). Optional description (≤6000), severity (SEV1–SEV4), errorRate (0–1), latencyP95 (milliseconds), and logs (≤30). Each log requires an ISO-8601 timestamp, level (INFO/WARN/ERROR/FATAL), message (≤4000), and optional traceId (≤128).

The event ID identifies one immutable event. Use a fresh ID for each new alert, even if its fingerprint matches a prior alert. Replaying an ID with altered telemetry or timestamps conflicts. The source/fingerprint pair is hashed before grouping within a service.

## Other requests

```json
{"status":"ACKNOWLEDGED","note":"On-call has taken ownership."}
```

```json
{"assignee":"payments-oncall"}
```

```json
{"message":"Dependency health confirmed; comparing release changes."}
```

```json
{"slug":"payments","name":"Payments API","team":"payments-oncall"}
```

```json
{"up":true,"errorRate":0.001,"latencyP95":120,"cpuPercent":24}
```

Use `{}` as the RCA request body. The callback contract is defined in `Contracts.RcaCallback`; it is an internal interface, not an end-user mutation route.

Errors use Problem Details where handled: 400 malformed/invalid request, 401 authentication required, 403 insufficient role, 404 unknown incident/service, 409 illegal transition, reused event payload, stale analysis or concurrent mutation. Retry a concurrent conflict after refetching state. Do not retry a changed payload using the same event ID.
