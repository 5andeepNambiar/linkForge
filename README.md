# LinkForge - Distributed URL Shortener with Analytics

A production-grade distributed URL shortening platform built with Java, Spring Boot, Kafka, Redis, PostgreSQL, Angular, and Docker.

LinkForge is designed to simulate the architecture and engineering challenges behind large-scale URL shortening systems such as Bitly and TinyURL. The platform focuses on low-latency redirects, event-driven analytics processing, distributed system design, and scalability.

---

## Features

* Collision-resistant Base62 short URL generation
* High-performance URL redirection
* Redis-powered caching layer
* Kafka-driven asynchronous analytics pipeline
* URL expiration support
* Rate limiting using Redis
* Real-time click analytics dashboard
* Device and browser tracking
* Dockerized microservices architecture
* Horizontally scalable design

---

## Architecture Overview

```text
                    +----------------------+
                    | Angular Dashboard    |
                    +----------+-----------+
                               |
                               v
                    +----------------------+
                    | URL Service          |
                    +----+------------+----+
                         |            |
                         v            v
                    +---------+   +----------------+
                    | Redis   |   | PostgreSQL     |
                    +----+----+   +--------+-------+
                         ^                 ^
                         |                 |
+----------+      +------+-------+         |
| Clients  +----->| Redirect     |---------+
+----------+      | Service      |
                  +------+-------+
                         |
                         v
                  +--------------+
                  | Kafka        |
                  | Click Events |
                  +------+-------+
                         |
                         v
                  +--------------+
                  | Analytics    |
                  | Service      |
                  +--------------+
```

---

## Tech Stack

### Backend

* Java 21
* Spring Boot
* Spring Data JPA
* Spring Kafka
* Spring Security

### Frontend

* Angular
* TypeScript
* Chart.js

### Infrastructure

* Redis
* PostgreSQL
* Apache Kafka
* Docker
* Docker Compose

---

## System Design Highlights

### URL Creation

1. User submits a long URL
2. Service generates a distributed unique ID
3. ID is encoded using Base62
4. Metadata is stored in PostgreSQL
5. Redis cache is populated

### Redirect Flow

1. Request arrives for short URL
2. Redis lookup performed
3. Cache hit → redirect immediately
4. Cache miss → fetch from PostgreSQL
5. Analytics event published to Kafka

### Analytics Pipeline

1. Redirect service emits click event
2. Kafka buffers traffic spikes
3. Analytics service consumes events
4. Aggregates stored in PostgreSQL
5. Dashboard visualizes metrics

---

## Engineering Challenges Solved

### Low-Latency Redirects

Redis acts as the primary read path, minimizing database access and reducing redirect latency.

### Event-Driven Analytics

Kafka decouples analytics processing from user-facing requests, ensuring redirects remain fast even under heavy load.

### Collision-Resistant Short Codes

Distributed IDs combined with Base62 encoding ensure scalable and unique URL generation.

### Scalability

The architecture supports horizontal scaling of services independently:

* URL Service
* Redirect Service
* Analytics Service

---

## Database Schema

### Short URLs

```sql
short_urls
├── id
├── short_code
├── original_url
├── expires_at
├── active
└── created_at
```

### Analytics

```sql
click_events
├── event_id
├── short_url_id
├── occurred_at
├── ip_address
└── user_agent
```

---

## API Endpoints

### Create Short URL

```http
POST /api/v1/urls
```

### Redirect

```http
GET /{shortCode}
```

### Analytics

```http
GET /api/v1/analytics/urls/{shortCode}
```

---

## Local Development

### Start Infrastructure

```bash
docker-compose up -d
```

### Start Backend Services

```bash
./mvnw spring-boot:run
```

### Start Angular Frontend

```bash
npm install
ng serve
```

---

## Future Improvements

* Custom domains
* QR code generation
* Multi-region deployment
* Redis Cluster
* Kafka Streams analytics
* Prometheus and Grafana monitoring
* Kubernetes deploymenet
