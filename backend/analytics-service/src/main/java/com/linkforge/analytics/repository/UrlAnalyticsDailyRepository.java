package com.linkforge.analytics.repository;

import com.linkforge.analytics.domain.UrlAnalyticsDaily;
import com.linkforge.analytics.domain.UrlAnalyticsDailyId;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UrlAnalyticsDailyRepository extends JpaRepository<UrlAnalyticsDaily, UrlAnalyticsDailyId> {
  List<UrlAnalyticsDaily> findByShortCodeAndMetricDateBetweenOrderByMetricDate(String shortCode, LocalDate from, LocalDate to);

  List<UrlAnalyticsDaily> findByShortUrlIdAndMetricDateBetweenOrderByMetricDate(UUID shortUrlId, LocalDate from, LocalDate to);
}
