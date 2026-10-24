# Delivery validation record

Validated on 24 October 2026. This record distinguishes executed checks from configuration supplied for later execution.

| Check | Result |
| --- | --- |
| Core workflow integration tests | **7 passed**, real Spring/JPA services against H2 using the initial SQL schema |
| Analyzer/redaction tests | **4 passed**, including credential redaction and storage bounds |
| AI adapter tests | **3 passed**, using an injected transport fixture; JSON validation and unknown-evidence fallback verified |
| Frontend utility tests | **5 passed**, using Node's test runner |
| React JSX source parsing | Passed with the available Babel parser |
| JSON and Maven XML parsing | Passed |
| YAML parsing and duplicate-key checks | Passed with a strict loader |
| Full Maven reactor with security/Kafka/Redis/OTLP dependencies | Not run successfully: required artifacts are not cached and external package-registry DNS is unavailable |
| Vite production build / Playwright | Not run here: package-registry access is unavailable; the partial local npm cache could not satisfy installation |
| Docker Compose integration | Not run here: Docker is unavailable |
| Terraform provider validation and Kubernetes server-side validation | Not run here: CLIs/providers and a cluster are unavailable |
| Live AI provider, AKS provisioning, public app hosting | Not performed |

**19 behavior-level tests passed.** Java verification used an isolated local build containing the unchanged domain/service/AI-adapter source and tests that could compile with available dependencies. It excluded SecurityConfig, Redis configuration, Kafka classes and telemetry exporters. It used Spring SQL initialization for the migration text rather than exercising Flyway itself. This result is not a claim that the entire production dependency graph compiled or that security/infrastructure integration checks passed.

The source also includes five HTTP/security tests and two Playwright scenarios. GitHub Actions is configured to run the full Maven reactor, frontend build, Terraform validation, Kustomize rendering and the real Compose/Playwright pipeline once the repository is published with working runner/package access. Those pending checks are not represented as passing.

No API key was reused from another conversation, no real incident data was uploaded to an AI provider, and no Azure spending was initiated.
