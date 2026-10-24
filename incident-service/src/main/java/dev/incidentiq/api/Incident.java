package dev.incidentiq.api;
import dev.incidentiq.contracts.Contracts.*;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="incidents",indexes={@Index(name="idx_incident_service_status",columnList="service,status"),@Index(name="idx_incident_created",columnList="createdAt")})
public class Incident {
 @Id public UUID id;
 @Version public long version;
 @Column(nullable=false,length=200) public String title;
 @Column(nullable=false,length=6000) public String description;
 @Column(nullable=false,length=64) public String service;
 @Column(nullable=false,length=64) public String fingerprint;
 @Enumerated(EnumType.STRING) @Column(nullable=false) public Severity severity;
 @Enumerated(EnumType.STRING) @Column(nullable=false) public Status status;
 @Column(nullable=false,length=100) public String assignee;
 @Column(nullable=false) public Instant createdAt;
 @Column(nullable=false) public Instant updatedAt;
 public Instant acknowledgedAt;
 public Instant resolvedAt;
 @Column(nullable=false) public int occurrences;
 @Column(nullable=false) public long revision;
 @Column(nullable=false,length=20) public String rcaStatus="NOT_REQUESTED";
 public UUID rcaRequestId;
 public Instant rcaRequestedAt;
 @Column(length=30000) public String analysisJson;
 public Incident() {}
}
