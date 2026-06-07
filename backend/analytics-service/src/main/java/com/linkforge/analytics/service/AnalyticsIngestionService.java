package com.linkforge.analytics.service;

import com.linkforge.analytics.domain.ClickEventEntity;
import com.linkforge.analytics.domain.UrlAnalyticsDaily;
import com.linkforge.analytics.domain.UrlAnalyticsDailyId;
import com.linkforge.analytics.domain.UrlAnalyticsTotal;
import com.linkforge.analytics.repository.ClickEventRepository;
import com.linkforge.analytics.repository.UrlAnalyticsDailyRepository;
import com.linkforge.analytics.repository.UrlAnalyticsTotalRepository;
import com.linkforge.common.events.ClickEvent;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsIngestionService {
  private final ClickEventRepository clickEvents;
  private final UrlAnalyticsTotalRepository totals;
  private final UrlAnalyticsDailyRepository daily;

  public AnalyticsIngestionService(ClickEventRepository clickEvents, UrlAnalyticsTotalRepository totals, UrlAnalyticsDailyRepository daily) {
    this.clickEvents = clickEvents;
    this.totals = totals;
    this.daily = daily;
  }

  @Transactional
  public void ingest(ClickEvent event, int partition, long offset) {
    if (clickEvents.existsByKafkaPartitionAndKafkaOffset(partition, offset)) {
      return;
    }

    ClickEventEntity entity = new ClickEventEntity(
        event.eventId(),
        event.shortUrlId(),
        event.shortCode(),
        event.timestamp(),
        event.ipAddress(),
        event.userAgent(),
        event.device() == null ? null : event.device().type(),
        event.device() == null ? null : event.device().browser(),
        event.device() == null ? null : event.device().os(),
        event.geo() == null ? null : event.geo().country(),
        event.geo() == null ? null : event.geo().city(),
        partition,
        offset);
    clickEvents.save(entity);

    UrlAnalyticsTotal total = totals.findById(event.shortUrlId()).orElseGet(() -> new UrlAnalyticsTotal(event.shortUrlId(), event.shortCode()));
    total.increment(event.timestamp());
    totals.save(total);

    var metricDate = event.timestamp().atZone(ZoneOffset.UTC).toLocalDate();
    var dailyId = new UrlAnalyticsDailyId(event.shortUrlId(), metricDate);
    UrlAnalyticsDaily dailyAggregate = daily.findById(dailyId)
        .orElseGet(() -> new UrlAnalyticsDaily(event.shortUrlId(), event.shortCode(), metricDate));
    dailyAggregate.increment();
    daily.save(dailyAggregate);
  }
}
