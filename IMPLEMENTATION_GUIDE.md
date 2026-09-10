# SwiftPay - Implementation Guide

## 📌 Project Structure

```
swiftpay/
├── pom.xml                          # Parent POM (multi-module Maven project)
├── docker-compose.yml               # Local infrastructure setup
├── init-db.sql                      # Database initialization
├── README.md                        # Quick start guide
├── ARCHITECTURE.md                  # Architecture documentation
├── IMPLEMENTATION_GUIDE.md          # This file
│
├── common/                          # Shared domain models & events
│   ├── pom.xml
│   └── src/main/java/com/swiftpay/common/
│       ├── domain/                  # Enums, constants
│       ├── events/                  # PaymentInitiatedEvent, etc.
│       ├── dto/                     # PaymentRequestDto, ApiResponse
│       └── exception/               # SwiftPayException, InsufficientFundsException
│
├── transaction-gateway/             # Service A - REST API
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/swiftpay/gateway/
│       ├── TransactionGatewayApplication.java
│       ├── controller/              # PaymentController, HealthController
│       ├── service/                 # PaymentService, IdempotencyService
│       ├── entity/                  # Transaction entity
│       ├── repository/              # TransactionRepository
│       ├── kafka/                   # PaymentEventProducer
│       ├── config/                  # KafkaProducerConfig, RedisConfig
│       └── resources/application.yml
│
├── ledger-service/                  # Service B - Payment Processor
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/swiftpay/ledger/
│       ├── LedgerServiceApplication.java
│       ├── controller/              # LedgerController, HealthController
│       ├── service/                 # LedgerService
│       ├── entity/                  # Account, LedgerEntry
│       ├── repository/              # AccountRepository, LedgerEntryRepository
│       ├── kafka/                   # PaymentEventListener, PaymentEventPublisher
│       ├── config/                  # KafkaConsumerConfig, KafkaProducerConfig
│       └── resources/application.yml
│
├── analytics-worker/                # Service C - Analytics
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/swiftpay/analytics/
│       ├── AnalyticsWorkerApplication.java
│       ├── controller/              # AnalyticsController, HealthController
│       ├── service/                 # AnalyticsService
│       ├── entity/                  # PaymentAnalytics
│       ├── repository/              # PaymentAnalyticsRepository
│       ├── kafka/                   # PaymentCompletedEventListener
│       ├── config/                  # KafkaConsumerConfig
│       ├── dto/                     # AnalyticsMetrics
│       └── resources/application.yml
│
└── .github/workflows/
    └── build-and-test.yml           # GitHub Actions CI/CD
```

## 🛠️ Build Instructions

### 1. Prerequisites
```bash
# Required versions
Java 21 JDK
Maven 3.8+
Docker 20.10+
Docker Compose 1.29+
Git
```

### 2. Clone Repository
```bash
git clone <repository-url>
cd swiftpay
```

### 3. Build Entire Project
```bash
# Option 1: Full build with all modules
mvn clean package

# Option 2: Build specific module
mvn clean package -pl transaction-gateway

# Option 3: Skip tests for faster build
mvn clean package -DskipTests

# Option 4: Run tests separately
mvn test
mvn verify
```

### 4. Build Docker Images
```bash
# Build all services
docker-compose build

# Build specific service
docker-compose build transaction-gateway
```

### Build Output
```
transaction-gateway/target/transaction-gateway-1.0.0.jar
ledger-service/target/ledger-service-1.0.0.jar
analytics-worker/target/analytics-worker-1.0.0.jar
```

## 🚀 Running Locally

### Option 1: Using Provided Scripts
```bash
# Make scripts executable
chmod +x build.sh start.sh test-api.sh

# Build and run
./build.sh
./start.sh

# Test APIs
./test-api.sh

# Stop
docker-compose down
```

### Option 2: Manual Start
```bash
# Build
mvn clean package -DskipTests

# Start infrastructure
docker-compose up -d

# Monitor startup (wait 30-45 seconds)
docker-compose logs -f

# Check health
curl http://localhost:8080/health
curl http://localhost:8081/health
curl http://localhost:8082/health
```

### Option 3: IntelliJ IDEA
```
1. Open project: File → Open → Select swiftpay directory
2. Maven: Right-click pom.xml → Maven → Reload Project
3. Run service: Right-click main class → Run
   - TransactionGatewayApplication
   - LedgerServiceApplication
   - AnalyticsWorkerApplication
4. Set environment variables in Run Configuration:
   SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/swiftpay
   SPRING_DATASOURCE_USERNAME=swiftpay
   SPRING_DATASOURCE_PASSWORD=swiftpay_password
   SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
   SPRING_REDIS_HOST=localhost
   SPRING_REDIS_PORT=6379
```

## 🧪 Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify
```

### Test Coverage
```bash
mvn test jacoco:report
# Report: target/site/jacoco/index.html
```

### Manual API Testing

**1. Start services**
```bash
docker-compose up -d
sleep 30
```

**2. Create payment**
```bash
curl -X POST http://localhost:8080/v1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "transaction_id": "txn-2024-001",
    "sender_id": "user001",
    "receiver_id": "user002",
    "amount": 100.50,
    "currency": "USD"
  }'
```

**3. Check balance**
```bash
curl http://localhost:8081/v1/ledger/account/user001
```

**4. View Swagger UI**
- http://localhost:8080/swagger-ui.html
- http://localhost:8081/swagger-ui.html
- http://localhost:8082/swagger-ui.html

### Load Testing

**Install k6:**
```bash
# macOS
brew install k6

# Linux
sudo apt-get install k6

# Windows (via Chocolatey)
choco install k6
```

**Create load-test.js:**
```javascript
import http from 'k6/http';
import { check } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const errorRate = new Rate('errors');
const duration = new Trend('request_duration');

export const options = {
  vus: 250,                              // Virtual Users (250 TPS)
  duration: '4166s',                     // ~1M transactions
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
    'errors': ['rate<0.1'],
  },
};

export default function() {
  const txnId = `txn-${Date.now()}-${Math.random()}`;
  const senderId = `user${Math.floor(Math.random() * 1000)}`;
  const receiverId = `user${Math.floor(Math.random() * 1000)}`;
  
  // Avoid self-transfers
  if (senderId === receiverId) return;

  const payload = JSON.stringify({
    transaction_id: txnId,
    sender_id: senderId,
    receiver_id: receiverId,
    amount: Math.random() * 1000,
    currency: 'USD',
  });

  const res = http.post('http://localhost:8080/v1/payments', payload, {
    headers: { 'Content-Type': 'application/json' },
  });

  const isSuccess = res.status === 202 || res.status === 409; // 409 = duplicate
  errorRate.add(!isSuccess);
  duration.add(res.timings.duration);

  check(res, {
    'status is 202/409': (r) => r.status === 202 || r.status === 409,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });
}
```

**Run load test:**
```bash
k6 run load-test.js --out json=results.json

# View results
k6 stats results.json
```

## 🔍 Troubleshooting

### Services Not Starting

**Check service health:**
```bash
docker-compose ps
docker-compose logs transaction-gateway
docker-compose logs ledger-service
docker-compose logs analytics-worker
```

**Check PostgreSQL:**
```bash
docker exec swiftpay-postgres psql -U swiftpay -d swiftpay \
  -c "SELECT COUNT(*) FROM transactions;"
```

**Check Redis:**
```bash
docker exec swiftpay-redis redis-cli PING
docker exec swiftpay-redis redis-cli KEYS "*"
```

**Check Kafka:**
```bash
docker exec swiftpay-kafka kafka-broker-api-versions.sh \
  --bootstrap-server kafka:29092
```

### Payment Not Processing

**1. Check transaction status**
```bash
curl http://localhost:8080/v1/payments/{transactionId}
```

**2. Check Kafka topics**
```bash
docker exec swiftpay-kafka kafka-topics.sh \
  --bootstrap-server kafka:29092 \
  --list
```

**3. Check consumer lag**
```bash
docker exec swiftpay-kafka kafka-consumer-groups.sh \
  --bootstrap-server kafka:29092 \
  --group ledger-service-group \
  --describe
```

### High Memory Usage

**Increase JVM heap:**
```yaml
# docker-compose.yml
environment:
  JAVA_OPTS: -Xmx1g -Xms512m
```

### Timeout Issues

**Increase connection timeout:**
```yaml
# transaction-gateway/src/main/resources/application.yml
spring:
  datasource:
    hikari:
      connection-timeout: 60000
```

## 📦 Configuration

### Environment Variables

**PostgreSQL:**
```
POSTGRES_DB=swiftpay
POSTGRES_USER=swiftpay
POSTGRES_PASSWORD=swiftpay_password
```

**Kafka:**
```
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092
```

**Redis:**
```
SPRING_REDIS_HOST=redis
SPRING_REDIS_PORT=6379
```

**JVM:**
```
JAVA_OPTS=-Xmx512m -Xms256m
```

### Application Properties

**transaction-gateway/src/main/resources/application.yml:**
- Server port: 8080
- Kafka topic: payment-initiated
- Redis TTL: 24 hours
- Max connections: 20

**ledger-service/src/main/resources/application.yml:**
- Server port: 8081
- Consumer group: ledger-service-group
- Kafka topics: payment-initiated, payment-completed, payment-failed

**analytics-worker/src/main/resources/application.yml:**
- Server port: 8082
- Consumer group: analytics-worker-group
- Kafka topic: payment-completed

## 📊 Monitoring

### Application Metrics
```bash
# Prometheus metrics
curl http://localhost:8080/actuator/metrics

# Specific metric
curl http://localhost:8080/actuator/metrics/http.server.requests
```

### Database Metrics
```bash
docker exec swiftpay-postgres psql -U swiftpay -d swiftpay -c "
  SELECT
    schemaname,
    tablename,
    idx_scan as index_scans,
    idx_tup_read as tuples_read,
    idx_tup_fetch as tuples_fetched
  FROM pg_stat_user_indexes
  ORDER BY idx_scan DESC;
"
```

### Kafka Metrics
```bash
docker exec swiftpay-kafka kafka-consumer-groups.sh \
  --bootstrap-server kafka:29092 \
  --group ledger-service-group \
  --describe
```

## 🚢 Deployment

### Docker Compose Production Checklist
- [ ] Update database passwords
- [ ] Set resource limits (memory, CPU)
- [ ] Enable persistence volumes
- [ ] Configure logging driver
- [ ] Set up backup strategy
- [ ] Enable health checks
- [ ] Use restart policies

### Kubernetes Deployment (Future)
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: transaction-gateway
spec:
  replicas: 3
  selector:
    matchLabels:
      app: transaction-gateway
  template:
    metadata:
      labels:
        app: transaction-gateway
    spec:
      containers:
      - name: transaction-gateway
        image: swiftpay/transaction-gateway:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_DATASOURCE_URL
          value: jdbc:postgresql://postgres:5432/swiftpay
        livenessProbe:
          httpGet:
            path: /health/live
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /health/ready
            port: 8080
          initialDelaySeconds: 20
          periodSeconds: 5
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
```

## 📚 Documentation

- **README.md** - Quick start & API overview
- **ARCHITECTURE.md** - System design & data flow
- **IMPLEMENTATION_GUIDE.md** - This file
- **Code comments** - Inline documentation

## 🔐 Security Checklist

- [ ] Database credentials rotated
- [ ] Redis password configured (if needed)
- [ ] Kafka SSL/TLS enabled (production)
- [ ] API rate limiting configured
- [ ] Request input validation
- [ ] CORS policies configured
- [ ] Sensitive data not logged
- [ ] SQL injection prevention (JPA)
- [ ] CSRF protection (if applicable)

## ⚡ Performance Tuning Checklist

- [ ] Connection pool sizes optimized
- [ ] Kafka batch size tuned
- [ ] Database indexes verified
- [ ] Query execution plans reviewed
- [ ] JVM heap sizing optimized
- [ ] Redis cache hit ratio > 80%
- [ ] P95 latency < 500ms
- [ ] P99 latency < 1000ms

## 🎯 Next Steps

1. **Build & Deploy**
   - Run `./start.sh`
   - Verify all services healthy
   - Run `./test-api.sh`

2. **Performance Tuning**
   - Run load test with k6
   - Monitor metrics via actuator
   - Identify bottlenecks
   - Optimize configuration

3. **Production Deployment**
   - Set up Kubernetes cluster
   - Configure persistent storage
   - Set up monitoring/alerting
   - Create backup strategy
   - Document runbooks

4. **Continuous Improvement**
   - Monitor production metrics
   - Analyze error logs
   - Plan optimizations
   - Community feedback

---

**Version:** 1.0.0  
**Last Updated:** 2024-01-15  
**Maintainer:** SwiftPay Engineering Team
