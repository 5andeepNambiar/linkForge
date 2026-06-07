package com.linkforge.redirect;

import com.linkforge.common.ratelimit.RedisTokenBucketRateLimiter;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootApplication(scanBasePackages = {"com.linkforge.redirect", "com.linkforge.common"})
public class RedirectServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(RedirectServiceApplication.class, args);
  }

  @Bean
  RedisTokenBucketRateLimiter redisTokenBucketRateLimiter(
      StringRedisTemplate redisTemplate,
      @Value("${linkforge.rate-limit.max-requests}") int maxRequests,
      @Value("${linkforge.rate-limit.window-seconds}") long windowSeconds) {
    return new RedisTokenBucketRateLimiter(redisTemplate, maxRequests, Duration.ofSeconds(windowSeconds));
  }
}
