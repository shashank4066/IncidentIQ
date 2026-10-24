package dev.incidentiq.api;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="service_catalog")
public class ServiceRecord {
 @Id @Column(length=64) public String slug;
 @Column(nullable=false,length=100) public String name;
 @Column(nullable=false,length=100) public String team;
 public Instant heartbeatAt;
 public boolean up=true;
 public double errorRate;
 @Column(name="latency_p95") public long latencyP95;
 public double cpuPercent;
 public ServiceRecord() {}
}
