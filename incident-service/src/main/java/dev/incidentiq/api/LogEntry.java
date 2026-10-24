package dev.incidentiq.api;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="incident_logs",indexes=@Index(name="idx_logs_incident",columnList="incidentId"))
public class LogEntry {
 @Id public UUID id;
 @Column(nullable=false) public UUID incidentId;
 @Column(nullable=false) public Instant timestamp;
 @Column(nullable=false,length=10) public String level;
 @Column(nullable=false,length=4000) public String message;
 @Column(length=128) public String traceId;
 public LogEntry() {}
}
