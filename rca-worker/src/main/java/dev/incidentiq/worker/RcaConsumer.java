package dev.incidentiq.worker;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.incidentiq.contracts.Contracts.*;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.beans.factory.annotation.Value;
import java.net.http.HttpClient;
import java.time.Duration;
@Component
public class RcaConsumer {
 private final ObjectMapper json;private final AiAnalyzer analyzer;private final RestClient api;
 public RcaConsumer(ObjectMapper json,AiAnalyzer analyzer,RestClient.Builder builder,@Value("${app.api.url}") String url,@Value("${app.api.worker-password}") String password){
  this.json=json;this.analyzer=analyzer;
  var factory=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());factory.setReadTimeout(Duration.ofSeconds(15));
  api=builder.baseUrl(url).requestFactory(factory).defaultHeaders(h->h.setBasicAuth("rca-worker",password)).build();
 }
 @KafkaListener(topics="${app.kafka.topic}",groupId="incidentiq-rca-v1",concurrency="2")
 public void onEvent(String payload) throws Exception {
  var e=json.readValue(payload,Event.class);
  if(e.schemaVersion()!=1)throw new IllegalArgumentException("Unsupported event schema");
  if(!"RCA_REQUESTED".equals(e.type()))return;
  if(e.context()==null||e.requestId()==null)throw new IllegalArgumentException("Missing RCA context");
  var analysis=analyzer.analyze(e.context());
  try {
   api.post().uri("/api/internal/incidents/{id}/rca",e.incidentId()).contentType(org.springframework.http.MediaType.APPLICATION_JSON).body(new RcaCallback(e.requestId(),e.context().revision(),analysis)).retrieve().toBodilessEntity();
  }catch(HttpClientErrorException.Conflict ex){org.slf4j.LoggerFactory.getLogger(getClass()).info("Discarded obsolete RCA request {}",e.requestId());}
 }
}
