package com.linkforge.redirect.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "short_urls")
public class ShortUrlProjection {
  @Id
  private UUID id;

  @Column(nullable = false, unique = true, length = 16)
  private String shortCode;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String originalUrl;

  @Column(nullable = false)
  private boolean active;

  private Instant expiresAt;

  protected ShortUrlProjection() {
  }

  public UUID getId() { return id; }
  public String getShortCode() { return shortCode; }
  public String getOriginalUrl() { return originalUrl; }
  public boolean isActive() { return active; }
  public Instant getExpiresAt() { return expiresAt; }
}
