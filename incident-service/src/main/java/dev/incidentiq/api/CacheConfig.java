package dev.incidentiq.api;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.incidentiq.api.Dtos.Summary;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.cache.*;
import org.springframework.data.redis.serializer.*;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import java.time.Duration;
@Configuration @Profile("cloud")
public class CacheConfig {
 @Bean RedisCacheManager cacheManager(RedisConnectionFactory c,ObjectMapper mapper){
  var serializer=new Jackson2JsonRedisSerializer<>(mapper,Summary.class);
  var config=RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofSeconds(10)).disableCachingNullValues().serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
  return RedisCacheManager.builder(c).withCacheConfiguration("summary",config).build();
 }
}
