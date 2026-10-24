# Contributing

Use Java 21 and Node 22. Keep contract schema changes backward-compatible or introduce a new versioned topic. Add behavior-level tests for lifecycle, idempotency and evidence guarantees. Run `mvn verify`, frontend tests/build and the Compose smoke test before submitting changes. Never commit populated environment files, Terraform state, API keys or real incident logs. Keep RCA recommendations advisory and evidence references valid.
