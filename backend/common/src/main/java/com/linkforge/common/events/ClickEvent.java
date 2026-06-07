package com.linkforge.common.events;

import java.time.Instant;
import java.util.UUID;

public record ClickEvent(
    UUID eventId,
    UUID shortUrlId,
    String shortCode,
    Instant timestamp,
    String ipAddress,
    String userAgent,
    DeviceMetadata device,
    GeoMetadata geo
) {
  public record DeviceMetadata(String type, String browser, String os) {}

  public record GeoMetadata(String country, String city) {}
}
