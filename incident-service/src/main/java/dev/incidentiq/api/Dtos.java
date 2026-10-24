package dev.incidentiq.api;
import dev.incidentiq.contracts.Contracts.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
public final class Dtos {
 private Dtos() {}
 public record LogInput(@NotNull Instant timestamp,@NotBlank @Pattern(regexp="INFO|WARN|ERROR|FATAL") String level,@NotBlank @Size(max=4000) String message,@Size(max=128) String traceId) {}
 public record AlertInput(@NotBlank @Size(max=64) String source,@NotBlank @Size(max=128) String eventId,@NotBlank @Size(max=200) String fingerprint,@NotBlank @Size(max=200) String title,@Size(max=6000) String description,@NotBlank @Pattern(regexp="[a-z0-9-]{1,64}") String service,Severity severity,@DecimalMin("0.0") @DecimalMax("1.0") Double errorRate,@Min(0) Long latencyP95,@Size(max=30) List<@Valid LogInput> logs) {}
 public record Transition(@NotNull Status status,@NotBlank @Size(max=1800) String note) {}
 public record Assignment(@NotBlank @Size(max=100) String assignee) {}
 public record Note(@NotBlank @Size(max=1800) String message) {}
 public record ServiceInput(@NotBlank @Pattern(regexp="[a-z0-9-]{1,64}") String slug,@NotBlank @Size(max=100) String name,@NotBlank @Size(max=100) String team) {}
 public record Telemetry(boolean up,@DecimalMin("0") @DecimalMax("1") double errorRate,@Min(0) long latencyP95,@DecimalMin("0") @DecimalMax("100") double cpuPercent) {}
 public record Summary(long total,long active,long critical,long resolved,Double mttrMinutes,Double mttaMinutes,Map<String,Long> bySeverity,List<DailyCount> trend) {}
 public record DailyCount(String day,long count) {}
 public record ServiceView(String slug,String name,String team,String health,Instant heartbeatAt,double errorRate,long latencyP95,double cpuPercent,long activeIncidents) {}
 public record IncidentView(UUID id,String title,String description,String service,Severity severity,Status status,String assignee,Instant createdAt,Instant updatedAt,Instant acknowledgedAt,Instant resolvedAt,int occurrences,long revision,String rcaStatus,Analysis analysis,List<Evidence> logs,List<TimelineEntry> timeline) {}
 public record AlertResult(boolean created,boolean duplicate,IncidentView incident) {}
 public record PageResult(List<IncidentView> items,long total,int page,int size) {}
}
