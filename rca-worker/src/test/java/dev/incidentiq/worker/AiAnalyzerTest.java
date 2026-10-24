package dev.incidentiq.worker;
import com.fasterxml.jackson.databind.*;
import dev.incidentiq.contracts.Contracts.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
class AiAnalyzerTest {
 private final ObjectMapper json=new ObjectMapper().findAndRegisterModules();
 private Context context(){return new Context(UUID.randomUUID(),1,"Alert","","api",Severity.SEV3,List.of(new Evidence("log-1",Instant.now(),"ERROR","connection pool timeout","trace-1")));}
 @Test void noKeyUsesHonestOfflineMode(){
  var analyzer=new AiAnalyzer(json,"http://localhost:1","","unused",new SimpleMeterRegistry());
  var result=analyzer.analyze(context());assertThat(result.source()).isEqualTo("HEURISTIC");assertThat(result.unknowns()).isNotEmpty();
 }
 @Test void validatesAndReturnsAiEvidence(){
  var analyzer=new AiAnalyzer(json,"test-only-key","test-model",new SimpleMeterRegistry(),body->{assertThat(body.get("model")).isEqualTo("test-model");assertThat(body.get("messages").toString()).contains("untrusted DATA");return response("log-1");});
  var result=analyzer.analyze(context());assertThat(result.source()).isEqualTo("AI");assertThat(result.hypotheses().getFirst().evidenceIds()).containsExactly("log-1");
 }
 @Test void inventedEvidenceFallsBackToLabeledBaseline(){
  var analyzer=new AiAnalyzer(json,"test-only-key","test-model",new SimpleMeterRegistry(),body->response("invented-id"));
  var result=analyzer.analyze(context());assertThat(result.source()).isEqualTo("HEURISTIC");assertThat(result.unknowns()).anySatisfy(x->assertThat(x).contains("fallback"));
 }
 private JsonNode response(String evidence){
  var content=json.valueToTree(Map.of("summary","Database acquisition failures observed.","hypotheses",List.of(Map.of("cause","Pool exhaustion may be responsible.","evidenceIds",List.of(evidence))),"remediation",List.of("Inspect pool wait metrics."),"unknowns",List.of("Verify deployment changes."))).toString();
  return json.valueToTree(Map.of("choices",List.of(Map.of("message",Map.of("content",content)))));
 }
}
