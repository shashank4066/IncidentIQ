package dev.incidentiq.api;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="outbox_events",indexes=@Index(name="idx_outbox_pending",columnList="publishedAt,createdAt"))
public class OutboxEvent {
 @Id public UUID id;
 @Column(nullable=false) public UUID incidentId;
 @Column(nullable=false,columnDefinition="text") public String payload;
 @Column(nullable=false) public Instant createdAt;
 public Instant publishedAt;
 public int attempts;
 public OutboxEvent() {}
}
