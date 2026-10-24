package dev.incidentiq.worker;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.*;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.*;
import org.springframework.util.backoff.FixedBackOff;
import org.springframework.beans.factory.annotation.Value;
@Configuration
public class KafkaConfig {
 @Bean NewTopic events(@Value("${app.kafka.topic}") String topic){return TopicBuilder.name(topic).partitions(3).replicas(1).build();}
 @Bean NewTopic deadLetters(@Value("${app.kafka.topic}") String topic){return TopicBuilder.name(topic+".DLT").partitions(3).replicas(1).build();}
 @Bean DefaultErrorHandler errors(KafkaTemplate<String,String> kafka){
  var recoverer=new DeadLetterPublishingRecoverer(kafka);recoverer.setFailIfSendResultIsError(true);
  var handler=new DefaultErrorHandler(recoverer,new FixedBackOff(3000,3));handler.addNotRetryableExceptions(IllegalArgumentException.class);return handler;
 }
}
