package dev.incidentiq.api;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="incident_timeline",indexes=@Index(name="idx_timeline_incident",columnList="incidentId"))
public class TimelineEntry {
 @Id public UUID id;
 @Column(nullable=false) public UUID incidentId;
 @Column(nullable=false) public Instant timestamp;
 @Column(nullable=false,length=50) public String type;
 @Column(nullable=false,length=100) public String actor;
 @Column(nullable=false,length=2000) public String message;
 public TimelineEntry() {}
}
