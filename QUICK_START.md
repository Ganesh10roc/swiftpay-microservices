# SwiftPay - Quick Start Guide

## Prerequisites
- Docker & Docker Compose
- Java 21 (optional - Docker includes it)
- curl or Postman for API testing

## Run SwiftPay (5 minutes)

```bash
# Clone repository
git clone https://github.com/Ganesh10roc/swiftpay-microservices.git
cd swiftpay-microservices

# Start all services
docker-compose up -d

# Wait for services to be healthy (30-60 seconds)
docker-compose ps

# Test payment endpoint
curl -X POST http://localhost:8080/v1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "sender_id": "user1",
    "receiver_id": "user2",
    "amount": 100.00,
    "currency": "USD"
  }'

# View results
curl http://localhost:8080/health
curl http://localhost:8081/health
curl http://localhost:8082/health
```

## API Endpoints

### Transaction Gateway (8080)
```
POST /v1/payments           - Initiate payment
GET  /v1/transactions/{id}  - Get transaction status
GET  /health                - Health check
```

### Ledger Service (8081)
```
GET  /v1/accounts/{id}     - Get account balance
GET  /v1/transactions      - Transaction history
GET  /health               - Health check
```

### Analytics Worker (8082)
```
GET  /v1/analytics/summary - Payment analytics
GET  /health               - Health check
```

## Load Testing

```bash
# Run 1M transaction load test
python generate-submission.py

# Or with K6
docker run --rm -v /path/to/swiftpay:/app grafana/k6:latest \
  run /app/load-test.js
```

## Stop Services
```bash
docker-compose down
```

## Architecture
- Transaction Gateway: Payment initiation & idempotency
- Ledger Service: Atomic accounting operations
- Analytics Worker: Real-time payment analytics
- Kafka: Event streaming
- PostgreSQL: Persistent storage
- Redis: Caching & idempotency

See ARCHITECTURE.md for detailed design.
