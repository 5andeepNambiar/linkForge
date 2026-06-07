package com.linkforge.analytics.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class AnalyticsDtos {
  private AnalyticsDtos() {
  }

  public record AnalyticsResponse(
      String shortCode,
      long totalClicks,
      Instant lastClickedAt,
      List<DailyPoint> daily,
      List<RecentClick> recentClicks
  ) {}

  public record DailyPoint(LocalDate date, long clicks) {}

  public record RecentClick(Instant timestamp, String ipAddress, String deviceType, String browser, String os, String country) {}
}
