package com.linkforge.analytics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "url_analytics_totals")
public class UrlAnalyticsTotal {
  @Id
  private UUID shortUrlId;

  @Column(nullable = false, unique = true, length = 16)
  private String shortCode;

  @Column(nullable = false)
  private long totalClicks;

  @Column(nullable = false)
  private long uniqueIps;

  private Instant lastClickedAt;

  protected UrlAnalyticsTotal() {
  }

  public UrlAnalyticsTotal(UUID shortUrlId, String shortCode) {
    this.shortUrlId = shortUrlId;
    this.shortCode = shortCode;
  }

  public void increment(Instant occurredAt) {
    totalClicks++;
    lastClickedAt = occurredAt;
  }

  public UUID getShortUrlId() { return shortUrlId; }
  public String getShortCode() { return shortCode; }
  public long getTotalClicks() { return totalClicks; }
  public long getUniqueIps() { return uniqueIps; }
  public Instant getLastClickedAt() { return lastClickedAt; }
}
