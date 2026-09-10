# SwiftPay - Real-Time Payment Ledger

A resilient, scalable fintech platform for peer-to-peer (P2P) money transfers with real-time processing, atomic transactions, and comprehensive audit logging.

## 🏗️ Architecture

### Microservices Overview

**Service A: Transaction Gateway** (Port 8080)
- REST API for payment initiation
- Redis-based idempotency (24-hour TTL)
- Sender balance validation
- Kafka event publishing
- Swagger/OpenAPI documentation

**Service B: Ledger Service** (Port 8081)
- Event-driven Kafka consumer
- Atomic debit/credit operations with pessimistic locking
- Real-time balance updates
- Transaction history reporting
- Automatic account creation with initial balance

**Service C: Analytics Worker** (Port 8082, Bonus)
- Real-time OLAP integration
- Payment completion event consumption
- Transaction metrics and volume monitoring
- Historical analytics queries

## 🛠️ Technology Stack

- **Language:** Java 21
- **Framework:** Spring Boot 3.3.0
- **Database:** PostgreSQL 16
- **Message Broker:** Apache Kafka 7.5.0 + Zookeeper
- **Caching:** Redis 7
- **API Documentation:** OpenAPI/Swagger
- **Testing:** JUnit 5, Testcontainers
- **CI/CD:** GitHub Actions
- **Containerization:** Docker & Docker Compose

## 📋 Prerequisites

- Docker & Docker Compose (v1.29+)
- Maven 3.8+
- Java 21 JDK
- Git

## 🚀 Quick Start

### 1. Clone Repository
```bash
git clone <repo-url>
cd swiftpay
```

### 2. Build All Services
```bash
mvn clean package
```

### 3. Start Infrastructure
```bash
docker-compose up -d
```

### 4. Verify Services
```bash
# Wait ~30 seconds for services to start
curl http://localhost:8080/health
curl http://localhost:8081/health
curl http://localhost:8082/health
```

### 5. Access APIs
- **Transaction Gateway Swagger:** http://localhost:8080/swagger-ui.html
- **Ledger Service Swagger:** http://localhost:8081/swagger-ui.html
- **Analytics Swagger:** http://localhost:8082/swagger-ui.html

## 📡 API Endpoints

### Transaction Gateway (Service A)

#### Initiate Payment
```bash
POST /v1/payments
Content-Type: application/json

{
  "transaction_id": "uuid-string",
  "sender_id": "user001",
  "receiver_id": "user002",
  "amount": 100.50,
  "currency": "USD"
}

Response: 202 Accepted
{
  "status": "SUCCESS",
  "message": "Payment initiated successfully",
  "data": {
    "id": 1,
    "transactionId": "uuid-string",
    "status": "PENDING",
    ...
  }
}
```

#### Get Transaction
```bash
GET /v1/payments/{transactionId}
Response: 200 OK
```

#### Get Transaction History
```bash
GET /v1/payments/history/{userId}?page=0&size=20
Response: 200 OK
```

### Ledger Service (Service B)

#### Get Ledger History
```bash
GET /v1/ledger/history/{userId}?page=0&size=20
Response: 200 OK
```

#### Get Account Details
```bash
GET /v1/ledger/account/{userId}
Response: 200 OK
{
  "status": "SUCCESS",
  "data": {
    "userId": "user001",
    "balance": 9900.00,
    "currency": "USD"
  }
}
```

### Analytics Worker (Service C)

#### Get Last Hour Metrics
```bash
GET /v1/analytics/metrics/hour
Response: 200 OK
{
  "status": "SUCCESS",
  "data": {
    "transactionCount": 150,
    "totalAmount": 15000.00,
    "averageAmount": 100.00,
    "uniqueSenderCount": 45
  }
}
```

#### Get Last Day Metrics
```bash
GET /v1/analytics/metrics/day
Response: 200 OK
```

#### Get Custom Period Metrics
```bash
GET /v1/analytics/metrics?startTime=2024-01-01T00:00:00&endTime=2024-01-02T00:00:00
Response: 200 OK
```

## 🔄 Payment Flow

```
1. Client POST /v1/payments
   ↓
2. Transaction Gateway
   - Validate idempotency key (Redis)
   - Save transaction as PENDING
   - Publish PaymentInitiatedEvent → Kafka
   ↓
3. Ledger Service (Consumer)
   - Receive PaymentInitiatedEvent
   - Lock sender & receiver accounts
   - Validate balance
   - Debit sender, Credit receiver
   - Record ledger entries
   - Publish PaymentCompletedEvent → Kafka
   ↓
4. Analytics Worker (Consumer)
   - Receive PaymentCompletedEvent
   - Insert into analytics table
   - Metrics available via /v1/analytics endpoints
```

## 🔒 Error Handling

### Duplicate Transaction (Idempotency)
```json
{
  "status": "ERROR",
  "message": "Transaction already exists: ...",
  "error": {
    "code": "DUPLICATE_TRANSACTION",
    "message": "..."
  }
}
```
Response: 409 Conflict

### Insufficient Funds
Ledger Service publishes `PaymentFailedEvent` with reason.
Transaction Gateway can query to update status.
Response: 202 Accepted (initial)
Error handling at Ledger level.

### Database Constraint Violation
Spring transaction rollback with retry mechanism via Kafka retry topics.

### Kafka Outage
Retry mechanism enabled:
- 4 attempts for Transaction Gateway → Ledger
- 3 attempts for Ledger → Analytics
- Exponential backoff with jitter

## 📊 Database Schema

### Transactions Table (Service A)
- `transaction_id` (UUID, unique)
- `sender_id`, `receiver_id`
- `amount`, `currency`
- `status` (PENDING/COMPLETED/FAILED)
- Timestamps & error reasons

### Accounts Table (Service B)
- `user_id` (unique)
- `balance` (with optimistic versioning)
- Timestamps

### Ledger Entries Table (Service B)
- `transaction_id` reference
- `user_id`, `account_id`
- `debit`/`credit` amounts
- `balance_after`
- Status tracking

### Payment Analytics Table (Service C)
- `transaction_id` (unique)
- `sender_id`, `receiver_id`
- `amount`, `currency`
- `completed_at`, `ingested_at`

## 🧪 Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify
```

### Load Testing (250 TPS for 1M transactions)

Install k6:
```bash
# macOS
brew install k6

# Ubuntu/Debian
sudo apt-get install k6
```

Create load test script:
```javascript
// load-test.js
import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus: 250,
  duration: '4166s', // ~1M transactions at 250 TPS
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
    http_req_failed: ['rate<0.1'],
  },
};

export default function () {
  const url = 'http://localhost:8080/v1/payments';
  const payload = JSON.stringify({
    transaction_id: `txn-${Date.now()}-${Math.random()}`,
    sender_id: `user${Math.floor(Math.random() * 1000)}`,
    receiver_id: `user${Math.floor(Math.random() * 1000)}`,
    amount: Math.random() * 1000,
    currency: 'USD',
  });

  const res = http.post(url, payload, {
    headers: { 'Content-Type': 'application/json' },
  });

  check(res, {
    'status is 202': (r) => r.status === 202,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });
}
```

Run load test:
```bash
k6 run load-test.js
```

## 📈 Performance Tuning

### Connection Pooling
- PostgreSQL: 20 max connections per service
- Redis: Jedis pool with 8 connections

### Kafka Optimization
- Batch processing: 20 inserts
- Compression: Snappy
- Partitions: 3 per topic
- Replication: 1 (local only)

### Caching Strategy
- Redis TTL: 24 hours for idempotency
- Pessimistic locking for account updates
- Read-only transaction support for reports

## 🐛 Troubleshooting

### Services Not Starting
```bash
# Check logs
docker-compose logs -f transaction-gateway
docker-compose logs -f ledger-service
docker-compose logs -f analytics-worker

# Verify connectivity
docker exec swiftpay-postgres psql -U swiftpay -d swiftpay -c "SELECT 1"
docker exec swiftpay-redis redis-cli ping
docker exec swiftpay-kafka kafka-broker-api-versions.sh --bootstrap-server kafka:29092
```

### High Transaction Latency
1. Check database index usage
2. Monitor Kafka consumer lag
3. Review Redis connection pool
4. Check network bandwidth

### Out of Memory
Adjust JVM heap in docker-compose.yml:
```yaml
JAVA_OPTS: -Xmx1g -Xms512m
```

## 📝 Logging & Observability

### Application Logs
```bash
docker-compose logs -f [service-name]
```

### Health Checks
- `/health` - Basic status
- `/health/live` - Liveness probe
- `/health/ready` - Readiness probe
- `/actuator/metrics` - Prometheus metrics

### Log Levels
```yaml
logging:
  level:
    com.swiftpay: DEBUG
    org.springframework.kafka: INFO
```

## 🔐 Security

- Pessimistic locking prevents race conditions
- Transaction isolation level: READ_COMMITTED
- Input validation on all API endpoints
- Idempotency key enforcement (24-hour)
- No sensitive data in logs

## 📦 Deployment

### Kubernetes Deployment (Future)
```yaml
# Deployment manifests in ./k8s/
kubectl apply -f k8s/
```

### Environment Variables
```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/swiftpay
SPRING_DATASOURCE_USERNAME=swiftpay
SPRING_DATASOURCE_PASSWORD=swiftpay_password
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092
SPRING_REDIS_HOST=redis
SPRING_REDIS_PORT=6379
```

## 🛑 Stopping Services

```bash
docker-compose down

# Remove volumes (careful!)
docker-compose down -v
```

## 📚 Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Kafka Documentation](https://kafka.apache.org/documentation/)
- [PostgreSQL Tuning](https://wiki.postgresql.org/wiki/Performance_Optimization)
- [Redis Best Practices](https://redis.io/docs/)

## 🤝 Contributing

1. Create feature branch: `git checkout -b feature/your-feature`
2. Commit changes: `git commit -am 'Add feature'`
3. Push to branch: `git push origin feature/your-feature`
4. Create Pull Request

## 📄 License

MIT License - See LICENSE file for details

## 👥 Support

For issues or questions:
- Email: support@swiftpay.com
- GitHub Issues: [Create an issue](../../issues)

---

**Version:** 1.0.0  
**Last Updated:** 2024-01-15  
**Built with ❤️ by SwiftPay Team**
