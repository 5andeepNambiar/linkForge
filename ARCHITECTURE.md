# LinkForge Distributed URL Shortener with Analytics Platform

## 1. High-level Architecture Diagram

```text
                    +----------------------+
                    | Angular Dashboard    |
                    | URL + Analytics UI   |
                    +----------+-----------+
                               |
                               v
                    +----------------------+
                    | URL Service          |
                    | create/list metadata |
                    +----+------------+----+
                         |            |
        write-through    |            | system of record
                         v            v
                    +---------+   +----------------+
                    | Redis   |   | PostgreSQL     |
                    | cache + |   | URLs, events,  |
                    | limits  |   | aggregates     |
                    +----+----+   +--------+-------+
                         ^                 ^
                         | cache-aside     |
+----------+      +------+-------+         |
| Browser  +----->| Redirect     |---------+
| Clients  |      | Service      | DB miss fallback
+----------+      +------+-------+
                         |
                         | async click event; redirect must not block
                         v
                  +--------------+
                  | Kafka        |
                  | url-click-   |
                  | events       |
                  +------+-------+
                         |
                         v
                  +--------------+
                  | Analytics    |
                  | Service      |
                  +--------------+
```

## 2. Microservices Breakdown and Responsibilities

### URL Service

- Owns URL creation and metadata APIs.
- Normalizes and validates long URLs.
- Ensures idempotent URL creation using a SHA-256 idempotency key over canonical URL, optional expiry, and owner.
- Generates short codes with a Snowflake-style 64-bit ID encoded as Base62.
- Writes URL metadata to PostgreSQL and primes Redis using a write-through cache update.
- Handles soft expiration by storing `expires_at` and `active`; actual redirect enforcement happens in the Redirect Service.

### Redirect Service

- Owns the hot path: `GET /{shortCode}`.
- Applies Redis-backed rate limiting before lookup.
- Resolves from Redis first, falls back to PostgreSQL on cache miss, and repopulates Redis with the remaining TTL.
- Publishes click events to Kafka asynchronously after resolution.
- Degrades gracefully if Kafka is down: redirect still succeeds and the event is logged as dropped.

### Analytics Service

- Consumes `url-click-events` from Kafka.
- Stores raw click events for audit/debug windows.
- Updates per-URL daily aggregates and total click counts in PostgreSQL.
- Exposes query APIs for dashboard analytics.
- Uses eventual consistency; the dashboard may trail redirects by seconds under load.

### Rate Limiting Service

- Implemented as a shared Redis-backed token bucket component used by URL and Redirect services.
- Uses atomic Redis Lua script semantics conceptually; this skeleton uses Spring Data Redis operations and can be swapped for a Lua script in production.
- Enforces per-IP limits and can be extended with authenticated user keys.

## 3. PostgreSQL Schema Design

```sql
CREATE TABLE short_urls (
  id UUID PRIMARY KEY,
  short_code VARCHAR(16) NOT NULL UNIQUE,
  original_url TEXT NOT NULL,
  normalized_url TEXT NOT NULL,
  idempotency_key CHAR(64) NOT NULL UNIQUE,
  owner_id VARCHAR(128),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  expires_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_short_urls_active_expiry ON short_urls (short_code, active, expires_at);
CREATE INDEX idx_short_urls_owner_created ON short_urls (owner_id, created_at DESC);

CREATE TABLE click_events (
  event_id UUID PRIMARY KEY,
  short_url_id UUID NOT NULL REFERENCES short_urls(id),
  short_code VARCHAR(16) NOT NULL,
  occurred_at TIMESTAMPTZ NOT NULL,
  ip_address INET,
  user_agent TEXT,
  device_type VARCHAR(32),
  browser VARCHAR(64),
  os VARCHAR(64),
  country VARCHAR(2),
  city VARCHAR(128),
  kafka_partition INT,
  kafka_offset BIGINT,
  created_at TIMESTAMPTZ NOT NULL,
  UNIQUE (kafka_partition, kafka_offset)
);

CREATE TABLE url_analytics_totals (
  short_url_id UUID PRIMARY KEY REFERENCES short_urls(id),
  short_code VARCHAR(16) NOT NULL UNIQUE,
  total_clicks BIGINT NOT NULL DEFAULT 0,
  unique_ips BIGINT NOT NULL DEFAULT 0,
  last_clicked_at TIMESTAMPTZ
);

CREATE TABLE url_analytics_daily (
  short_url_id UUID NOT NULL REFERENCES short_urls(id),
  short_code VARCHAR(16) NOT NULL,
  metric_date DATE NOT NULL,
  clicks BIGINT NOT NULL DEFAULT 0,
  unique_ips BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (short_url_id, metric_date)
);
```

## 4. Kafka Event Schema Definition

Topic: `url-click-events`

Key: `shortCode`

```json
{
  "eventId": "uuid",
  "shortUrlId": "uuid",
  "shortCode": "abc123",
  "timestamp": "2026-05-27T17:00:00Z",
  "ipAddress": "203.0.113.10",
  "userAgent": "Mozilla/5.0 ...",
  "device": {
    "type": "mobile",
    "browser": "Chrome",
    "os": "Android"
  },
  "geo": {
    "country": "IN",
    "city": "Bengaluru"
  }
}
```

## 5. REST API Specifications

### URL Service

`POST /api/v1/urls`

```json
{
  "url": "https://example.com/product?id=42",
  "expiresAt": "2026-06-30T00:00:00Z",
  "ownerId": "user-123"
}
```

Returns `201 Created` or `200 OK` for an idempotent duplicate.

```json
{
  "id": "uuid",
  "shortCode": "b9X2kP",
  "shortUrl": "http://localhost:8082/b9X2kP",
  "originalUrl": "https://example.com/product?id=42",
  "expiresAt": "2026-06-30T00:00:00Z",
  "createdAt": "2026-05-27T17:00:00Z"
}
```

`GET /api/v1/urls?ownerId=user-123`

Returns generated URL metadata.

### Redirect Service

`GET /{shortCode}`

- `302 Found` with `Location` header on success.
- `404 Not Found` for unknown, inactive, or expired short codes.
- `429 Too Many Requests` when rate limited.

### Analytics Service

`GET /api/v1/analytics/urls/{shortCode}`

Returns total clicks, recent click events, device breakdown, and daily series.

`GET /api/v1/analytics/urls/{shortCode}/daily?from=2026-05-01&to=2026-05-27`

Returns date-bucketed trend data.

## 6. Backend Spring Boot Project Structure

```text
backend/
  pom.xml
  common/
    src/main/java/com/linkforge/common/
      cache/UrlCacheEntry.java
      events/ClickEvent.java
      ids/Base62.java
      ids/SnowflakeIdGenerator.java
      ratelimit/RedisTokenBucketRateLimiter.java
  url-service/
    src/main/java/com/linkforge/url/
      UrlServiceApplication.java
      api/UrlController.java
      domain/ShortUrl.java
      repository/ShortUrlRepository.java
      service/UrlCreationService.java
  redirect-service/
    src/main/java/com/linkforge/redirect/
      RedirectServiceApplication.java
      api/RedirectController.java
      repository/ShortUrlLookupRepository.java
      service/RedirectResolver.java
      service/ClickEventPublisher.java
  analytics-service/
    src/main/java/com/linkforge/analytics/
      AnalyticsServiceApplication.java
      consumer/ClickEventConsumer.java
      api/AnalyticsController.java
      domain/ClickEventEntity.java
      domain/UrlAnalyticsDaily.java
      domain/UrlAnalyticsTotal.java
      repository/*
```

## 7. Angular Frontend Structure

```text
frontend/
  src/app/
    core/api/url-api.service.ts
    core/api/analytics-api.service.ts
    features/url-shortener/
      url-shortener.component.ts
    features/url-table/
      url-table.component.ts
    features/analytics-dashboard/
      analytics-dashboard.component.ts
    app.routes.ts
```

## 8. Docker Compose Setup for Full System

The compose file starts PostgreSQL, Redis, Kafka, all three Spring Boot services, and the Angular frontend. Each service is independently buildable and horizontally scalable behind a real load balancer; local compose runs one replica per service.

## 9. Key Tradeoffs and Design Decisions

- Redis vs PostgreSQL on redirects: Redis is the primary read path because redirect workloads are read-heavy and latency-sensitive. PostgreSQL remains authoritative for consistency, cache rehydration, and recovery.
- Kafka vs synchronous analytics: Kafka decouples redirects from analytics writes. Redirects remain available if analytics is degraded, trading immediate analytics accuracy for low latency and fault isolation.
- Cache strategy: URL creation uses write-through cache priming. Redirect uses cache-aside on miss. Redis TTL mirrors URL expiry when present.
- Collision resistance: Short codes come from Snowflake-style distributed IDs encoded in Base62. PostgreSQL unique constraints still protect against collisions or node misconfiguration, and creation retries on duplicate code.
- Idempotency: URL creation uses a unique idempotency key so retries return the same short URL when inputs match.
- Expiration: Expiry is enforced at redirect time. Optional scheduled cleanup can deactivate expired rows asynchronously without correctness depending on the job.
- Fault tolerance: Redirect continues when Kafka is unavailable. Redis outages degrade to PostgreSQL lookup if permitted by service policy; in high-scale production, circuit breakers protect the database.
- Bottlenecks: Redis hot keys for extremely popular URLs, Kafka partition skew by short code, PostgreSQL aggregate write contention, and rate limiter Redis command volume. Mitigations include local near-cache, Kafka partition expansion, buffered aggregate updates, and Redis cluster.

## 10. Data Flows

### URL Creation Flow

1. Dashboard calls URL Service `POST /api/v1/urls`.
2. URL Service normalizes the URL and computes an idempotency key.
3. If the key exists, the existing record is returned.
4. Otherwise a Snowflake ID is generated and Base62-encoded as the short code.
5. URL metadata is inserted into PostgreSQL under unique constraints.
6. URL Service writes `url:{shortCode}` into Redis with expiry-aligned TTL.
7. Response returns the short code and public short URL.

### URL Redirect Flow

1. Client requests Redirect Service `GET /{shortCode}`.
2. Redirect Service applies Redis token bucket rate limiting by IP.
3. Service looks up `url:{shortCode}` in Redis.
4. On cache hit, it redirects immediately and publishes Kafka analytics asynchronously.
5. On cache miss, it reads PostgreSQL, validates `active` and `expires_at`, repopulates Redis, redirects, and publishes the event.
6. If Kafka publish fails, redirect still succeeds.

### Analytics Pipeline Flow

1. Redirect Service emits `ClickEvent` to Kafka topic `url-click-events`.
2. Analytics Service consumes events in a consumer group.
3. It stores raw click events with Kafka partition/offset idempotency.
4. It increments total and daily aggregates in PostgreSQL.
5. Dashboard queries Analytics Service APIs for totals, trends, and event breakdowns.
