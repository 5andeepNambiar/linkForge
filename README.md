LinkForge
Production-grade distributed URL shortener with asynchronous clickstream analytics.

LinkForge is a full-stack system design project built to demonstrate how a real redirect-heavy URL platform can be split into independently scalable services. The redirect path is optimized for low latency with Redis-first lookups, while analytics is processed asynchronously through Kafka so click tracking never blocks user redirects.

Highlights
Microservices backend with Java 21 and Spring Boot
Angular dashboard for URL management and analytics
Redis-first redirect resolution for low-latency hot-path reads
PostgreSQL as the source of truth
Kafka-based clickstream pipeline
Eventual consistency for analytics
Redis-backed rate limiting
TTL-aware URL expiration
Docker Compose setup for local development
Clean separation between URL creation, redirect resolution, and analytics processing
Architecture
                         +----------------------+
                         | Angular Dashboard    |
                         | URL + Analytics UI   |
                         +----------+-----------+
                                    |
                                    v
                         +----------------------+
                         | URL Service          |
                         | create/list URLs     |
                         +----+------------+----+
                              |            |
                 cache prime  |            | source of truth
                              v            v
                         +---------+   +----------------+
                         | Redis   |   | PostgreSQL     |
                         | cache + |   | URLs, events,  |
                         | limits  |   | aggregates     |
                         +----+----+   +--------+-------+
                              ^                 ^
                              | cache miss      |
       +----------+    +------+-------+         |
       | Browser  +--->| Redirect     |---------+
       | Clients  |    | Service      |
       +----------+    +------+-------+
                              |
                              | async click events
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
Detailed system design notes are in ARCHITECTURE.md.

Tech Stack
Layer	Technology
Frontend	Angular, Chart.js, Nginx
Backend	Java 21, Spring Boot, Spring Web, Spring Data JPA
Cache / Rate Limiting	Redis
Database	PostgreSQL
Streaming	Kafka
Runtime	Docker Compose
Services
URL Service
Responsible for creating and listing short URLs.

Validates and normalizes long URLs
Computes idempotency keys to avoid duplicate URL creation on retries
Generates compact Base62 short codes from Snowflake-style distributed IDs
Stores URL metadata in PostgreSQL
Primes Redis after creation
Applies Redis-backed request rate limiting
Key files:

UrlController.java
UrlCreationService.java
ShortUrl.java
Redirect Service
Responsible for resolving short URLs to original URLs.

Handles GET /{shortCode}
Applies IP-based Redis rate limiting
Reads Redis first for hot-path redirect lookup
Falls back to PostgreSQL on cache miss
Repopulates Redis after a successful database lookup
Publishes click events to Kafka asynchronously
Keeps redirects functional even if analytics is unavailable
Key files:

RedirectController.java
RedirectResolver.java
ClickEventPublisher.java
Analytics Service
Responsible for consuming click events and serving analytics.

Consumes Kafka topic url-click-events
Stores raw click events
Updates total click aggregates
Updates daily click aggregates
Exposes per-URL analytics APIs
Uses Kafka partition/offset idempotency to reduce duplicate processing risk
Key files:

ClickEventConsumer.java
AnalyticsIngestionService.java
AnalyticsController.java
Frontend
Angular dashboard for product and operational workflows.

Create short URLs
Copy/open generated short URLs
View active/expired URL status
Filter and sort generated URLs
Inspect per-URL analytics
View daily click trends
View recent clickstream events
See device, browser, and OS breakdowns
Key files:

analytics-dashboard.component.ts
url-shortener.component.ts
url-table.component.ts
styles.css
Repository Structure
.
├── ARCHITECTURE.md
├── README.md
├── docker-compose.yml
├── backend
│   ├── common
│   ├── url-service
│   ├── redirect-service
│   └── analytics-service
└── frontend
    └── src/app
        ├── core/api
        └── features
Running Locally
Prerequisites
Docker Desktop
Docker Compose
You do not need to install Java, Maven, Node, PostgreSQL, Redis, or Kafka locally if you run the Docker Compose setup.

Start the System
docker compose up --build
First startup can take several minutes because Docker needs to download images and build the Spring Boot and Angular applications.

Open the App
Dashboard: http://localhost:4200
URL Service health: http://localhost:8081/actuator/health
Redirect Service health: http://localhost:8082/actuator/health
Analytics Service health: http://localhost:8083/actuator/health
Stop the System
docker compose down
To stop the system and delete persisted local data:

docker compose down -v
API Reference
Create Short URL
curl -X POST http://localhost:8081/api/v1/urls \
  -H 'Content-Type: application/json' \
  -d '{
    "url": "https://example.com/product/42",
    "ownerId": "demo-user"
  }'
Example response:

{
  "id": "2f2a8bbd-8dc9-4bd5-a1f7-1d28f4df63ef",
  "shortCode": "7kM9xA",
  "shortUrl": "http://localhost:8082/7kM9xA",
  "originalUrl": "https://example.com/product/42",
  "expiresAt": null,
  "createdAt": "2026-06-07T05:00:00Z"
}
List URLs
curl "http://localhost:8081/api/v1/urls?ownerId=demo-user"
Redirect
curl -i http://localhost:8082/{shortCode}
Successful redirects return:

HTTP/1.1 302
Location: https://example.com/product/42
Get Per-URL Analytics
curl http://localhost:8083/api/v1/analytics/urls/{shortCode}
Get Daily Analytics
curl "http://localhost:8083/api/v1/analytics/urls/{shortCode}/daily?from=2026-06-01&to=2026-06-07"
Core Data Flow
URL Creation
User submits a long URL from the Angular dashboard.
URL Service validates and normalizes the URL.
Service computes an idempotency key.
If an existing record matches, it returns the existing short URL.
Otherwise it generates a distributed ID and Base62 short code.
PostgreSQL stores the URL metadata.
Redis is primed with the redirect mapping.
The short URL is returned to the client.
Redirect
Browser requests /{shortCode} from the Redirect Service.
Redirect Service applies Redis-backed IP rate limiting.
Redis is checked for url:{shortCode}.
On cache hit, the service returns a 302 redirect.
On cache miss, PostgreSQL is queried and Redis is repopulated.
A click event is published to Kafka asynchronously.
Redirect succeeds even if Kafka or Analytics Service is unavailable.
Analytics
Redirect Service emits a click event to Kafka.
Analytics Service consumes the event.
Raw click event is stored in PostgreSQL.
Total and daily aggregates are updated.
Dashboard queries Analytics Service for charts and clickstream data.
Database Model
Primary tables:

short_urls: source of truth for short URL metadata
click_events: raw clickstream events
url_analytics_totals: total click counters per URL
url_analytics_daily: daily trend aggregates
The design keeps raw events and aggregates. Raw events support auditing and future reprocessing, while aggregates make dashboard queries cheap.

Kafka Event
Topic:

url-click-events
Event shape:

{
  "eventId": "uuid",
  "shortUrlId": "uuid",
  "shortCode": "7kM9xA",
  "timestamp": "2026-06-07T05:00:00Z",
  "ipAddress": "203.0.113.10",
  "userAgent": "Mozilla/5.0 ...",
  "device": {
    "type": "desktop",
    "browser": "Chrome",
    "os": "macOS"
  },
  "geo": {
    "country": "ZZ",
    "city": null
  }
}
Design Decisions
Redis for Redirects
Redirects are the hottest path in a URL shortener. Redis keeps the common case fast and reduces PostgreSQL load. PostgreSQL remains authoritative, so Redis can be rebuilt if it is flushed or restarted.

Kafka for Analytics
Click analytics should not slow down redirects. Kafka decouples redirect traffic from analytics processing and absorbs bursts. This makes analytics eventually consistent, which is acceptable for dashboards.

Snowflake ID + Base62
Short code generation should work across multiple service instances. A Snowflake-style ID avoids central coordination, and Base62 encoding produces compact URL-safe codes.

Idempotent URL Creation
Retries should not create duplicate short URLs. URL Service computes a SHA-256 idempotency key from the normalized URL, owner, and expiration inputs.

Redis-Backed Rate Limiting
Rate limiting is stored in Redis so it works across horizontally scaled service instances. A local in-memory limiter would fail once multiple service replicas are running.

Scalability Notes
The system is designed around stateless services:

URL Service can scale horizontally for API writes.
Redirect Service can scale horizontally for high read traffic.
Analytics Service can scale with Kafka partitions.
Redis handles cache and shared rate limiting.
PostgreSQL remains the durable source of truth.
Potential production bottlenecks and mitigations:

Bottleneck	Mitigation
Redis hot keys for viral URLs	Redis Cluster, local near-cache, CDN/edge redirect cache
PostgreSQL aggregate write contention	Buffered aggregation, partitioned tables, batch updates
Kafka partition skew	Partition by event ID or use more partitions for high-cardinality traffic
Redis outage	PostgreSQL fallback with circuit breakers and request shedding
Analytics replay duplicates	Event IDs, partition/offset tracking, idempotent upserts
Production Hardening Ideas
This repository is a strong local system design implementation. For production, the next upgrades would be:

API gateway or ingress controller
Authentication and multi-tenant ownership model
Database migrations with Flyway or Liquibase
OpenTelemetry tracing across services
Prometheus/Grafana dashboards
Dead-letter topic for failed analytics events
Outbox pattern for guaranteed event publication
Redis Lua script for fully atomic sliding-window rate limiting
GeoIP provider integration
Kubernetes manifests or Helm chart
CI pipeline for backend, frontend, and Docker builds
Interview Talking Points
Use this framing:

LinkForge separates URL creation, redirect resolution, and analytics into independent services. Redirects are optimized with Redis-first lookup and PostgreSQL fallback. Analytics is asynchronous through Kafka, so click tracking never blocks redirects. PostgreSQL is the source of truth, Redis handles hot-path reads and shared rate limiting, and Kafka provides buffering and decoupling for clickstream processing.

Strong keywords to mention:

Cache-aside
Write-through cache priming
Eventual consistency
Hot-path optimization
Horizontal scalability
Idempotency
Backpressure
Fault isolation
Distributed ID generation
Source of truth
Development Commands
Build only the frontend:

docker compose build frontend
Restart only the frontend:

docker compose up -d frontend
Build one backend service:

docker compose build url-service
docker compose build redirect-service
docker compose build analytics-service
View logs:

docker compose logs -f
View one service:

docker compose logs -f redirect-service
License
This project is intended as a portfolio and system design reference implementation.
