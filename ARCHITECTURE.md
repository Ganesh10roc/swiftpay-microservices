# SwiftPay Architecture Guide

## System Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                        External Clients                             │
│                     (Mobile, Web, API Clients)                      │
└────────────────────────┬──────────────────────────────────────────┘
                         │
                    ┌────▼─────┐
                    │ API GW    │
                    │ (Port 80) │
                    └────┬─────┘
                         │
        ┌────────────────┼────────────────┐
        │                │                │
    ┌───▼───┐        ┌───▼────┐      ┌───▼──────┐
    │ TXN    │        │ Ledger │      │Analytics │
    │Gateway │        │Service │      │ Worker   │
    │(8080)  │        │ (8081) │      │ (8082)   │
    └───┬───┘        └───┬────┘      └───┬──────┘
        │                │                │
        ▼                ▼                ▼
    ┌──────────────────────────┐
    │    PostgreSQL (5432)     │
    │  - Transactions Table    │
    │  - Accounts Table        │
    │  - Ledger Entries        │
    │  - Analytics Table       │
    └──────────────────────────┘
        │
        ├─────────────┐
        │             │
    ┌───▼────┐    ┌───▼──────┐
    │  Redis │    │  Kafka   │
    │ (6379) │    │ (9092)   │
    └────────┘    └──────────┘
```

## Component Responsibilities

### Service A: Transaction Gateway

**Purpose:** REST API entry point for payment initiation

**Key Responsibilities:**
- Accept payment requests via `/v1/payments` endpoint
- Validate request structure and basic business rules
- Implement idempotency using Redis (24-hour TTL)
- Create transaction record with PENDING status
- Publish `PaymentInitiatedEvent` to Kafka topic
- Return 202 (Accepted) to client immediately

**Dependencies:**
- PostgreSQL (transactions table)
- Redis (idempotency key storage)
- Apache Kafka (event publishing)

**Error Handling:**
- Duplicate transaction → 409 Conflict
- Invalid request → 400 Bad Request
- Redis down → 503 Service Unavailable
- Kafka down → 503 Service Unavailable

### Service B: Ledger Service

**Purpose:** Event-driven payment processor with atomic transactions

**Key Responsibilities:**
- Consume `PaymentInitiatedEvent` from Kafka
- Retrieve and lock sender & receiver accounts (pessimistic locking)
- Validate sender has sufficient balance
- Perform atomic debit/credit operation:
  - Debit sender's account
  - Credit receiver's account
  - Record double-entry ledger
- Publish `PaymentCompletedEvent` or `PaymentFailedEvent`
- Expose GET endpoint for transaction history and account details

**Dependencies:**
- PostgreSQL (accounts & ledger tables)
- Apache Kafka (event consumption & publishing)

**Concurrency Control:**
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<Account> findByUserIdForUpdate(String userId);
```

**Error Handling:**
- Insufficient funds → Publish `PaymentFailedEvent`
- Database constraint violation → Transaction rollback + retry
- Kafka consumer failure → Retry topic with exponential backoff

### Service C: Analytics Worker (Bonus)

**Purpose:** Real-time analytics and metrics aggregation

**Key Responsibilities:**
- Consume `PaymentCompletedEvent` from Kafka
- Insert completed transactions into analytics table
- Expose endpoints for volume metrics:
  - Transaction count per period
  - Total transaction volume
  - Average transaction amount
  - Unique sender count
- Support custom time-range queries

**Dependencies:**
- PostgreSQL (analytics table)
- Apache Kafka (event consumption)

## Data Flow Sequences

### Successful Payment Flow

```
1. Client → POST /v1/payments
   {sender_id, receiver_id, amount, currency}

2. Transaction Gateway:
   - Redis: Check idempotency key (NOT EXISTS)
   - PostgreSQL: INSERT transaction (status=PENDING)
   - Redis: SET idempotency_key (TTL=24h)
   - Kafka: PUBLISH PaymentInitiatedEvent
   - Return: 202 Accepted

3. Ledger Service (Consumer):
   - Kafka: CONSUME PaymentInitiatedEvent
   - PostgreSQL: SELECT accounts FOR UPDATE (lock)
   - Validate balance (sender.balance >= amount)
   - PostgreSQL: UPDATE sender (balance -= amount)
   - PostgreSQL: UPDATE receiver (balance += amount)
   - PostgreSQL: INSERT ledger_entry (debit)
   - PostgreSQL: INSERT ledger_entry (credit)
   - Kafka: PUBLISH PaymentCompletedEvent

4. Analytics Worker (Consumer):
   - Kafka: CONSUME PaymentCompletedEvent
   - PostgreSQL: INSERT payment_analytics

5. (Optional) Client → GET /v1/payments/{transactionId}
   - Query updated transaction status
```

### Failed Payment Flow (Insufficient Funds)

```
1-2: Same as successful flow

3. Ledger Service:
   - PostgreSQL: SELECT accounts FOR UPDATE (lock)
   - Validate balance (sender.balance < amount) → FAIL
   - Kafka: PUBLISH PaymentFailedEvent (reason: "Insufficient funds")
   - NO database updates

4. Transaction Gateway (optional polling):
   - Receives PaymentFailedEvent (via listener or polling)
   - Updates transaction status to FAILED
```

## Database Design

### Transactions Table (Service A)
```sql
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(36) UNIQUE NOT NULL,  -- UUID
    sender_id VARCHAR(100) NOT NULL,
    receiver_id VARCHAR(100) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,                 -- PENDING, COMPLETED, FAILED
    idempotency_key VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    error_reason TEXT,
    
    INDEX (transaction_id),
    INDEX (sender_id),
    INDEX (receiver_id),
    INDEX (status),
    INDEX (created_at)
);
```

### Accounts Table (Service B)
```sql
CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(100) UNIQUE NOT NULL,
    balance NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    version BIGINT,                              -- Optimistic locking
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    
    INDEX (user_id)
);
```

### Ledger Entries Table (Service B)
```sql
CREATE TABLE ledger_entries (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(36) UNIQUE NOT NULL,
    user_id VARCHAR(100) NOT NULL,
    account_id BIGINT NOT NULL,
    debit NUMERIC(19, 2),                        -- Outgoing
    credit NUMERIC(19, 2),                       -- Incoming
    balance_after NUMERIC(19, 2) NOT NULL,      -- Snapshot
    status VARCHAR(20) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    
    INDEX (transaction_id),
    INDEX (user_id),
    INDEX (created_at)
);
```

### Payment Analytics Table (Service C)
```sql
CREATE TABLE payment_analytics (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(36) UNIQUE NOT NULL,
    sender_id VARCHAR(100) NOT NULL,
    receiver_id VARCHAR(100) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    completed_at TIMESTAMP NOT NULL,
    ingested_at TIMESTAMP NOT NULL,
    
    INDEX (transaction_id),
    INDEX (completed_at)
);
```

## Kafka Topics

### Payment Topics

**Topic: `payment-initiated`**
- Schema: `PaymentInitiatedEvent`
- Partitions: 3
- Replication Factor: 1
- Consumer: Ledger Service

**Topic: `payment-completed`**
- Schema: `PaymentCompletedEvent`
- Partitions: 3
- Replication Factor: 1
- Consumer: Analytics Worker

**Topic: `payment-failed`**
- Schema: `PaymentFailedEvent`
- Partitions: 3
- Replication Factor: 1
- Consumer: Transaction Gateway (optional)

### Retry Topics

**Topic: `payment-initiated-retry-0`, `payment-initiated-retry-1`, etc.**
- Auto-created for Kafka retry mechanism
- Exponential backoff: 1s, 2s, 4s, 8s

## Idempotency Implementation

### Redis Strategy

```
Key Pattern: idempotency:{transactionId}
Value: {transactionId} (any non-null value)
TTL: 24 hours

Flow:
1. Check EXISTS idempotency:{txn_id}
   - If true → Throw DuplicateTransactionException (409)
   - If false → Proceed to step 2

2. Set idempotency:{txn_id} with EX 86400
   - Now other requests with same ID are rejected

3. Save transaction to database
   - If DB save fails → Delete idempotency key (rollback)
```

## Concurrency Control

### Pessimistic Locking (Accounts)

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<Account> findByUserIdForUpdate(String userId);
```

**Mechanism:**
- SELECT ... FOR UPDATE clause
- Blocks other writers on same row
- Released at transaction end (COMMIT/ROLLBACK)
- Serializes concurrent payment processing

**Tradeoff:**
- Pro: Guarantees consistency
- Con: Reduced throughput under contention

### Optimistic Locking (Future Enhancement)

```java
@Version
private Long version;
```

Can be added later for better throughput.

## Error Handling Strategy

### Level 1: Input Validation
- API request structure validation
- Amount > 0, Currency valid, etc.
- Return: 400 Bad Request

### Level 2: Business Rule Validation
- Sender = Receiver check
- Sufficient balance check
- Return: 409 Conflict (if duplicate) or publish PaymentFailedEvent

### Level 3: System Failures
- Database down → Transaction rollback, retry with backoff
- Kafka down → In-memory queue with drain-on-recovery
- Redis down → Disable caching, or graceful degradation

### Retry Mechanism
```
Attempt 1: Immediate
Attempt 2: After 1s
Attempt 3: After 2s
Attempt 4: After 4s
Attempt 5+: Give up → PaymentFailedEvent
```

## Performance Optimization

### Connection Pooling
- PostgreSQL: 20 connections max per service
- Redis: 8 connections with queue
- Kafka: 3 consumer threads per partition

### Query Optimization
```sql
-- Indexed columns for fast lookup
WHERE transaction_id = ...      -- Indexed
WHERE sender_id = ...           -- Indexed
WHERE status = 'PENDING'        -- Indexed
WHERE created_at > ...          -- Indexed
```

### Caching Strategy
- Redis for idempotency keys (24h TTL)
- No application-level result caching
- Database keeps single source of truth

### Kafka Batching
```
batch.size: 20
linger.ms: 10
```

## Monitoring & Observability

### Metrics to Track
1. Transaction throughput (TPS)
2. P95/P99 latency
3. Payment success rate
4. Database connection pool usage
5. Kafka consumer lag
6. Redis hit/miss ratio

### Health Checks
```
GET /health → Basic status
GET /health/live → Liveness (process running)
GET /health/ready → Readiness (dependencies ready)
```

### Logging
```yaml
# Structured logging in JSON format
timestamp: ISO8601
level: DEBUG/INFO/WARN/ERROR
service: transaction-gateway
transaction_id: UUID
sender_id: userXX
message: Payment initiated
```

## Security Considerations

1. **No Sensitive Data in Logs**
   - Full account numbers masked
   - Transaction amounts ok
   - Never log passwords or secrets

2. **Input Sanitization**
   - SQL injection: Parameterized queries (JPA)
   - XSS: Return JSON, not HTML
   - CSRF: Stateless REST API

3. **Transaction Isolation**
   - SERIALIZABLE for critical operations
   - READ_COMMITTED for queries

4. **Rate Limiting**
   - Can be added at API Gateway layer
   - Per-user transaction rate limiting

## Deployment Architecture

### Docker Compose (Dev/Test)
```
Single host, all services, persistent volumes
```

### Kubernetes (Production Future)
```
- Separate pods for each service
- StatefulSet for databases
- ConfigMap for settings
- Secrets for credentials
- Horizontal Pod Autoscaling (HPA)
```

## Scaling Considerations

### Horizontal Scaling
1. **API Gateway (nginx/Envoy)**
   - Load balance across Transaction Gateway instances
   
2. **Ledger Service Instances**
   - Kafka partitions = Ledger instances (max)
   - Each instance processes subset of partitions
   - Requires careful state management

3. **Database Sharding (Future)**
   - Shard by sender_id or user_id
   - Multiple PostgreSQL instances
   - Application-level routing

### Vertical Scaling
- Increase JVM heap for caching
- Increase connection pool sizes
- Increase Kafka batch sizes

## Testing Strategy

### Unit Tests
- Repository layer
- Service business logic
- Exception handling

### Integration Tests
- Kafka producer/consumer
- PostgreSQL transactions
- Redis operations

### End-to-End Tests
- Full payment flow
- Idempotency validation
- Ledger consistency

### Load Testing
- 250 TPS sustained
- 1M transaction scenario
- Latency distribution (p50, p95, p99)

---

**Version:** 1.0.0  
**Last Updated:** 2024-01-15
