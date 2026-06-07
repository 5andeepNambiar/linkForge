package com.linkforge.common.ids;

import java.time.Clock;
import java.time.Instant;

public final class SnowflakeIdGenerator {
  private static final long CUSTOM_EPOCH_MILLIS = Instant.parse("2026-01-01T00:00:00Z").toEpochMilli();
  private static final long NODE_ID_BITS = 10L;
  private static final long SEQUENCE_BITS = 12L;
  private static final long MAX_NODE_ID = (1L << NODE_ID_BITS) - 1L;
  private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1L;

  private final Clock clock;
  private final long nodeId;
  private long lastTimestamp = -1L;
  private long sequence = 0L;

  public SnowflakeIdGenerator(long nodeId) {
    this(nodeId, Clock.systemUTC());
  }

  SnowflakeIdGenerator(long nodeId, Clock clock) {
    if (nodeId < 0 || nodeId > MAX_NODE_ID) {
      throw new IllegalArgumentException("nodeId must be between 0 and " + MAX_NODE_ID);
    }
    this.nodeId = nodeId;
    this.clock = clock;
  }

  public synchronized long nextId() {
    long timestamp = nowMillis();
    if (timestamp < lastTimestamp) {
      throw new IllegalStateException("clock moved backwards");
    }
    if (timestamp == lastTimestamp) {
      sequence = (sequence + 1) & MAX_SEQUENCE;
      if (sequence == 0) {
        timestamp = waitUntilNextMillis(timestamp);
      }
    } else {
      sequence = 0L;
    }
    lastTimestamp = timestamp;
    return ((timestamp - CUSTOM_EPOCH_MILLIS) << (NODE_ID_BITS + SEQUENCE_BITS))
        | (nodeId << SEQUENCE_BITS)
        | sequence;
  }

  private long waitUntilNextMillis(long timestamp) {
    long current = timestamp;
    while (current <= lastTimestamp) {
      current = nowMillis();
    }
    return current;
  }

  private long nowMillis() {
    return clock.millis();
  }
}
