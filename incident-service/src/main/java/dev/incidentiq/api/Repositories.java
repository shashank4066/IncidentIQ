package dev.incidentiq.api;
import dev.incidentiq.contracts.Contracts.Status;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
interface Incidents extends JpaRepository<Incident,UUID>,JpaSpecificationExecutor<Incident> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select i from Incident i where i.id=:id") Optional<Incident> lock(@Param("id") UUID id);
 Optional<Incident> findFirstByServiceAndFingerprintAndStatusNotOrderByCreatedAtDesc(String service,String fingerprint,Status status);
 long countByStatusNot(Status status);
 @Query("select i from Incident i where i.rcaStatus='QUEUED' and i.rcaRequestedAt < :before") List<Incident> expired(@Param("before") java.time.Instant before);
}
interface Services extends JpaRepository<ServiceRecord,String> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select s from ServiceRecord s where s.slug=:slug") Optional<ServiceRecord> lock(@Param("slug") String slug);
}
interface Logs extends JpaRepository<LogEntry,UUID> { List<LogEntry> findTop30ByIncidentIdOrderByTimestampDesc(UUID incidentId); long countByIncidentId(UUID incidentId); }
interface Timelines extends JpaRepository<TimelineEntry,UUID> { List<TimelineEntry> findTop100ByIncidentIdOrderByTimestampDesc(UUID incidentId); }
interface Receipts extends JpaRepository<AlertReceipt,String> {}
interface Outbox extends JpaRepository<OutboxEvent,UUID> {
 @Query(value="select * from outbox_events where published_at is null order by created_at limit 20 for update skip locked",nativeQuery=true) List<OutboxEvent> claim();
 long countByPublishedAtIsNull();
}
