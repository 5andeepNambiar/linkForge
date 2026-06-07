package com.linkforge.analytics.api;

import com.linkforge.analytics.api.AnalyticsDtos.AnalyticsResponse;
import com.linkforge.analytics.api.AnalyticsDtos.DailyPoint;
import com.linkforge.analytics.api.AnalyticsDtos.RecentClick;
import com.linkforge.analytics.repository.ClickEventRepository;
import com.linkforge.analytics.repository.UrlAnalyticsDailyRepository;
import com.linkforge.analytics.repository.UrlAnalyticsTotalRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {
  private final UrlAnalyticsTotalRepository totals;
  private final UrlAnalyticsDailyRepository daily;
  private final ClickEventRepository events;

  public AnalyticsController(UrlAnalyticsTotalRepository totals, UrlAnalyticsDailyRepository daily, ClickEventRepository events) {
    this.totals = totals;
    this.daily = daily;
    this.events = events;
  }

  @GetMapping("/urls/{shortCode}")
  public AnalyticsResponse analytics(
      @PathVariable String shortCode,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    LocalDate end = to == null ? LocalDate.now() : to;
    LocalDate start = from == null ? end.minusDays(30) : from;
    var total = totals.findByShortCode(shortCode);
    List<DailyPoint> dailyPoints = daily.findByShortCodeAndMetricDateBetweenOrderByMetricDate(shortCode, start, end)
        .stream()
        .map(row -> new DailyPoint(row.getMetricDate(), row.getClicks()))
        .toList();
    List<RecentClick> recent = events.findTop50ByShortCodeOrderByOccurredAtDesc(shortCode)
        .stream()
        .map(row -> new RecentClick(row.getOccurredAt(), row.getIpAddress(), row.getDeviceType(), row.getBrowser(), row.getOs(), row.getCountry()))
        .toList();
    return new AnalyticsResponse(shortCode, total.map(value -> value.getTotalClicks()).orElse(0L), total.map(value -> value.getLastClickedAt()).orElse(null), dailyPoints, recent);
  }

  @GetMapping("/urls/{shortCode}/daily")
  public List<DailyPoint> daily(
      @PathVariable String shortCode,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return daily.findByShortCodeAndMetricDateBetweenOrderByMetricDate(shortCode, from, to)
        .stream()
        .map(row -> new DailyPoint(row.getMetricDate(), row.getClicks()))
        .toList();
  }
}
