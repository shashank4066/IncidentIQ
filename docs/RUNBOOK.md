# Operations runbook

## Incident API unavailable

Check container/pod readiness and logs, PostgreSQL connectivity, migration results and configured credentials. Cloud startup requires a valid database schema. Demo startup should select the `demo` profile. Do not use both `demo` and `cloud` simultaneously.

## Alert ingestion returns an error

400: validate the DTO and enum values. 401/403: use the right account. 404: register the service first. 409: inspect whether the event ID was reused with different content or the incident was concurrently updated. Replay an identical payload only for an actual duplicate event. A new event requires a fresh event ID.

## Analysis stays queued or fails

Check the worker, Kafka topic and consumer group `incidentiq-rca-v1`; inspect the outbox backlog metric and `incidentiq.events.v1.DLT`. Kafka topics have three partitions and the DLT has matching partition count. The worker must have a reachable API URL and the same worker password as the API. The source schema must be version 1.

After five minutes the API marks uncompleted requests FAILED. Correct the dependency, then request RCA again. Do not blindly replay a stale DLT message: verify the request ID/revision is still current. A 409 callback means evidence/request state has moved on and the worker discards that result deliberately.

A HEURISTIC result is expected with no AI key. If a configured provider fails, the result's unknowns explain that a fallback was used. Inspect `incidentiq_ai_requests_total{result="fallback"}`. Provider error bodies and keys are not logged. Verify the base URL is the trusted provider's `/v1`-style prefix and that the model supports chat-completions JSON text.

## Outbox backlog grows

Verify broker health, DNS and topic existence. Rows are retained after publishing to support debugging. The publisher retries indefinitely; do not delete pending rows to hide an outage. Delivery is at least once. Alert on sustained pending count and database growth; implement an approved archival/retention policy before high-volume use.

## Service health is unknown

Post a fresh authenticated telemetry heartbeat to `/api/services/{slug}/telemetry`. The API uses its own arrival time. Heartbeats become stale after ten minutes. A resolved incident alone cannot make telemetry healthy.

## Grafana shows no data

Enable the observability Compose profile. Verify Prometheus target health and the generated scrape password. Generate API traffic and allow the Prometheus rate window to accumulate samples. Traces require `TRACING_ENABLED=true` and a running collector/Tempo. In Grafana Explore select Tempo; use trace IDs from API logs or your instrumented producer logs. The incident log trace field is a correlation reference, not proof that this project captured the upstream application's trace.

Prometheus rules are included. Alertmanager routing and paging integrations are not configured. A producer or adapter must POST compatible alert JSON; there is no claim of an already configured Prometheus Alertmanager webhook converter.

## Cache or credential rotation

Redis backs the cloud summary cache; an unavailable Redis instance can fail summary requests while incident persistence remains in PostgreSQL. Restore Redis and retry. The cache has a ten-second TTL and mutation eviction. Rotate all three application passwords independently and recreate/restart affected API/worker containers. Never paste passwords into a public issue or commit them.

## Capacity and recovery

Keep database backups outside the node and test restores. The provided stateful examples use one replica each and are suitable for development, not availability guarantees. Before scaling incident volume, replace in-memory dashboard aggregation with database aggregates, define receipt/outbox/log retention, set request rate limits and measure query/callback performance under your own load.
