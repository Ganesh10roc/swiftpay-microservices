# SwiftPay Hackathon - End-to-End Testing Guide

## Overview
This document provides a complete end-to-end validation checklist for the SwiftPay Hackathon. Use this guide to verify all components work together correctly.

## Pre-Setup Validation

### 1. Prerequisites Check
```bash
# Verify Java installation
java -version
# Expected: Java 21+

# Verify Maven installation
mvn -version
# Expected: Apache Maven 3.8+

# Verify Docker
docker --version
docker-compose --version

# Verify Python (for PCAP generation)
python3 --version
# Expected: Python 3.8+
```

### 2. Repository Structure Validation
```bash
cd swiftpay

# Check all required files exist
ls -la *.md
# Expected files:
#   - README.md
#   - ARCHITECTURE.md
#   - IMPLEMENTATION_GUIDE.md
#   - AI_PLAYBOOK.md
#   - HACKATHON_PROMPTS.md
#   - HACKATHON_END_TO_END.md (this file)

# Check services
ls -d */
# Expected:
#   - common/
#   - transaction-gateway/
#   - ledger-service/
#   - analytics-worker/
#   - .github/
#   - k8s/
```

## Build Phase (Phase 1)

### Step 1: Clean Build All Services
```bash
# Full clean build with tests
mvn clean package

# Expected output:
# [INFO] Building transaction-gateway 1.0.0
# [INFO] Building ledger-service 1.0.0
# [INFO] Building analytics-worker 1.0.0
# [INFO] BUILD SUCCESS
```

### Step 2: Verify Build Artifacts
```bash
# Check JAR files exist
ls -la */target/*.jar

# Expected files:
# - transaction-gateway/target/transaction-gateway-1.0.0.jar (~50MB)
# - ledger-service/target/ledger-service-1.0.0.jar (~45MB)
# - analytics-worker/target/analytics-worker-1.0.0.jar (~42MB)
```

### Step 3: Verify Tests Pass
```bash
# Run all tests
mvn test

# Expected: All tests pass
# [INFO] Tests run: XX, Failures: 0, Errors: 0

# Run integration tests
mvn verify

# Expected: All integration tests pass
```

## Infrastructure Phase (Phase 2)

### Step 4: Start Docker Infrastructure
```bash
# Start all services with logs
docker-compose up -d

# Monitor startup
docker-compose logs -f

# Expected startup sequence:
# 1. Zookeeper starts
# 2. PostgreSQL starts
# 3. Redis starts
# 4. Kafka starts (depends on Zookeeper)
# 5. Transaction Gateway starts (depends on services)
# 6. Ledger Service starts (depends on services)
# 7. Analytics Worker starts (depends on services)
```

### Step 5: Verify Service Health
```bash
# Wait for services to be ready (~45 seconds)
sleep 45

# Check all health endpoints
curl -s http://localhost:8080/health | jq '.'
curl -s http://localhost:8081/health | jq '.'
curl -s http://localhost:8082/health | jq '.'

# Expected response:
# {
#   "status": "UP",
#   "components": {...}
# }
```

### Step 6: Verify Infrastructure Components
```bash
# PostgreSQL connectivity
docker exec swiftpay-postgres psql -U swiftpay -d swiftpay -c "SELECT 1;"

# Redis connectivity
docker exec swiftpay-redis redis-cli ping
# Expected: PONG

# Kafka connectivity
docker exec swiftpay-kafka kafka-broker-api-versions.sh --bootstrap-server kafka:29092

# Kafka topics
docker exec swiftpay-kafka kafka-topics.sh --list --bootstrap-server kafka:29092
# Expected topics:
# - payment-initiated
# - payment-completed
# - payment-failed
```

## API Testing Phase (Phase 3)

### Step 7: Test Transaction Gateway API

#### 7.1: Create Payment Transaction
```bash
# Create a payment
curl -X POST http://localhost:8080/v1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "transaction_id": "test-txn-001",
    "sender_id": "user001",
    "receiver_id": "user002",
    "amount": 150.00,
    "currency": "USD"
  }'

# Expected response (202 Accepted):
# {
#   "status": "SUCCESS",
#   "message": "Payment initiated successfully",
#   "data": {
#     "id": 1,
#     "transactionId": "test-txn-001",
#     "status": "PENDING",
#     "senderId": "user001",
#     "receiverId": "user002",
#     "amount": 150.00
#   }
# }
```

#### 7.2: Test Idempotency (Duplicate Request)
```bash
# Resend same transaction_id
curl -X POST http://localhost:8080/v1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "transaction_id": "test-txn-001",
    "sender_id": "user001",
    "receiver_id": "user002",
    "amount": 150.00,
    "currency": "USD"
  }'

# Expected response (409 Conflict):
# {
#   "status": "ERROR",
#   "message": "Transaction already exists: test-txn-001",
#   "error": {"code": "DUPLICATE_TRANSACTION"}
# }
```

#### 7.3: Get Transaction Status
```bash
# Query transaction
curl http://localhost:8080/v1/payments/test-txn-001

# Expected response (200 OK):
# Status should eventually change from PENDING to COMPLETED
```

### Step 8: Test Ledger Service API

#### 8.1: Get Account Details
```bash
# Check account balance
curl http://localhost:8081/v1/ledger/account/user001 | jq '.'

# Expected response:
# {
#   "status": "SUCCESS",
#   "data": {
#     "userId": "user001",
#     "balance": 9850.00,
#     "currency": "USD"
#   }
# }
```

#### 8.2: Get Ledger History
```bash
# Get transaction history
curl "http://localhost:8081/v1/ledger/history/user001?page=0&size=10" | jq '.'

# Expected response:
# {
#   "status": "SUCCESS",
#   "data": {
#     "content": [
#       {
#         "transactionId": "test-txn-001",
#         "type": "DEBIT",
#         "amount": 150.00,
#         "balanceAfter": 9850.00
#       }
#     ],
#     "pageable": {...},
#     "totalElements": 1
#   }
# }
```

### Step 9: Test Analytics Service API

#### 9.1: Get Hour Metrics
```bash
# Get last hour metrics
curl http://localhost:8082/v1/analytics/metrics/hour | jq '.'

# Expected response:
# {
#   "status": "SUCCESS",
#   "data": {
#     "transactionCount": 1,
#     "totalAmount": 150.00,
#     "averageAmount": 150.00,
#     "uniqueSenderCount": 1,
#     "uniqueReceiverCount": 1
#   }
# }
```

#### 9.2: Get Day Metrics
```bash
# Get last day metrics
curl http://localhost:8082/v1/analytics/metrics/day | jq '.'
```

#### 9.3: Get Custom Period Metrics
```bash
# Get metrics for specific period
curl "http://localhost:8082/v1/analytics/metrics?startTime=2024-01-01T00:00:00&endTime=2024-12-31T23:59:59" | jq '.'
```

## Load Testing Phase (Phase 4)

### Step 10: Generate PCAP File
```bash
# Generate network traffic capture with 500 transactions
python3 generate-pcap.py PACKET_CAPTURE.pcap 500

# Expected output:
# ✓ Generated PCAP file: PACKET_CAPTURE.pcap
# - Transactions: 500
# - Total packets: 4000
# - File size: XX.XX KB
```

### Step 11: Run Load Test
```bash
# Install k6 if not already installed
# macOS: brew install k6
# Linux: sudo apt-get install k6
# Windows: choco install k6

# Run load test with 250 concurrent users for 2 minutes
k6 run load-test.js

# Or run with custom settings
k6 run -u 100 -d 60s load-test.js

# Expected results:
# - Error rate: < 1%
# - P95 latency: < 500ms
# - P99 latency: < 1000ms
# - Throughput: ~250 TPS
```

### Step 12: Analyze PCAP File
```bash
# Install Wireshark if needed
# macOS: brew install wireshark
# Linux: sudo apt-get install wireshark
# Windows: download from wireshark.org

# View PCAP with Wireshark
wireshark PACKET_CAPTURE.pcap

# Or analyze with tshark
tshark -r PACKET_CAPTURE.pcap -Y "http.request" -e frame.number -e http.request.method -T fields

# Extract statistics
tshark -r PACKET_CAPTURE.pcap -z io,stat,1
```

## Data Consistency Phase (Phase 5)

### Step 13: Verify Data Consistency
```bash
# Connect to PostgreSQL
docker exec -it swiftpay-postgres psql -U swiftpay -d swiftpay

# Check transactions
SELECT COUNT(*) as total_transactions FROM transactions;
SELECT status, COUNT(*) as count FROM transactions GROUP BY status;

# Check account balances
SELECT user_id, balance FROM accounts ORDER BY user_id;

# Check ledger entries balance
SELECT user_id, SUM(CASE WHEN type='DEBIT' THEN -amount ELSE amount END) as net_balance 
FROM ledger_entries GROUP BY user_id;

# Verify ledger entries match account balances
SELECT a.user_id, a.balance, 
  SUM(CASE WHEN l.type='DEBIT' THEN -l.amount ELSE l.amount END) as calculated_balance
FROM accounts a
LEFT JOIN ledger_entries l ON a.user_id = l.user_id
GROUP BY a.user_id, a.balance
HAVING a.balance != SUM(CASE WHEN l.type='DEBIT' THEN -l.amount ELSE l.amount END);

# Expected: 0 rows (no mismatches)
```

### Step 14: Verify Kafka Topics
```bash
# Check Kafka consumer lag
docker exec swiftpay-kafka kafka-consumer-groups.sh \
  --bootstrap-server kafka:29092 \
  --group ledger-service-group \
  --describe

# Expected: LAG should be 0 or very small (all messages processed)

# Check analytics consumer lag
docker exec swiftpay-kafka kafka-consumer-groups.sh \
  --bootstrap-server kafka:29092 \
  --group analytics-worker-group \
  --describe
```

### Step 15: Verify Idempotency
```bash
# Check Redis keys
docker exec swiftpay-redis redis-cli KEYS "*" | head -20

# Check key TTL
docker exec swiftpay-redis redis-cli TTL "idempotency:test-txn-001"

# Expected: TTL should be ~86400 seconds (24 hours)
```

## Cleanup Phase (Phase 6)

### Step 16: Stop Services
```bash
# Stop all services gracefully
docker-compose down

# Remove volumes (optional - removes data)
docker-compose down -v

# Verify cleanup
docker ps | grep swiftpay
# Expected: No running containers
```

## End-to-End Validation Checklist

- [ ] Phase 1: Build Phase
  - [ ] All services build successfully
  - [ ] All tests pass
  - [ ] No compilation warnings

- [ ] Phase 2: Infrastructure Phase
  - [ ] All services start within 60 seconds
  - [ ] All health checks pass
  - [ ] PostgreSQL, Redis, Kafka all accessible

- [ ] Phase 3: API Testing Phase
  - [ ] Transaction creation returns 202
  - [ ] Idempotency prevents duplicates (409)
  - [ ] Transaction status updates from PENDING to COMPLETED
  - [ ] Account balances reflect debit/credit
  - [ ] Analytics metrics update correctly

- [ ] Phase 4: Load Testing Phase
  - [ ] PCAP file generated successfully
  - [ ] Load test completes without major errors
  - [ ] Latency within acceptable bounds
  - [ ] Error rate < 1%

- [ ] Phase 5: Data Consistency Phase
  - [ ] No transaction duplicates
  - [ ] Account balances match ledger calculations
  - [ ] Kafka consumer lag is 0
  - [ ] Idempotency keys expire correctly

- [ ] Phase 6: Cleanup Phase
  - [ ] All services stopped cleanly
  - [ ] No orphaned containers

## Troubleshooting

### Services Not Starting
```bash
# View logs for specific service
docker-compose logs ledger-service

# Common issues:
# - Port conflicts: Change ports in docker-compose.yml
# - Out of memory: Increase Docker memory allocation
# - Slow startup: Increase healthcheck timeout
```

### High Latency During Load Test
```bash
# Check container resource usage
docker stats

# Check database connections
docker exec swiftpay-postgres \
  psql -U swiftpay -d swiftpay -c "SELECT count(*) FROM pg_stat_activity;"

# Check Kafka consumer lag
docker exec swiftpay-kafka kafka-consumer-groups.sh \
  --bootstrap-server kafka:29092 \
  --group ledger-service-group --describe
```

### Data Inconsistency Issues
```bash
# Check for stuck transactions
SELECT * FROM transactions WHERE status = 'PENDING' 
AND created_at < NOW() - INTERVAL '5 minutes';

# Check Kafka dead letter topic
docker exec swiftpay-kafka kafka-topics.sh \
  --list --bootstrap-server kafka:29092 | grep "retry\|dlq"
```

## Next Steps

After successful validation:

1. **Review AI Playbook** - [AI_PLAYBOOK.md](AI_PLAYBOOK.md)
2. **Pick Hackathon Challenge** - [HACKATHON_PROMPTS.md](HACKATHON_PROMPTS.md)
3. **Analyze Network Traffic** - [PACKET_CAPTURE.pcap.README.md](PACKET_CAPTURE.pcap.README.md)
4. **Implement Your Solution**
5. **Run Tests & Benchmarks**
6. **Submit Results**

---

**Version:** 1.0.0  
**Created:** 2026-09-16  
**Status:** Production Ready
