# LinkForge

Production-style distributed URL shortener and analytics platform.

## Architecture

Read [ARCHITECTURE.md](/Users/sandynambiar/Documents/LinkForge/ARCHITECTURE.md) for the required design output: architecture diagram, service responsibilities, schemas, Kafka event contract, REST APIs, package structure, frontend structure, Docker Compose, tradeoffs, and data flows.

## Run Locally

```bash
docker compose up --build
```

Then open:

- Dashboard: http://localhost:4200
- URL Service: http://localhost:8081/actuator/health
- Redirect Service: http://localhost:8082/actuator/health
- Analytics Service: http://localhost:8083/actuator/health

## Example API Calls

```bash
curl -X POST http://localhost:8081/api/v1/urls \
  -H 'Content-Type: application/json' \
  -d '{"url":"https://example.com/product/42","ownerId":"demo-user"}'
```

```bash
curl -i http://localhost:8082/{shortCode}
```

```bash
curl http://localhost:8083/api/v1/analytics/urls/{shortCode}
```
