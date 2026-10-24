package dev.incidentiq.api;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.incidentiq.contracts.*;
import dev.incidentiq.contracts.Contracts.*;
import dev.incidentiq.api.Dtos.*;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

@Service @Transactional
public class IncidentService {
 private final Incidents incidents; private final Services services; private final Logs logs; private final Timelines timeline;
 private final Receipts receipts; private final Outbox outbox; private final ObjectMapper json; private final MeterRegistry metrics; private final boolean cloud;
 public IncidentService(Incidents i,Services s,Logs l,Timelines t,Receipts r,Outbox o,ObjectMapper j,MeterRegistry m,org.springframework.core.env.Environment env) {
  incidents=i;services=s;logs=l;timeline=t;receipts=r;outbox=o;json=j;metrics=m;cloud=env.matchesProfiles("cloud");
 }
 @CacheEvict(value="summary",allEntries=true)
 public AlertResult ingest(AlertInput a,String actor) {
  // Lock the service row to serialize grouping and idempotency checks for that service.
  var svc=services.lock(a.service()).orElseThrow(()->problem(404,"Service is not registered"));
  String key=hash(a.source()+":"+a.eventId()), payloadHash=hash(write(a));
  var receipt=receipts.findById(key);
  if(receipt.isPresent()) {
   if(!receipt.get().payloadHash.equals(payloadHash)) throw problem(409,"Event ID was already used with different content");
   return new AlertResult(false,true,view(require(receipt.get().incidentId),true));
  }
  String fingerprint=hash(a.source()+":"+a.fingerprint());
  var existing=incidents.findFirstByServiceAndFingerprintAndStatusNotOrderByCreatedAtDesc(a.service(),fingerprint,Status.RESOLVED);
  boolean created=existing.isEmpty(); var i=existing.orElseGet(Incident::new); Instant now=Instant.now();
  var severity=classify(a);
  if(created) {
   i.id=UUID.randomUUID();i.title=Redactor.clean(a.title(),200);i.description=Redactor.clean(a.description(),6000);i.service=a.service();i.fingerprint=fingerprint;
   i.severity=severity;i.status=Status.OPEN;i.assignee=svc.team;i.createdAt=now;i.occurrences=0;
  } else if(severity.ordinal()<i.severity.ordinal()) { i.severity=severity; }
  i.updatedAt=now;i.occurrences++;i.revision++;
  invalidateRca(i);
  incidents.saveAndFlush(i); addLogs(i,a.logs());
  record(i,created?"CREATED":"ALERT_GROUPED",actor,created?"Alert classified as "+i.severity+" and assigned to "+i.assignee:"Related alert received; occurrence "+i.occurrences);
  var saved=new AlertReceipt();saved.id=key;saved.payloadHash=payloadHash;saved.incidentId=i.id;saved.receivedAt=now;receipts.save(saved);
  event(i,created?"INCIDENT_CREATED":"ALERT_GROUPED",null);
  metrics.counter("incidentiq_alerts","result",created?"created":"grouped").increment();
  return new AlertResult(created,false,view(i,true));
 }
 static Severity classify(AlertInput a) {
  if(a.severity()!=null) return a.severity();
  if(a.errorRate()!=null && a.errorRate()>=0.20) return Severity.SEV1;
  if((a.errorRate()!=null && a.errorRate()>=0.05)||(a.latencyP95()!=null && a.latencyP95()>=3000)) return Severity.SEV2;
  if((a.errorRate()!=null && a.errorRate()>=0.01)||(a.latencyP95()!=null && a.latencyP95()>=1000)) return Severity.SEV3;
  return Severity.SEV4;
 }
 @Transactional(readOnly=true) public PageResult list(String search,Status status,Severity severity,String service,int page,int size) {
  Specification<Incident> spec=(r,q,b)->b.conjunction();
  if(status!=null) spec=spec.and((r,q,b)->b.equal(r.get("status"),status));
  if(severity!=null) spec=spec.and((r,q,b)->b.equal(r.get("severity"),severity));
  if(service!=null&&!service.isBlank()) spec=spec.and((r,q,b)->b.equal(r.get("service"),service));
  if(search!=null&&!search.isBlank()) { String s="%"+search.toLowerCase(Locale.ROOT).replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";spec=spec.and((r,q,b)->b.or(b.like(b.lower(r.get("title")),s,'\\'),b.like(b.lower(r.get("assignee")),s,'\\'))); }
  var result=incidents.findAll(spec,PageRequest.of(Math.max(0,page),Math.min(100,Math.max(1,size)),Sort.by(Sort.Direction.DESC,"createdAt")));
  return new PageResult(result.getContent().stream().map(i->view(i,false)).toList(),result.getTotalElements(),result.getNumber(),result.getSize());
 }
 @Transactional(readOnly=true) public IncidentView get(UUID id) { return view(require(id),true); }
 @CacheEvict(value="summary",allEntries=true) public IncidentView transition(UUID id,Transition input,String actor) {
  var i=lock(id);var allowed=switch(i.status) {
   case OPEN->Set.of(Status.ACKNOWLEDGED);
   case ACKNOWLEDGED->Set.of(Status.INVESTIGATING);
   case INVESTIGATING->Set.of(Status.MITIGATED,Status.RESOLVED);
   case MITIGATED->Set.of(Status.INVESTIGATING,Status.RESOLVED);
   case RESOLVED->Set.<Status>of();
  };
  if(!allowed.contains(input.status())) throw problem(409,"Transition from "+i.status+" to "+input.status()+" is not allowed");
  var now=Instant.now(); i.status=input.status();i.updatedAt=now;
  if(i.status==Status.ACKNOWLEDGED) i.acknowledgedAt=now;
  if(i.status==Status.RESOLVED) {i.resolvedAt=now;metrics.timer("incidentiq_resolution_duration").record(Duration.between(i.createdAt,now));}
  record(i,"STATUS_CHANGED",actor,input.status()+": "+Redactor.clean(input.note(),1800));event(i,"STATUS_CHANGED",null);
  return view(i,true);
 }
 public IncidentView assign(UUID id,Assignment a,String actor) {var i=lock(id);i.assignee=Redactor.clean(a.assignee(),100);i.updatedAt=Instant.now();record(i,"ASSIGNED",actor,"Assigned to "+i.assignee);event(i,"INCIDENT_ASSIGNED",null);return view(i,true);}
 public IncidentView note(UUID id,Note n,String actor) {var i=lock(id);record(i,"NOTE",actor,Redactor.clean(n.message(),1800));i.updatedAt=Instant.now();return view(i,true);}
 public IncidentView appendLogs(UUID id,List<LogInput> input,String actor) {
  var i=lock(id);addLogs(i,input);i.revision++;i.updatedAt=Instant.now();invalidateRca(i);record(i,"LOGS_ADDED",actor,input.size()+" log entries attached; previous RCA invalidated");return view(i,true);
 }
 public IncidentView requestRca(UUID id,String actor) {
  var i=lock(id);
  if("QUEUED".equals(i.rcaStatus)) return view(i,true);
  i.rcaRequestId=UUID.randomUUID();i.rcaRequestedAt=Instant.now();i.rcaStatus="QUEUED";
  record(i,"RCA_REQUESTED",actor,"Analysis requested for evidence revision "+i.revision);
  if(cloud) event(i,"RCA_REQUESTED",i.rcaRequestId);
  else {i.analysisJson=write(new HeuristicAnalyzer().analyze(context(i)));i.rcaStatus="COMPLETED";record(i,"RCA_COMPLETED","heuristic-engine","Offline analysis completed");}
  return view(i,true);
 }
 public IncidentView completeRca(UUID id,RcaCallback result) {
  var i=lock(id);
  // At-least-once delivery is safe; stale results cannot replace newer evidence.
  if(!Objects.equals(i.rcaRequestId,result.requestId())||i.revision!=result.revision()) throw problem(409,"Analysis belongs to an obsolete evidence revision");
  if("COMPLETED".equals(i.rcaStatus)) return view(i,true);
  if(!"QUEUED".equals(i.rcaStatus)) throw problem(409,"Analysis is not pending");
  validateAnalysis(i,result.analysis());i.analysisJson=write(result.analysis());i.rcaStatus="COMPLETED";
  record(i,"RCA_COMPLETED","rca-worker","Analysis completed using "+result.analysis().source());metrics.counter("incidentiq_rca_completed").increment();return view(i,true);
 }
 void validateAnalysis(Incident i,Analysis a) {
  if(a==null||a.summary()==null||a.summary().length()>4000||a.hypotheses()==null||a.hypotheses().size()>8||a.remediation()==null||a.remediation().size()>12||a.unknowns()==null||a.unknowns().size()>8||a.generatedAt()==null||a.source()==null||!Set.of("AI","HEURISTIC").contains(a.source())) throw problem(400,"Invalid analysis structure");
  var ids=new HashSet<>(context(i).logs().stream().map(Evidence::id).toList());
  for(var h:a.hypotheses()) if(h==null||h.cause()==null||h.cause().length()>2000||h.evidenceIds()==null||!ids.containsAll(h.evidenceIds())) throw problem(400,"Analysis references unknown evidence");
  if(write(a).length()>29000) throw problem(400,"Analysis is too large");
 }
 @Cacheable("summary") @Transactional(readOnly=true) public Summary summary() {
  var all=incidents.findAll();long active=all.stream().filter(i->i.status!=Status.RESOLVED).count();
  var bySeverity=new LinkedHashMap<String,Long>();for(var s:Severity.values())bySeverity.put(s.name(),all.stream().filter(i->i.severity==s&&i.status!=Status.RESOLVED).count());
  var mttr=all.stream().filter(i->i.resolvedAt!=null).mapToLong(i->Duration.between(i.createdAt,i.resolvedAt).toSeconds()).average();
  var mtta=all.stream().filter(i->i.acknowledgedAt!=null).mapToLong(i->Duration.between(i.createdAt,i.acknowledgedAt).toSeconds()).average();
  var trend=new ArrayList<DailyCount>();for(int n=6;n>=0;n--){var day=LocalDate.now(ZoneOffset.UTC).minusDays(n);trend.add(new DailyCount(day.toString(),all.stream().filter(i->i.createdAt.atZone(ZoneOffset.UTC).toLocalDate().equals(day)).count()));}
  return new Summary(all.size(),active,bySeverity.get("SEV1"),all.size()-active,mttr.isPresent()?mttr.getAsDouble()/60:null,mtta.isPresent()?mtta.getAsDouble()/60:null,bySeverity,trend);
 }
 @Transactional(readOnly=true) public List<ServiceView> serviceViews() {
  var all=incidents.findAll();return services.findAll(Sort.by("name")).stream().map(s->{
   var active=all.stream().filter(i->i.service.equals(s.slug)&&i.status!=Status.RESOLVED).toList();
   String health=s.heartbeatAt==null||s.heartbeatAt.isBefore(Instant.now().minusSeconds(600))?"UNKNOWN":!s.up||active.stream().anyMatch(i->i.severity==Severity.SEV1)?"DOWN":s.errorRate>=0.02||active.stream().anyMatch(i->i.severity==Severity.SEV2)?"DEGRADED":"HEALTHY";
   return new ServiceView(s.slug,s.name,s.team,health,s.heartbeatAt,s.errorRate,s.latencyP95,s.cpuPercent,active.size());
  }).toList();
 }
 public void register(ServiceInput input) {if(services.existsById(input.slug()))throw problem(409,"Service already exists");var s=new ServiceRecord();s.slug=input.slug();s.name=input.name();s.team=input.team();services.save(s);}
 public void telemetry(String slug,Telemetry t) {var s=services.lock(slug).orElseThrow(()->problem(404,"Service not found"));s.up=t.up();s.errorRate=t.errorRate();s.latencyP95=t.latencyP95();s.cpuPercent=t.cpuPercent();s.heartbeatAt=Instant.now();}
 public void expireRca() { for(var candidate:incidents.expired(Instant.now().minusSeconds(300))) {var i=lock(candidate.id);if("QUEUED".equals(i.rcaStatus)&&i.rcaRequestedAt.isBefore(Instant.now().minusSeconds(300))){i.rcaStatus="FAILED";record(i,"RCA_FAILED","system","Worker did not complete within five minutes; retry analysis.");}} }
 private void invalidateRca(Incident i) {i.rcaStatus="NOT_REQUESTED";i.rcaRequestId=null;i.analysisJson=null;}
 private Incident require(UUID id) {return incidents.findById(id).orElseThrow(()->problem(404,"Incident not found"));}
 private Incident lock(UUID id) {return incidents.lock(id).orElseThrow(()->problem(404,"Incident not found"));}
 private void addLogs(Incident i,List<LogInput> input) {if(input==null)return;for(var l:input){var e=new LogEntry();e.id=UUID.randomUUID();e.incidentId=i.id;e.timestamp=l.timestamp();e.level=l.level();e.message=Redactor.clean(l.message(),4000);e.traceId=Redactor.clean(l.traceId(),128);logs.save(e);}}
 private void record(Incident i,String type,String actor,String message) {var t=new TimelineEntry();t.id=UUID.randomUUID();t.incidentId=i.id;t.timestamp=Instant.now();t.type=type;t.actor=actor;t.message=message;timeline.save(t);}
 private Context context(Incident i) {return new Context(i.id,i.revision,i.title,i.description,i.service,i.severity,evidence(i));}
 private List<Evidence> evidence(Incident i) {return logs.findTop30ByIncidentIdOrderByTimestampDesc(i.id).stream().map(l->new Evidence(l.id.toString(),l.timestamp,l.level,l.message,l.traceId)).toList();}
 private void event(Incident i,String type,UUID requestId) {if(!cloud)return;var e=new OutboxEvent();e.id=UUID.randomUUID();e.incidentId=i.id;e.createdAt=Instant.now();e.payload=write(new Event(1,e.id,type,i.id,requestId,e.createdAt,context(i)));outbox.save(e);}
 private IncidentView view(Incident i,boolean details) {
  Analysis analysis=null;try{if(i.analysisJson!=null)analysis=json.readValue(i.analysisJson,Analysis.class);}catch(JsonProcessingException e){throw new IllegalStateException("Stored analysis is invalid",e);}
  return new IncidentView(i.id,i.title,i.description,i.service,i.severity,i.status,i.assignee,i.createdAt,i.updatedAt,i.acknowledgedAt,i.resolvedAt,i.occurrences,i.revision,i.rcaStatus,analysis,details?evidence(i):List.of(),details?timeline.findTop100ByIncidentIdOrderByTimestampDesc(i.id):List.of());
 }
 private String write(Object o) {try{return json.writeValueAsString(o);}catch(JsonProcessingException e){throw new IllegalStateException("Cannot serialize event",e);}}
 private static String hash(String s) {try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 static ResponseStatusException problem(int code,String detail){return new ResponseStatusException(HttpStatus.valueOf(code),detail);}
}
