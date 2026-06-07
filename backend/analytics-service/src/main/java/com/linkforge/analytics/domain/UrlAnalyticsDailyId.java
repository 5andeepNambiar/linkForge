package com.linkforge.analytics.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class UrlAnalyticsDailyId implements Serializable {
  private UUID shortUrlId;
  private LocalDate metricDate;

  public UrlAnalyticsDailyId() {
  }

  public UrlAnalyticsDailyId(UUID shortUrlId, LocalDate metricDate) {
    this.shortUrlId = shortUrlId;
    this.metricDate = metricDate;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof UrlAnalyticsDailyId that)) {
      return false;
    }
    return Objects.equals(shortUrlId, that.shortUrlId) && Objects.equals(metricDate, that.metricDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(shortUrlId, metricDate);
  }
}
