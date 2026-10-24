package dev.incidentiq.contracts;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
/** Versioned wire contracts shared by the API and Kafka worker. */
public final class Contracts {
 private Contracts() {}
 public enum Severity { SEV1, SEV2, SEV3, SEV4 }
 public enum Status { OPEN, ACKNOWLEDGED, INVESTIGATING, MITIGATED, RESOLVED }
 public record Evidence(String id, Instant timestamp, String level, String message, String traceId) {}
 public record Context(UUID incidentId, long revision, String title, String description, String service, Severity severity, List<Evidence> logs) {}
 public record Event(int schemaVersion, UUID eventId, String type, UUID incidentId, UUID requestId, Instant occurredAt, Context context) {}
 public record Hypothesis(String cause, List<String> evidenceIds) {}
 public record Analysis(String source, String summary, List<Hypothesis> hypotheses, List<String> remediation, List<String> unknowns, Instant generatedAt) {}
 public record RcaCallback(UUID requestId, long revision, Analysis analysis) {}
}
