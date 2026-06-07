package com.linkforge.url;

import com.linkforge.common.ids.SnowflakeIdGenerator;
import com.linkforge.common.ratelimit.RedisTokenBucketRateLimiter;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootApplication(scanBasePackages = {"com.linkforge.url", "com.linkforge.common"})
public class UrlServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(UrlServiceApplication.class, args);
  }

  @Bean
  SnowflakeIdGenerator snowflakeIdGenerator(@Value("${linkforge.node-id}") long nodeId) {
    return new SnowflakeIdGenerator(nodeId);
  }

  @Bean
  RedisTokenBucketRateLimiter redisTokenBucketRateLimiter(
      StringRedisTemplate redisTemplate,
      @Value("${linkforge.rate-limit.max-requests}") int maxRequests,
      @Value("${linkforge.rate-limit.window-seconds}") long windowSeconds) {
    return new RedisTokenBucketRateLimiter(redisTemplate, maxRequests, Duration.ofSeconds(windowSeconds));
  }
}
