package com.linkforge.common.cache;

import java.time.Instant;
import java.util.UUID;

public record UrlCacheEntry(
    UUID id,
    String shortCode,
    String originalUrl,
    Instant expiresAt
) {
  public boolean expired(Instant now) {
    return expiresAt != null && !expiresAt.isAfter(now);
  }
}
