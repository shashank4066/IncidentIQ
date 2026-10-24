package dev.incidentiq.contracts;
import dev.incidentiq.contracts.Contracts.*;
import java.time.Instant;
import java.util.*;
/** Offline baseline: hypotheses, never claims of proven causation. */
public final class HeuristicAnalyzer {
 public Analysis analyze(Context c) {
  var hs=new ArrayList<Hypothesis>(); var steps=new LinkedHashSet<String>();
  match(c,hs,steps,List.of("connection pool","too many connections","sqltransientconnection"),"Database connection pool exhaustion may be increasing latency.","Inspect active connections, pool wait time and slow queries; compare against recent deployments before changing pool limits.");
  match(c,hs,steps,List.of("outofmemory","oomkilled","heap space"),"Memory pressure may be terminating or stalling the application.","Inspect pod restart reasons, working set and heap profiles; check recent allocation changes.");
  match(c,hs,steps,List.of("timeout","timed out","connection refused","503","502"),"An unavailable or slow dependency may be causing cascading failures.","Correlate trace IDs with dependency latency and health; inspect timeout and circuit breaker metrics.");
  match(c,hs,steps,List.of("disk full","no space left"),"Storage exhaustion may be preventing writes.","Inspect volume usage and retention; preserve evidence before removing data.");
  if(hs.isEmpty()) { hs.add(new Hypothesis("Insufficient evidence to identify a likely failure mechanism.",List.of())); steps.add("Collect error logs, traces, dependency health and deployment changes around incident start."); }
  return new Analysis("HEURISTIC",c.service()+" has a "+c.severity()+" incident: "+c.title()+". Evaluated "+c.logs().size()+" log entries.",hs,List.copyOf(steps),List.of("Hypotheses require human validation; correlation does not prove causation.","Deployment history, dependency telemetry and business impact may be incomplete."),Instant.now());
 }
 private void match(Context c,List<Hypothesis> hs,Set<String> steps,List<String> needles,String cause,String step) {
  var ids=c.logs().stream().filter(l->needles.stream().anyMatch(n->l.message().toLowerCase(Locale.ROOT).contains(n))).map(Evidence::id).toList();
  if(!ids.isEmpty()) { hs.add(new Hypothesis(cause,ids)); steps.add(step); }
 }
}
