package dev.incidentiq.api;
import dev.incidentiq.api.Dtos.*;
import dev.incidentiq.contracts.Contracts.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
@Component @ConditionalOnProperty(name="app.seed",havingValue="true")
public class DemoSeed implements CommandLineRunner {
 private final IncidentService app;private final Services services;private final Incidents incidents;
 public DemoSeed(IncidentService a,Services s,Incidents i){app=a;services=s;incidents=i;}
 @Override public void run(String... args){
  String[][] catalog={{"payments","Payments API","payments-oncall"},{"checkout","Checkout Gateway","commerce-oncall"},{"identity","Identity Service","platform-oncall"},{"notifications","Notifications","platform-oncall"}};
  for(var s:catalog)if(!services.existsById(s[0]))app.register(new ServiceInput(s[0],s[1],s[2]));
  if(incidents.count()>0)return;
  app.telemetry("payments",new Telemetry(true,0.23,4820,78));app.telemetry("checkout",new Telemetry(true,0.08,3200,63));app.telemetry("identity",new Telemetry(true,0.001,120,24));app.telemetry("notifications",new Telemetry(true,0.003,240,36));
  var p=app.ingest(new AlertInput("demo","payments-1","pool-exhaustion","Payment failures above threshold","Payment authorization errors increased after the latest release.","payments",Severity.SEV1,0.23,4820L,List.of(new LogInput(Instant.now().minusSeconds(420),"ERROR","SQLTransientConnectionException: connection pool exhausted; waited 30000ms","8eaf24b7c901"),new LogInput(Instant.now().minusSeconds(390),"ERROR","POST /authorize timed out while acquiring database connection","8eaf24b7c902"))),"demo-seed").incident();
  app.transition(p.id(),new Transition(Status.ACKNOWLEDGED,"On-call acknowledged the alert."),"demo-seed");app.transition(p.id(),new Transition(Status.INVESTIGATING,"Comparing database waits with the last release."),"demo-seed");app.requestRca(p.id(),"demo-seed");
  app.ingest(new AlertInput("demo","checkout-1","upstream-latency","Checkout latency is elevated","p95 latency exceeded 3000 ms for five minutes.","checkout",Severity.SEV2,0.08,3200L,List.of(new LogInput(Instant.now(),"ERROR","Upstream payments returned 503; circuit breaker open","d720bb3c110a"))),"demo-seed");
  app.ingest(new AlertInput("demo","notify-1","delivery-lag","Notification delivery lag","Email queue is processing slower than expected.","notifications",Severity.SEV3,0.015,1800L,List.of(new LogInput(Instant.now(),"WARN","Queue age 90s; consumer throughput below baseline","4b731ac99011"))),"demo-seed");
  var resolved=app.ingest(new AlertInput("demo","identity-1","token-refresh","Token refresh error spike","Short-lived authentication errors.","identity",Severity.SEV2,0.05,500L,List.of()),"demo-seed").incident();
  app.transition(resolved.id(),new Transition(Status.ACKNOWLEDGED,"Alert acknowledged."),"demo-seed");app.transition(resolved.id(),new Transition(Status.INVESTIGATING,"Configuration mismatch identified."),"demo-seed");app.transition(resolved.id(),new Transition(Status.RESOLVED,"Rolled back configuration and verified recovery."),"demo-seed");
  backdate(p.id(),26);backdate(resolved.id(),42);
 }
 @Transactional public void backdate(UUID id,int minutes){var i=incidents.findById(id).orElseThrow();i.createdAt=Instant.now().minusSeconds(minutes*60L);i.acknowledgedAt=i.createdAt.plusSeconds(120);if(i.status==Status.RESOLVED)i.resolvedAt=i.createdAt.plusSeconds(31*60);incidents.save(i);}
}
