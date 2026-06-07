package com.linkforge.common.ratelimit;

import java.time.Duration;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;

public class RedisTokenBucketRateLimiter {
  private final StringRedisTemplate redisTemplate;
  private final int maxRequests;
  private final Duration window;

  public RedisTokenBucketRateLimiter(StringRedisTemplate redisTemplate, int maxRequests, Duration window) {
    this.redisTemplate = redisTemplate;
    this.maxRequests = maxRequests;
    this.window = window;
  }

  public boolean allow(String key) {
    String redisKey = "rate:" + key;
    long now = System.currentTimeMillis();
    long windowStart = now - window.toMillis();
    redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, windowStart);
    Long count = redisTemplate.opsForZSet().zCard(redisKey);
    if (count != null && count >= maxRequests) {
      return false;
    }
    redisTemplate.opsForZSet().add(redisKey, now + ":" + UUID.randomUUID(), now);
    redisTemplate.expire(redisKey, window.plusSeconds(1));
    return true;
  }
}
