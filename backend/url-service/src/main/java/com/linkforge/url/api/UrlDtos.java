package com.linkforge.url.api;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;

public final class UrlDtos {
  private UrlDtos() {
  }

  public record CreateUrlRequest(
      @NotBlank String url,
      @Future Instant expiresAt,
      String ownerId
  ) {}

  public record UrlResponse(
      UUID id,
      String shortCode,
      String shortUrl,
      String originalUrl,
      Instant expiresAt,
      Instant createdAt
  ) {}
}
