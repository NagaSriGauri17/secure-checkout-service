# Secure Checkout Service

A **fault-tolerant, production-grade checkout backend** built with Spring Boot and PostgreSQL.

Core engineering goals:
- **Idempotent payment APIs** — clients can safely retry without causing duplicate charges
- **State-machine-driven order lifecycle** — every transition is validated; illegal states are rejected
- **Webhook delivery** — async status notifications with exponential back-off retry and persistence
- **Circuit breaker + retry** — automatically absorbs transient gateway failures via Resilience4j
- **Full observability** — structured logging with correlation IDs, Prometheus metrics, Grafana dashboards

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    REST Clients                         │
└───────────────┬─────────────────────────────────────────┘
                │ HTTP
┌───────────────▼─────────────────────────────────────────┐
│            OrderController  /api/v1/orders              │
│  POST /          GET /:id       PATCH /:id/status       │
│  POST /:id/pay                                          │
└───────────────┬─────────────────────────────────────────┘
                │
┌───────────────▼─────────────────────────────────────────┐
│                    OrderService                         │
│  • State machine enforcement                            │
│  • Idempotency check (DB lookup before gateway call)    │
│  • Metrics recording                                    │
└──────┬──────────────────────────┬───────────────────────┘
       │                          │
┌──────▼──────────┐    ┌──────────▼───────────────────────┐
│ PaymentGateway  │    │       WebhookService              │
│   Service       │    │ • Persists events to DB           │
│ @CircuitBreaker │    │ • Scheduled retry (10s poll)      │
│ @Retry          │    │ • Exponential back-off            │
└──────┬──────────┘    └──────────────────────────────────┘
       │
  (Mock gateway:
   80% success,
   20% transient
   failure → retried)

PostgreSQL tables: orders · payments · webhook_events
```

## Order State Machine

```
CREATED ──────────────────────────────────► CANCELLED
   │
   ▼
PENDING_PAYMENT ──► PAYMENT_FAILED ──► PENDING_PAYMENT (retry)
   │                      │
   │                      └──────────────────────────► CANCELLED
   ▼
  PAID ──────────────────────────────────────────────► CANCELLED
   │
   ▼
PROCESSING
   │
   ▼
SHIPPED
   │
   ▼
DELIVERED  ◄─── terminal
```

---

## Running Locally

### Prerequisites
- Java 17+, Maven 3.9+
- Docker and Docker Compose

### 1. Start all services (app + PostgreSQL + Prometheus + Grafana)
```bash
docker compose up -d
```

### 2. Verify health
```bash
curl http://localhost:8080/actuator/health
```

### 3. Open Swagger UI
```
http://localhost:8080/swagger-ui.html
```

### 4. Open Grafana dashboards
```
http://localhost:3000   (admin / admin)
```

---

## API Walkthrough

### Create an order
```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "cust_abc123",
    "totalAmount": 1299.99,
    "currency": "INR",
    "webhookUrl": "https://webhook.site/your-token"
  }'
```

Response:
```json
{
  "id": "ord_a1b2c3d4...",
  "status": "CREATED",
  "totalAmount": 1299.99,
  "currency": "INR"
}
```

### Pay (with idempotency key)
```bash
curl -X POST http://localhost:8080/api/v1/orders/ord_a1b2c3d4.../pay \
  -H "Content-Type: application/json" \
  -d '{
    "idempotencyKey": "idem_unique_per_attempt_001",
    "paymentMethodToken": "tok_visa_4242"
  }'
```

**Run this twice with the same `idempotencyKey`** — the second call returns `"idempotentReplay": true` and does NOT charge again.

### Simulate a hard decline
```bash
-d '{ "idempotencyKey": "idem_002", "paymentMethodToken": "tok_fail_4000" }'
```

### Advance order through fulfilment
```bash
# After payment succeeds
curl -X PATCH "http://localhost:8080/api/v1/orders/ord_.../status?status=PROCESSING"
curl -X PATCH "http://localhost:8080/api/v1/orders/ord_.../status?status=SHIPPED"
curl -X PATCH "http://localhost:8080/api/v1/orders/ord_.../status?status=DELIVERED"
```

---

## Key Engineering Decisions

### Idempotency
Before calling the payment gateway, the service performs a database lookup on `idempotencyKey` (unique-indexed column). If a record exists, the stored result is returned immediately — the gateway is never called a second time. This prevents double charges during client retries after network timeouts.

### State Machine
`OrderStatus` is an enum where each state declares its own valid next states. The `Order.transitionTo()` method checks this at runtime. Invalid transitions throw `IllegalStateException`, which the global handler maps to HTTP 409 Conflict.

### Webhook Reliability
Webhook events are written to the `webhook_events` table inside the payment transaction. A `@Scheduled` poller picks them up every 10 seconds and POSTs to the target URL. On delivery failure, the event is re-queued with exponential back-off (2s → 4s → 8s). After `maxAttempts` the event is marked `EXHAUSTED` for manual inspection.

### Circuit Breaker
Resilience4j wraps the gateway call. After 50% failures in a 10-request window, the circuit opens and `gatewayFallback()` is called immediately — no waiting for gateway timeouts. This protects the service during gateway outages.

---

## Running Tests
```bash
mvn test
```

Tests cover:
- All valid and invalid state machine transitions (parameterized)
- Idempotency — second call with same key skips gateway
- Payment rejection on invalid order state

---

## Observability

| Signal | Tool | Endpoint |
|--------|------|----------|
| Metrics | Prometheus | `/actuator/prometheus` |
| Health | Spring Actuator | `/actuator/health` |
| Logs | Console (structured JSON) | stdout |
| API docs | Swagger UI | `/swagger-ui.html` |

Key custom metrics:
- `checkout.orders.created` — counter
- `checkout.payments.success` — counter
- `checkout.payments.failed` — counter
- `checkout.payments.idempotent_replays` — counter
- `checkout.payment.latency` — timer (p50, p95, p99)

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Framework | Spring Boot 3.2 |
| Database | PostgreSQL 16 |
| Resilience | Resilience4j (circuit breaker + retry) |
| HTTP client | Spring WebFlux WebClient |
| API docs | SpringDoc OpenAPI 3 |
| Metrics | Micrometer + Prometheus |
| Dashboards | Grafana |
| Build | Maven |
| Container | Docker (multi-stage build) |
| CI/CD | GitHub Actions |
