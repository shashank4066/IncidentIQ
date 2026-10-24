package dev.incidentiq.api;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="alert_receipts")
public class AlertReceipt {
 @Id @Column(length=64) public String id;
 @Column(nullable=false,length=64) public String payloadHash;
 @Column(nullable=false) public UUID incidentId;
 @Column(nullable=false) public Instant receivedAt;
 public AlertReceipt() {}
}
