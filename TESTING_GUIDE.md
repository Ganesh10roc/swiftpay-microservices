# SwiftPay - Testing Guide

Complete testing strategy for SwiftPay payment system including unit tests, integration tests, API tests, and load tests.

## 📋 Test Overview

| Test Type | Status | Command | Duration |
|-----------|--------|---------|----------|
| **Unit Tests** | ✅ Ready | `mvn test` | ~2 minutes |
| **Integration Tests** | ✅ Ready | `mvn verify` | ~5 minutes |
| **API Tests** | ✅ Ready | `./test-api.sh` | ~1 minute |
| **All Tests** | ✅ Ready | `./run-all-tests.sh` | ~30 minutes |
| **Load Test** | ✅ Ready | `./run-load-test.sh` | ~70 minutes |

---

## 🧪 Unit Tests

### Overview
- **Framework:** JUnit 5 + Mockito
- **Coverage:** Service layer, business logic
- **Location:** `src/test/java/`
- **Execution:** Maven Surefire plugin

### Test Files

**Transaction Gateway**
- `PaymentServiceTest.java` (8 test cases)
  - Successful payment initiation
  - Duplicate transaction handling
  - Transaction ID generation
  - Payment retrieval
  - Status updates
  - Large/small amounts
  - Concurrent transactions

**Ledger Service**
- `LedgerServiceTest.java` (8 test cases)
  - Successful payment processing
  - Insufficient funds validation
  - New account creation
  - Balance edge cases
  - Ledger retrieval
  - Account fetching

**Analytics Worker**
- `AnalyticsServiceTest.java` (8 test cases)
  - Metrics calculation
  - Average amount computation
  - Zero transaction handling
  - Hour/day metrics
  - Large transaction counts
  - Fractional amounts

### Run Unit Tests

```bash
# Run all unit tests
mvn test

# Run specific test class
mvn test -Dtest=PaymentServiceTest

# Run specific test method
mvn test -Dtest=PaymentServiceTest#testInitiatePaymentSuccess

# Generate coverage report
mvn test jacoco:report
# View: target/site/jacoco/index.html
```

### Expected Results
```
[INFO] Tests run: 24, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 🔗 Integration Tests

### Overview
- **Framework:** Testcontainers + PostgreSQL
- **Scope:** Database operations, entity persistence
- **Isolation:** Each test uses transaction rollback
- **Location:** `src/test/java/.../integration/`

### Test Files

**PaymentIntegrationTest.java**
- Database save/retrieve
- Unique constraint enforcement
- Transaction status updates
- Error reason storage
- Concurrent operations
- Timestamp preservation

### Run Integration Tests

```bash
# Run all integration tests
mvn verify

# Run only integration tests (skip unit tests)
mvn verify -DskipUnitTests=true

# Run specific integration test
mvn verify -Dtest=PaymentIntegrationTest
```

### Prerequisites
- Docker running (for Testcontainers)
- PostgreSQL image available locally

### Expected Results
```
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] Integration tests PASSED
```

---

## 🌐 API Tests

### Overview
- **Type:** End-to-end API testing
- **Tools:** curl commands
- **Scope:** HTTP endpoints, full payment flow
- **Duration:** ~1 minute

### Test Scenarios

**test-api.sh** executes 9 comprehensive tests:

1. **Health Check**
   - Verifies all 3 services are running
   - Endpoint: GET /health

2. **Initial Account Balance**
   - Retrieves balance for user001
   - Endpoint: GET /v1/ledger/account/{userId}

3. **Initiate Payment**
   - Sends valid payment request
   - Endpoint: POST /v1/payments
   - Response: 202 Accepted

4. **Wait for Processing**
   - Allows Kafka event processing
   - Sleep: 5 seconds

5. **Updated Balance Check**
   - Verifies balance changed after payment
   - Endpoint: GET /v1/ledger/account/{userId}

6. **Transaction History**
   - Retrieves transaction history
   - Endpoint: GET /v1/ledger/history/{userId}

7. **Analytics Metrics**
   - Retrieves last hour metrics
   - Endpoint: GET /v1/analytics/metrics/hour

8. **Duplicate Transaction Test**
   - Sends same transaction again
   - Expected: 409 Conflict

9. **Transaction Details**
   - Retrieves transaction by ID
   - Endpoint: GET /v1/payments/{transactionId}

### Run API Tests

```bash
# Start services first
docker-compose up -d
sleep 30

# Run API tests
chmod +x test-api.sh
./test-api.sh

# View detailed output
./test-api.sh 2>&1 | tee api-test-results.log
```

### Expected Results
```
✓ All 9 tests completed successfully
- Health checks: PASSED
- Payment flow: PASSED
- Idempotency: PASSED
- Analytics: PASSED
```

---

## 🚀 Complete Test Suite

### Overview
Runs all unit, integration, and API tests in sequence

### Run Complete Test Suite

```bash
chmod +x run-all-tests.sh
./run-all-tests.sh
```

### Test Execution Flow

```
1. Build project (Maven clean)
   └─ Compile all modules
   
2. Unit Tests (mvn test)
   ├─ PaymentServiceTest (8 tests)
   ├─ LedgerServiceTest (8 tests)
   └─ AnalyticsServiceTest (8 tests)
   
3. Integration Tests (mvn verify)
   └─ PaymentIntegrationTest (7 tests)
   
4. Infrastructure Check
   ├─ Docker available
   ├─ Docker Compose available
   └─ docker-compose.yml valid
   
5. API Tests (./test-api.sh)
   └─ 9 end-to-end tests
   
6. Summary Report
```

### Expected Duration: ~30 minutes

---

## 📊 Load Testing (250 TPS for 1M Transactions)

### Overview
- **Tool:** k6
- **Duration:** ~70 minutes
- **Target Load:** 250 TPS sustained
- **Total Transactions:** 1,000,000
- **Metrics Collected:** Response times, error rates, throughput

### Installation

```bash
# macOS
brew install k6

# Ubuntu/Debian
sudo apt-get install k6

# CentOS/RHEL
sudo yum install k6

# Windows (Chocolatey)
choco install k6

# Verify installation
k6 version
```

### Load Test Configuration

**load-test.js** settings:

```javascript
{
  vus: 250,                  // Virtual Users (= TPS)
  stages: [
    { duration: '30s', target: 50 },     // Ramp up
    { duration: '1m', target: 100 },
    { duration: '2m', target: 250 },
    { duration: '60m', target: 250 },    // Sustained load
    { duration: '2m', target: 100 },
    { duration: '1m', target: 0 },       // Ramp down
  ],
  
  thresholds: {
    'http_req_duration': [
      'p(50)<200',    // 50% should complete within 200ms
      'p(90)<500',    // 90% within 500ms
      'p(95)<800',    // 95% within 800ms
      'p(99)<1500',   // 99% within 1500ms
    ],
    'http_req_failed': ['rate<0.05'],  // <5% failure rate
  }
}
```

### Run Load Test

```bash
# Start services
docker-compose up -d
sleep 30

# Run load test (70 minutes)
chmod +x run-load-test.sh
./run-load-test.sh

# Or run k6 directly
k6 run load-test.js
```

### Expected Performance Metrics

#### Target Thresholds
| Metric | Target | Status |
|--------|--------|--------|
| P50 Latency | <200ms | ✅ |
| P90 Latency | <500ms | ✅ |
| P95 Latency | <800ms | ✅ |
| P99 Latency | <1500ms | ✅ |
| Success Rate | >95% | ✅ |
| Throughput | 250 TPS | ✅ |

#### Sample Results
```
Successful Payments: 750,000
Failed Payments: 50,000
Duplicate Payments: 200,000
Total Requests: 1,000,000

Response Time:
  Average: 245ms
  P50: 180ms
  P90: 420ms
  P95: 680ms
  P99: 1250ms
  Max: 2100ms

Error Rate: 3.2%
```

### Load Test Reports

Results saved to `load-test-results/`:
- `load-test-results.json` - Detailed results
- `summary.json` - Summary metrics
- `load-test.log` - Console output

### Analyze Load Test Results

```bash
# View summary
jq '.metrics' load-test-results/summary.json

# Filter specific metrics
jq '.metrics."http_req_duration".values' load-test-results/summary.json

# Check error details
jq '.metrics."http_req_failed"' load-test-results/summary.json
```

---

## 🔍 Detailed Test Analysis

### Code Coverage

```bash
# Generate coverage report
mvn test jacoco:report

# View coverage (recommended >80%)
open target/site/jacoco/index.html
```

Expected Coverage:
- **Lines:** 85%+
- **Branches:** 80%+
- **Methods:** 90%+

### Performance Analysis

```bash
# Monitor during load test
watch -n 1 'curl -s http://localhost:8081/actuator/metrics | jq .'

# Check database performance
docker exec swiftpay-postgres psql -U swiftpay -d swiftpay -c "
  SELECT
    schemaname,
    tablename,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
  FROM pg_stat_user_indexes
  ORDER BY idx_scan DESC;
"

# Monitor Kafka lag
docker exec swiftpay-kafka kafka-consumer-groups.sh \
  --bootstrap-server kafka:29092 \
  --group ledger-service-group \
  --describe
```

---

## 🐛 Troubleshooting Tests

### Unit Tests Fail

```bash
# Clean build
mvn clean test -U

# Check test logs
tail -f target/surefire-reports/*.txt

# Run with debugging
mvn test -X
```

### Integration Tests Fail

```bash
# Ensure Docker is running
docker ps

# Check Testcontainers logs
docker logs $(docker ps -lq)

# Run with verbose output
mvn verify -X -e
```

### API Tests Fail

```bash
# Check service logs
docker-compose logs transaction-gateway
docker-compose logs ledger-service

# Verify connectivity
curl -v http://localhost:8080/health

# Check database
docker exec swiftpay-postgres psql -U swiftpay -d swiftpay -c "SELECT COUNT(*) FROM transactions;"
```

### Load Test Issues

```bash
# Check k6 version
k6 version

# Run with lower VUs to diagnose
k6 run --vus 10 --duration 30s load-test.js

# Monitor resource usage
docker stats

# Check service logs
docker-compose logs --tail 100 -f
```

---

## 📈 CI/CD Integration

### GitHub Actions Workflow

Located in `.github/workflows/build-and-test.yml`:

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Build with Maven
        run: mvn clean package -DskipTests
      
      - name: Run Unit Tests
        run: mvn test
      
      - name: Run Integration Tests
        run: mvn verify
      
      - name: Build Docker Images
        run: docker-compose build
```

### Run Workflow Locally

```bash
# Install act
brew install act

# Run workflow
act push
```

---

## 📝 Test Results Documentation

### Report Template

Save results to `load-test-results/performance-report.md`:

```markdown
# SwiftPay Performance Test Report

## Test Configuration
- Date: YYYY-MM-DD HH:MM:SS
- Duration: 70 minutes
- Target Load: 250 TPS
- Total Transactions: 1,000,000

## Results Summary
- Successful: X transactions
- Failed: Y transactions
- Duplicates: Z transactions
- Success Rate: XX%

## Performance Metrics
- Average Latency: XXXms
- P95 Latency: XXXms
- P99 Latency: XXXms
- Max Latency: XXXms

## Resource Utilization
- CPU Usage: XX%
- Memory Usage: XX%
- DB Connections: XX/20
- Kafka Lag: XX messages

## Conclusion
✅ All thresholds met / ⚠ Some thresholds exceeded
```

---

## ✅ Test Checklist Before Submission

- [ ] All unit tests pass (mvn test)
- [ ] All integration tests pass (mvn verify)
- [ ] API tests pass (./test-api.sh)
- [ ] Load test completed (./run-load-test.sh)
- [ ] Load test results documented
- [ ] Code coverage >80%
- [ ] Docker images build successfully
- [ ] docker-compose up starts all services
- [ ] GitHub workflow runs without errors
- [ ] Test reports saved to load-test-results/

---

## 🎯 Next Steps

1. Run all tests: `./run-all-tests.sh`
2. Execute load test: `./run-load-test.sh`
3. Review results: `load-test-results/`
4. Commit to GitHub: `git push`
5. Monitor CI/CD: GitHub Actions

---

**Version:** 1.0.0  
**Last Updated:** 2024-01-15  
**Test Coverage:** Complete
