# IncidentIQ

[![Verify IncidentIQ](https://github.com/shashank4066/IncidentIQ/actions/workflows/ci.yml/badge.svg)](https://github.com/shashank4066/IncidentIQ/actions/workflows/ci.yml)

**A cloud incident command center with evidence-based, AI-assisted root cause investigation.**

Spring Boot 3.5 · Java 21 · React 19 · Kafka · PostgreSQL · Redis · OpenTelemetry · Prometheus · Grafana · Docker · Kubernetes · AKS · Terraform · GitHub Actions

IncidentIQ turns application alerts into assigned incidents, records the response, and analyzes a bounded evidence snapshot. It is a production-style reference project for learning, demonstrations and portfolio use. The repository includes deployment infrastructure; it does not claim that an Azure environment has already been provisioned or operated in production.

## What works

- Authenticated alert ingestion, payload validation and deterministic severity classification.
- Persistent event-ID idempotency; a reused event ID with different content returns HTTP 409.
- Grouping by source, service and fingerprint while an incident remains active; a new alert after resolution opens a new incident.
- Default assignment to the registered on-call team, reassignment, notes and an audit timeline.
- Guarded lifecycle: open → acknowledged → investigating → mitigated → resolved. Investigation may resolve directly; mitigation can return to investigation.
- MTTA and MTTR from actual timestamps, a seven-day volume chart, severity breakdown and service-health signals.
- Log evidence, trace references, common credential/email redaction, and RCA invalidation when evidence changes.
- A separate Kafka RCA worker with bounded retries, dead-letter publication, a transactional outbox, idempotent callbacks and rejection of stale results.
- A configurable OpenAI-compatible AI client. With no key or an invalid provider response, results are explicitly labeled **HEURISTIC**. AI claims are hypotheses with log references and human-verification steps.
- Two local run modes, monitoring dashboards, trace collection, Kubernetes manifests and an Azure deployment workflow.

## Start with the lightweight demo

The demo uses an in-memory H2 database, synchronous heuristic RCA and four clearly synthetic example incidents. It needs no Kafka, Redis, PostgreSQL, cloud account or AI key. Data resets on restart. Choose this first on a 16 GB laptop.

### Option A: Docker Desktop

Install Docker Desktop with Linux containers, then run from the repository root:

```bash
docker compose -f compose.demo.yml up --build -d
```

Open **http://localhost:3000** and select **Explore demo workspace**.

| Account | Username | Demo password | Access |
| --- | --- | --- | --- |
| Responder | `admin` | `demo-admin` | Read and mutate incidents |
| Observer | `viewer` | `demo-viewer` | Read only |

These credentials apply only to the demo profile. Host ports bind to loopback.

Stop it with `docker compose -f compose.demo.yml down`.

### Option B: Java + Node, no Docker

Prerequisites: JDK **21 or 25**, Maven **3.9+**, Node.js **22.12+** and npm. Windows PowerShell, IntelliJ and VS Code all work; Ubuntu is not required.

Terminal 1, repository root:

```bash
mvn -B -ntp -pl incident-service -am package
java -jar incident-service/target/incident-service-1.0.0.jar --spring.profiles.active=demo
```

Terminal 2:

```bash
cd frontend
npm install
npm run dev
```

Open **http://localhost:5173**. Vite proxies `/api` to port 8080. Do not open `index.html` directly.

Dependencies require Internet access on the first build. The Maven reactor must build `contracts` with the service; running a module in isolation before installing contracts will fail.

## Run the event-driven stack

```bash
python scripts/init-env.py
docker compose up --build -d --wait
```

Open http://localhost:3000. Sign in as `admin` with `ADMIN_PASSWORD` from your local `.env`. The generator preserves an existing `.env`, generates independent random passwords, and never prints them. Do not commit that file.

This mode runs PostgreSQL, Redis, a single-node Kafka KRaft broker, the incident API, the RCA worker and the frontend. Named volumes retain PostgreSQL and Kafka data. The default generated configuration seeds example data; set `SEED_DEMO=false` for an empty workspace. Register services through **Service health → Register service**, then ingest telemetry and alerts.

Allow roughly 4–6 GB for this stack. Monitoring is optional and consumes additional memory:

```bash
# Enable traces in .env by adding TRACING_ENABLED=true first.
docker compose --profile observability up --build -d --wait
```

| Endpoint | Purpose |
| --- | --- |
| http://localhost:3000 | React operations dashboard |
| http://localhost:8080/actuator/health/readiness | API readiness |
| http://localhost:9090 | Prometheus, when observability is enabled |
| http://localhost:3001 | Grafana, user `admin`, local `GRAFANA_PASSWORD` |

Grafana automatically provisions the **IncidentIQ · Operations** dashboard, Prometheus and Tempo. The API exports Micrometer metrics in Prometheus format and OpenTelemetry traces over OTLP/HTTP to the collector, which batches and sends them to Tempo. Metrics reach Prometheus by scraping; traces reach Tempo through the collector. Application logs are attached through the incident ingestion API; this repository does not claim to operate a general-purpose centralized log storage system.

Run `docker compose down` to stop. `down -v` also removes development database and broker volumes; use it only when you intend to reset that data.

## Configure AI RCA

Edit the generated `.env`:

```dotenv
AI_BASE_URL=https://your-trusted-provider.example/v1
AI_MODEL=your-model-id
AI_API_KEY=your-key
```

Recreate the worker with `docker compose up -d --force-recreate rca-worker`.

Only the cloud worker calls the model. The demo deliberately remains offline. The URL must be a trusted OpenAI-compatible chat-completions endpoint; the client appends `/chat/completions`. An API subscription and provider usage charges, if any, are separate from running this code.

The worker sends the title, description, severity, service identifier and latest 30 redacted log entries. Treat redaction as defense in depth, not a complete data-loss prevention system. Do not ingest customer secrets or personal records. AI output is bounded and validated; evidence IDs must exist in the supplied context. Remediation is advisory and is never executed automatically.

## Try the complete workflow

1. Sign in, open the seeded payment incident and review its timeline and evidence.
2. Declare a new incident or post the sample alert below. Verify its severity and team assignment.
3. Acknowledge and investigate it, adding a response note for each change.
4. Open **AI investigation → Generate analysis**. Cloud mode returns a queued state until the worker callback completes.
5. Attach new logs and observe that the old analysis is invalidated. Request an updated analysis.
6. Resolve the incident and verify that the dashboard updates MTTR.
7. Replay exactly the same alert: the occurrence count does not change. Send a new event ID with the same fingerprint: it groups with an active incident.

## API examples

See [docs/API.md](docs/API.md) for the full route table, request formats and error semantics. The sample below assumes the demo API is running; for cloud use your configured password instead. In PowerShell use `curl.exe` if `curl` is aliased.

```bash
curl -u admin:demo-admin -H "Content-Type: application/json" \
  --data @examples/payment-alert.json http://localhost:8080/api/alerts
```

```bash
python scripts/smoke.py
```

For cloud mode, supply the generated admin password as the `ADMIN_PASSWORD` environment variable before running the smoke script. It creates a dedicated service and resolves its disposable incident; it intentionally leaves the audit trail.

## Tests and CI

```bash
mvn -B -ntp verify
cd frontend
npm install
npm test
npm run build
# With a running Compose stack and matching ADMIN_PASSWORD / VIEWER_PASSWORD:
npx playwright install chromium
npm run test:e2e
```

The verification workflow runs Java tests, frontend tests and build, Terraform validation, Kustomize rendering, a real Docker Compose integration smoke test, and Playwright operator/viewer scenarios. Docker integration exercises PostgreSQL migrations, Kafka, Redis and the worker callback, rather than substituting them with H2.

The [full verification run passed](https://github.com/shashank4066/IncidentIQ/actions/runs/40082875245), including the Java reactor, frontend build, Terraform/Kustomize checks, real Compose integration and both Playwright scenarios. See [docs/VALIDATION.md](docs/VALIDATION.md) for the exact results and remaining deployment boundaries. When npm first resolves the dependency tree, retain and commit its generated `frontend/package-lock.json`; Docker and subsequent builds use `npm ci` when it exists.

## Architecture and operations

- [Architecture and event guarantees](docs/ARCHITECTURE.md)
- [API contract](docs/API.md)
- [Azure / AKS deployment](docs/DEPLOYMENT.md)
- [Operations runbook](docs/RUNBOOK.md)
- [Validation record](docs/VALIDATION.md)

## Project layout

| Directory | Responsibility |
| --- | --- |
| `contracts` | Versioned event records, redaction and offline analyzer |
| `incident-service` | REST API, security, lifecycle, persistence, outbox and dashboard data |
| `rca-worker` | Kafka consumer, AI adapter, evidence validation and result callback |
| `frontend` | React dashboard, responsive UI and browser tests |
| `observability` | Prometheus rules, Grafana provisioning, collector and Tempo |
| `infra/k8s` | Stateful development dependencies, application deployments, ingress and policies |
| `infra/terraform` | AKS, ACR, Entra integration and image-pull role assignment |
| `.github/workflows` | Verification and manual Azure deployment |
| `scripts` | Credential initialization and API smoke test |

## Boundaries before a real production rollout

This is a working reference implementation with deliberate limits: one workspace; environment-configured Basic-auth accounts; no tenant isolation, SSO or escalation paging; single-node sample stateful dependencies; no automatic remediation; and no claimed load, HA or disaster-recovery certification. Summary/service-health queries currently read the incident set and should become database aggregates/materialized views before large-volume use. Alert receipts and outbox rows need a retention policy appropriate to replay guarantees.

Use HTTPS, Entra/OIDC sign-in, managed stateful services with backups, TLS/SASL for remote brokers, a secret manager, restricted ingress, retention jobs, rate limits and your own performance/security checks before handling operational customer data. Azure resources and paid AI calls are created only when you configure and run those workflows yourself.

## Publish the source to GitHub

With Git and the official GitHub CLI installed, run from the extracted project in PowerShell:

```powershell
.\scripts\Publish-GitHub.ps1 -Public
```

The script authenticates when required, commits the source and creates a new repository named IncidentIQ. Omit `-Public` for a private repository. It preserves existing remotes/repositories and never force-pushes. If a repository already exists, use its normal Git push workflow or the connected GitHub file tools instead. Publishing source does not host the Spring Boot application; GitHub Pages alone cannot run this backend.
