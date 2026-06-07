package com.linkforge.redirect.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkforge.common.cache.UrlCacheEntry;
import com.linkforge.redirect.domain.ShortUrlProjection;
import com.linkforge.redirect.repository.ShortUrlLookupRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RedirectResolver {
  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;
  private final ShortUrlLookupRepository repository;

  public RedirectResolver(StringRedisTemplate redisTemplate, ObjectMapper objectMapper, ShortUrlLookupRepository repository) {
    this.redisTemplate = redisTemplate;
    this.objectMapper = objectMapper;
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public Optional<UrlCacheEntry> resolve(String shortCode) {
    String cacheKey = "url:" + shortCode;
    String payload = redisTemplate.opsForValue().get(cacheKey);
    if (payload != null) {
      try {
        UrlCacheEntry entry = objectMapper.readValue(payload, UrlCacheEntry.class);
        return entry.expired(Instant.now()) ? Optional.empty() : Optional.of(entry);
      } catch (JsonProcessingException ignored) {
        redisTemplate.delete(cacheKey);
      }
    }
    return repository.findByShortCode(shortCode)
        .filter(this::isRedirectable)
        .map(this::toCacheEntry)
        .map(entry -> {
          cache(cacheKey, entry);
          return entry;
        });
  }

  private boolean isRedirectable(ShortUrlProjection shortUrl) {
    return shortUrl.isActive()
        && (shortUrl.getExpiresAt() == null || shortUrl.getExpiresAt().isAfter(Instant.now()));
  }

  private UrlCacheEntry toCacheEntry(ShortUrlProjection shortUrl) {
    return new UrlCacheEntry(shortUrl.getId(), shortUrl.getShortCode(), shortUrl.getOriginalUrl(), shortUrl.getExpiresAt());
  }

  private void cache(String cacheKey, UrlCacheEntry entry) {
    try {
      String payload = objectMapper.writeValueAsString(entry);
      if (entry.expiresAt() == null) {
        redisTemplate.opsForValue().set(cacheKey, payload);
      } else {
        Duration ttl = Duration.between(Instant.now(), entry.expiresAt());
        if (!ttl.isNegative() && !ttl.isZero()) {
          redisTemplate.opsForValue().set(cacheKey, payload, ttl);
        }
      }
    } catch (JsonProcessingException ignored) {
      // Redirect correctness comes from PostgreSQL; cache refill failure only affects latency.
    }
  }
}
