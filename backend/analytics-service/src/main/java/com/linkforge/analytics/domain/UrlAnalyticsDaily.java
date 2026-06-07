package com.linkforge.analytics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@IdClass(UrlAnalyticsDailyId.class)
@Table(name = "url_analytics_daily")
public class UrlAnalyticsDaily {
  @Id
  private UUID shortUrlId;

  @Id
  private LocalDate metricDate;

  @Column(nullable = false, length = 16)
  private String shortCode;

  @Column(nullable = false)
  private long clicks;

  @Column(nullable = false)
  private long uniqueIps;

  protected UrlAnalyticsDaily() {
  }

  public UrlAnalyticsDaily(UUID shortUrlId, String shortCode, LocalDate metricDate) {
    this.shortUrlId = shortUrlId;
    this.shortCode = shortCode;
    this.metricDate = metricDate;
  }

  public void increment() {
    clicks++;
  }

  public UUID getShortUrlId() { return shortUrlId; }
  public LocalDate getMetricDate() { return metricDate; }
  public String getShortCode() { return shortCode; }
  public long getClicks() { return clicks; }
  public long getUniqueIps() { return uniqueIps; }
}
