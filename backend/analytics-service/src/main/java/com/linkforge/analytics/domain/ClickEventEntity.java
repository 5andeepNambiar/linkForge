package com.linkforge.analytics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "click_events", uniqueConstraints = @UniqueConstraint(columnNames = {"kafka_partition", "kafka_offset"}))
public class ClickEventEntity {
  @Id
  private UUID eventId;

  @Column(nullable = false)
  private UUID shortUrlId;

  @Column(nullable = false, length = 16)
  private String shortCode;

  @Column(nullable = false)
  private Instant occurredAt;

  @Column(name = "ip_address")
  private String ipAddress;

  @Column(name = "user_agent", columnDefinition = "TEXT")
  private String userAgent;

  @Column(name = "device_type")
  private String deviceType;
  private String browser;
  private String os;
  private String country;
  private String city;
  @Column(name = "kafka_partition")
  private Integer kafkaPartition;
  @Column(name = "kafka_offset")
  private Long kafkaOffset;

  @Column(nullable = false)
  private Instant createdAt = Instant.now();

  protected ClickEventEntity() {
  }

  public ClickEventEntity(UUID eventId, UUID shortUrlId, String shortCode, Instant occurredAt,
      String ipAddress, String userAgent, String deviceType, String browser, String os,
      String country, String city, Integer kafkaPartition, Long kafkaOffset) {
    this.eventId = eventId;
    this.shortUrlId = shortUrlId;
    this.shortCode = shortCode;
    this.occurredAt = occurredAt;
    this.ipAddress = ipAddress;
    this.userAgent = userAgent;
    this.deviceType = deviceType;
    this.browser = browser;
    this.os = os;
    this.country = country;
    this.city = city;
    this.kafkaPartition = kafkaPartition;
    this.kafkaOffset = kafkaOffset;
  }

  public UUID getEventId() { return eventId; }
  public UUID getShortUrlId() { return shortUrlId; }
  public String getShortCode() { return shortCode; }
  public Instant getOccurredAt() { return occurredAt; }
  public String getIpAddress() { return ipAddress; }
  public String getUserAgent() { return userAgent; }
  public String getDeviceType() { return deviceType; }
  public String getBrowser() { return browser; }
  public String getOs() { return os; }
  public String getCountry() { return country; }
}
