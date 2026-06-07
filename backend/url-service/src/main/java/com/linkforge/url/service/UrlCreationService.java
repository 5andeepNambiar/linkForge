package com.linkforge.url.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkforge.common.cache.UrlCacheEntry;
import com.linkforge.common.ids.Base62;
import com.linkforge.common.ids.SnowflakeIdGenerator;
import com.linkforge.url.api.UrlDtos.CreateUrlRequest;
import com.linkforge.url.domain.ShortUrl;
import com.linkforge.url.repository.ShortUrlRepository;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UrlCreationService {
  private final ShortUrlRepository repository;
  private final SnowflakeIdGenerator idGenerator;
  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;
  private final String publicBaseUrl;

  public UrlCreationService(
      ShortUrlRepository repository,
      SnowflakeIdGenerator idGenerator,
      StringRedisTemplate redisTemplate,
      ObjectMapper objectMapper,
      @Value("${linkforge.public-base-url}") String publicBaseUrl) {
    this.repository = repository;
    this.idGenerator = idGenerator;
    this.redisTemplate = redisTemplate;
    this.objectMapper = objectMapper;
    this.publicBaseUrl = publicBaseUrl;
  }

  @Transactional
  public CreationResult create(CreateUrlRequest request) {
    String normalized = normalize(request.url());
    String key = idempotencyKey(normalized, request.ownerId(), request.expiresAt());
    return repository.findByIdempotencyKey(key)
        .map(existing -> new CreationResult(existing, false))
        .orElseGet(() -> createNew(request, normalized, key));
  }

  public List<ShortUrl> list(String ownerId) {
    if (ownerId == null || ownerId.isBlank()) {
      return repository.findTop100ByOrderByCreatedAtDesc();
    }
    return repository.findTop100ByOwnerIdOrderByCreatedAtDesc(ownerId);
  }

  public String shortUrl(String shortCode) {
    return publicBaseUrl.replaceAll("/+$", "") + "/" + shortCode;
  }

  private CreationResult createNew(CreateUrlRequest request, String normalized, String key) {
    for (int attempt = 0; attempt < 5; attempt++) {
      String code = Base62.encode(idGenerator.nextId());
      if (repository.existsByShortCode(code)) {
        continue;
      }
      ShortUrl shortUrl = new ShortUrl(UUID.randomUUID(), code, request.url(), normalized, key, request.ownerId(), request.expiresAt());
      try {
        ShortUrl saved = repository.saveAndFlush(shortUrl);
        cache(saved);
        return new CreationResult(saved, true);
      } catch (DataIntegrityViolationException duplicate) {
        return repository.findByIdempotencyKey(key)
            .map(existing -> new CreationResult(existing, false))
            .orElseThrow(() -> duplicate);
      }
    }
    throw new IllegalStateException("unable to generate unique short code");
  }

  private void cache(ShortUrl shortUrl) {
    UrlCacheEntry entry = new UrlCacheEntry(shortUrl.getId(), shortUrl.getShortCode(), shortUrl.getOriginalUrl(), shortUrl.getExpiresAt());
    try {
      String payload = objectMapper.writeValueAsString(entry);
      String key = "url:" + shortUrl.getShortCode();
      if (shortUrl.getExpiresAt() == null) {
        redisTemplate.opsForValue().set(key, payload);
      } else {
        Duration ttl = Duration.between(Instant.now(), shortUrl.getExpiresAt());
        if (!ttl.isNegative() && !ttl.isZero()) {
          redisTemplate.opsForValue().set(key, payload, ttl);
        }
      }
    } catch (JsonProcessingException ignored) {
      // Cache priming failure should not fail URL creation.
    }
  }

  private String normalize(String url) {
    URI uri = URI.create(url.trim());
    String scheme = uri.getScheme() == null ? "https" : uri.getScheme().toLowerCase(Locale.ROOT);
    String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
    int port = uri.getPort();
    String authority = port > 0 ? host + ":" + port : host;
    return URI.create(scheme + "://" + authority + nullToEmpty(uri.getRawPath()) + nullToEmptyPrefixed("?", uri.getRawQuery())).normalize().toString();
  }

  private String idempotencyKey(String normalizedUrl, String ownerId, Instant expiresAt) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      String value = normalizedUrl + "|" + nullToEmpty(ownerId) + "|" + (expiresAt == null ? "" : expiresAt);
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private String nullToEmptyPrefixed(String prefix, String value) {
    return value == null ? "" : prefix + value;
  }

  public record CreationResult(ShortUrl shortUrl, boolean created) {}
}
