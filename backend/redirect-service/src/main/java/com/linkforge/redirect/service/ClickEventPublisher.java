package com.linkforge.redirect.service;

import com.linkforge.common.events.ClickEvent;
import com.linkforge.common.events.ClickEvent.GeoMetadata;
import com.linkforge.common.cache.UrlCacheEntry;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ClickEventPublisher {
  private static final Logger log = LoggerFactory.getLogger(ClickEventPublisher.class);

  private final KafkaTemplate<String, ClickEvent> kafkaTemplate;
  private final UserAgentClassifier userAgentClassifier;
  private final String topic;

  public ClickEventPublisher(
      KafkaTemplate<String, ClickEvent> kafkaTemplate,
      UserAgentClassifier userAgentClassifier,
      @Value("${linkforge.click-topic}") String topic) {
    this.kafkaTemplate = kafkaTemplate;
    this.userAgentClassifier = userAgentClassifier;
    this.topic = topic;
  }

  public void publish(UrlCacheEntry entry, String ipAddress, String userAgent) {
    ClickEvent event = new ClickEvent(
        UUID.randomUUID(),
        entry.id(),
        entry.shortCode(),
        Instant.now(),
        ipAddress,
        userAgent,
        userAgentClassifier.classify(userAgent),
        new GeoMetadata(inferCountry(ipAddress), null));
    kafkaTemplate.send(topic, entry.shortCode(), event)
        .whenComplete((result, error) -> {
          if (error != null) {
            log.warn("click event publish failed for shortCode={}", entry.shortCode(), error);
          }
        });
  }

  private String inferCountry(String ipAddress) {
    if (ipAddress == null || ipAddress.startsWith("127.") || ipAddress.startsWith("10.") || ipAddress.startsWith("192.168.")) {
      return "LOCAL";
    }
    return "ZZ";
  }
}
