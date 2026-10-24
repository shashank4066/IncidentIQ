package dev.incidentiq.api;
import dev.incidentiq.api.Dtos.*;
import dev.incidentiq.contracts.Contracts.*;
import dev.incidentiq.contracts.HeuristicAnalyzer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest(properties={"app.seed=false"}) @Transactional
class CoreWorkflowTest {
 @Autowired IncidentService app;@Autowired Incidents incidents;
 private String slug;
 @BeforeEach void setup(){slug="svc-"+UUID.randomUUID().toString().substring(0,8);app.register(new ServiceInput(slug,"Test API","api-oncall"));}
 private AlertInput alert(String event,String fingerprint){return new AlertInput("test",event,fingerprint,"API errors","Test context",slug,null,0.07,3500L,List.of(new LogInput(Instant.parse("2026-01-01T00:00:00Z"),"ERROR","connection pool timeout password=secret123","trace-1")));}
 @Test void alertReplayIsIdempotentAndRelatedAlertsAreGrouped(){
  var input=alert(UUID.randomUUID().toString(),"same-failure");var first=app.ingest(input,"test");var duplicate=app.ingest(input,"test");
  assertThat(first.created()).isTrue();assertThat(first.incident().severity()).isEqualTo(Severity.SEV2);assertThat(first.incident().assignee()).isEqualTo("api-oncall");
  assertThat(duplicate.duplicate()).isTrue();assertThat(duplicate.incident().occurrences()).isEqualTo(1);
  var grouped=app.ingest(alert(UUID.randomUUID().toString(),"same-failure"),"test");assertThat(grouped.created()).isFalse();assertThat(grouped.incident().id()).isEqualTo(first.incident().id());assertThat(grouped.incident().occurrences()).isEqualTo(2);
 }
 @Test void reusedEventWithDifferentPayloadIsRejected(){
  var input=alert(UUID.randomUUID().toString(),"fingerprint");app.ingest(input,"test");
  var changed=new AlertInput(input.source(),input.eventId(),input.fingerprint(),"Different title",input.description(),slug,null,0.07,3500L,input.logs());
  assertThatThrownBy(()->app.ingest(changed,"test")).isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
 }
 @Test void lifecycleGuardsAndMetricsUseCorrectTimestamps(){
  var i=app.ingest(alert(UUID.randomUUID().toString(),"lifecycle"),"test").incident();
  assertThatThrownBy(()->app.transition(i.id(),new Transition(Status.RESOLVED,"skip"),"test")).hasMessageContaining("409");
  var entity=incidents.findById(i.id()).orElseThrow();entity.createdAt=Instant.now().minusSeconds(600);incidents.save(entity);
  app.transition(i.id(),new Transition(Status.ACKNOWLEDGED,"Taking ownership"),"test");app.transition(i.id(),new Transition(Status.INVESTIGATING,"Reviewing traces"),"test");
  var resolved=app.transition(i.id(),new Transition(Status.RESOLVED,"Verified recovery"),"test");
  assertThat(resolved.acknowledgedAt()).isNotNull();assertThat(resolved.resolvedAt()).isAfter(resolved.createdAt());
  assertThat(resolved.timeline()).anySatisfy(t->assertThat(t.message).contains("Verified recovery"));
  assertThat(app.summary().mttrMinutes()).isGreaterThanOrEqualTo(10);
  var next=app.ingest(alert(UUID.randomUUID().toString(),"lifecycle"),"test");assertThat(next.created()).isTrue();assertThat(next.incident().id()).isNotEqualTo(i.id());
 }
 @Test void rcaIsLabeledAndNewEvidenceInvalidatesIt(){
  var i=app.ingest(alert(UUID.randomUUID().toString(),"rca"),"test").incident();assertThat(i.logs().getFirst().message()).doesNotContain("secret123");
  var analyzed=app.requestRca(i.id(),"test");assertThat(analyzed.rcaStatus()).isEqualTo("COMPLETED");assertThat(analyzed.analysis().source()).isEqualTo("HEURISTIC");
  assertThat(analyzed.analysis().hypotheses()).allSatisfy(h->assertThat(h.evidenceIds()).containsExactly(i.logs().getFirst().id()));
  var updated=app.appendLogs(i.id(),List.of(new LogInput(Instant.now(),"ERROR","New evidence","trace-2")),"test");assertThat(updated.rcaStatus()).isEqualTo("NOT_REQUESTED");assertThat(updated.analysis()).isNull();assertThat(updated.revision()).isEqualTo(i.revision()+1);
 }
 @Test void callbackIsIdempotentAndRejectsStaleAnalysis(){
  var i=app.ingest(alert(UUID.randomUUID().toString(),"callback"),"test").incident();var entity=incidents.findById(i.id()).orElseThrow();entity.rcaStatus="QUEUED";entity.rcaRequestId=UUID.randomUUID();
  var c=new Context(i.id(),i.revision(),i.title(),i.description(),slug,i.severity(),i.logs());var callback=new RcaCallback(entity.rcaRequestId,i.revision(),new HeuristicAnalyzer().analyze(c));
  assertThat(app.completeRca(i.id(),callback).rcaStatus()).isEqualTo("COMPLETED");int entries=app.get(i.id()).timeline().size();app.completeRca(i.id(),callback);assertThat(app.get(i.id()).timeline()).hasSize(entries);
  app.appendLogs(i.id(),List.of(new LogInput(Instant.now(),"INFO","Changed evidence","")),"test");assertThatThrownBy(()->app.completeRca(i.id(),callback)).hasMessageContaining("409");
 }
 @Test void heartbeatAndIncidentSeverityDriveServiceHealth(){
  assertThat(app.serviceViews().stream().filter(s->s.slug().equals(slug)).findFirst().orElseThrow().health()).isEqualTo("UNKNOWN");
  app.telemetry(slug,new Telemetry(true,0.001,120,20));assertThat(app.serviceViews().stream().filter(s->s.slug().equals(slug)).findFirst().orElseThrow().health()).isEqualTo("HEALTHY");
  app.ingest(alert(UUID.randomUUID().toString(),"health"),"test");assertThat(app.serviceViews().stream().filter(s->s.slug().equals(slug)).findFirst().orElseThrow().health()).isEqualTo("DEGRADED");
 }
 @Test void severityThresholdBoundaries(){
  var a=alert("classify","classify");
  assertThat(IncidentService.classify(new AlertInput(a.source(),a.eventId(),a.fingerprint(),a.title(),"",slug,null,0.2,1L,List.of()))).isEqualTo(Severity.SEV1);
  assertThat(IncidentService.classify(new AlertInput(a.source(),a.eventId(),a.fingerprint(),a.title(),"",slug,Severity.SEV4,0.9,9999L,List.of()))).isEqualTo(Severity.SEV4);
 }
}
