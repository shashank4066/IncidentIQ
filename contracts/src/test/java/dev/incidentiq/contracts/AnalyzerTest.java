package dev.incidentiq.contracts;
import dev.incidentiq.contracts.Contracts.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
class AnalyzerTest {
 @Test void referencesObservedEvidence() {
  var log=new Evidence("log-1",Instant.now(),"ERROR","connection pool timeout","trace-1");
  var result=new HeuristicAnalyzer().analyze(new Context(UUID.randomUUID(),1,"Slow API","","payments",Severity.SEV2,List.of(log)));
  assertThat(result.source()).isEqualTo("HEURISTIC");
  assertThat(result.hypotheses()).hasSize(2).allSatisfy(h->assertThat(h.evidenceIds()).containsExactly("log-1"));
 }
 @Test void doesNotInventCauseWithoutEvidence() {
  var result=new HeuristicAnalyzer().analyze(new Context(UUID.randomUUID(),1,"Alert","","api",Severity.SEV4,List.of()));
  assertThat(result.hypotheses().getFirst().cause()).contains("Insufficient evidence");
 }
 @Test void redactsSecrets() {
  assertThat(Redactor.clean("password=abc api_key=xyz Authorization: Bearer abc.def user@host.com")).doesNotContain("=abc","=xyz","abc.def","user@host.com").contains("[REDACTED]","[EMAIL]");
 }
 @Test void redactedFieldsRespectStorageBounds() {
  assertThat(Redactor.clean("password=a ".repeat(30),200)).hasSize(200).doesNotContain("=a");
 }
}
