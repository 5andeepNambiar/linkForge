package com.linkforge.url.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "short_urls")
public class ShortUrl {
  @Id
  private UUID id;

  @Column(nullable = false, unique = true, length = 16)
  private String shortCode;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String originalUrl;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String normalizedUrl;

  @Column(nullable = false, unique = true, length = 64)
  private String idempotencyKey;

  @Column(length = 128)
  private String ownerId;

  @Column(nullable = false)
  private boolean active = true;

  private Instant expiresAt;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  protected ShortUrl() {
  }

  public ShortUrl(UUID id, String shortCode, String originalUrl, String normalizedUrl,
      String idempotencyKey, String ownerId, Instant expiresAt) {
    this.id = id;
    this.shortCode = shortCode;
    this.originalUrl = originalUrl;
    this.normalizedUrl = normalizedUrl;
    this.idempotencyKey = idempotencyKey;
    this.ownerId = ownerId;
    this.expiresAt = expiresAt;
  }

  @PrePersist
  void onCreate() {
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  public UUID getId() { return id; }
  public String getShortCode() { return shortCode; }
  public String getOriginalUrl() { return originalUrl; }
  public String getNormalizedUrl() { return normalizedUrl; }
  public String getIdempotencyKey() { return idempotencyKey; }
  public String getOwnerId() { return ownerId; }
  public boolean isActive() { return active; }
  public Instant getExpiresAt() { return expiresAt; }
  public Instant getCreatedAt() { return createdAt; }
}
