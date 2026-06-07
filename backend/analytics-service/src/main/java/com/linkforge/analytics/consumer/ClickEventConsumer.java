package com.linkforge.analytics.consumer;

import com.linkforge.analytics.service.AnalyticsIngestionService;
import com.linkforge.common.events.ClickEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class ClickEventConsumer {
  private final AnalyticsIngestionService ingestionService;

  public ClickEventConsumer(AnalyticsIngestionService ingestionService) {
    this.ingestionService = ingestionService;
  }

  @KafkaListener(topics = "${linkforge.click-topic}")
  public void consume(
      ClickEvent event,
      @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
      @Header(KafkaHeaders.OFFSET) long offset) {
    ingestionService.ingest(event, partition, offset);
  }
}
