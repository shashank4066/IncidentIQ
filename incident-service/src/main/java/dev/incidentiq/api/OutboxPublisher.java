package dev.incidentiq.api;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
/** A committed row is retried until Kafka accepts it. Consumers must handle duplicates. */
@Component @Profile("cloud")
public class OutboxPublisher {
 private final Outbox outbox;private final KafkaTemplate<String,String> kafka;private final String topic;
 public OutboxPublisher(Outbox o,KafkaTemplate<String,String> k,@Value("${app.kafka.topic}") String t,MeterRegistry m){outbox=o;kafka=k;topic=t;m.gauge("incidentiq_outbox_pending",o,x->x.countByPublishedAtIsNull());}
 @Scheduled(fixedDelay=2000) @Transactional public void publish(){
  for(var e:outbox.claim()) {e.attempts++;try{kafka.send(topic,e.incidentId.toString(),e.payload).get(18,TimeUnit.SECONDS);e.publishedAt=Instant.now();}catch(InterruptedException ex){Thread.currentThread().interrupt();return;}catch(Exception ex){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Outbox delivery deferred for event {}",e.id);break;}}
 }
}
