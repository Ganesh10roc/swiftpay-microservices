# SwiftPay AI Playbook - Hackathon Edition

## Overview
This playbook guides AI systems (Claude, GPT-4, etc.) in understanding, analyzing, and optimizing the SwiftPay payment ledger system. Use this as a reference for architectural decisions, debugging strategies, and performance improvements.

## System Architecture Summary

### Three Core Services
1. **Transaction Gateway** (Port 8080) - REST API entry point
2. **Ledger Service** (Port 8081) - Payment processor with atomic operations
3. **Analytics Worker** (Port 8082) - Real-time metrics aggregation

### Technology Stack
- **Language:** Java 21
- **Framework:** Spring Boot 3.3.0
- **Database:** PostgreSQL 16
- **Message Broker:** Apache Kafka 7.5.0
- **Cache:** Redis 7
- **Containerization:** Docker & Docker Compose

## Key Design Patterns

### 1. Event-Driven Architecture
- **Pattern:** Publish-Subscribe via Kafka
- **Benefit:** Decoupled services, scalable event processing
- **Topics:**
  - `payment-initiated` → Ledger Service consumes
  - `payment-completed` → Analytics Worker consumes
  - `payment-failed` → Retry mechanism

### 2. Idempotency Strategy
- **Implementation:** Redis-based with 24-hour TTL
- **Key Generation:** `transaction_id` (client-provided UUID)
- **Benefit:** Handles duplicate requests safely (HTTP 409 response)

### 3. Pessimistic Locking
- **Database Level:** Sender & receiver accounts locked during balance updates
- **Transaction Isolation:** READ_COMMITTED
- **Benefit:** Prevents race conditions in concurrent transfers

### 4. Asynchronous Processing
- **Approach:** 202 Accepted → Event-driven completion
- **Advantage:** Non-blocking API, improved throughput
- **Trade-off:** Eventually consistent (not immediately consistent)

## Critical Data Flows

### Payment Submission Flow
```
1. Client POST /v1/payments
   └─ Validation: transaction_id uniqueness check (Redis)
   
2. Transaction Gateway saves PENDING status
   └─ Publish: PaymentInitiatedEvent → Kafka topic

3. Ledger Service consumes event
   └─ Lock sender & receiver accounts
   └─ Validate sender balance
   └─ Atomic debit/credit operations
   └─ Publish: PaymentCompletedEvent → Kafka topic

4. Analytics Worker consumes event
   └─ Insert into analytics table
   └─ Update metrics
```

### Error Handling Paths
- **Duplicate Transaction:** Idempotency check → HTTP 409
- **Insufficient Funds:** Ledger Service publishes PaymentFailedEvent
- **Kafka Unavailable:** Retry with exponential backoff (3-4 attempts)
- **Database Lock Timeout:** Automatic rollback + retry via Kafka

## Performance Characteristics

### Expected Throughput
- **Target:** 250 TPS (transactions per second)
- **Duration:** 1M transactions @ 250 TPS ≈ ~1 hour load test
- **P95 Latency Target:** <500ms
- **P99 Latency Target:** <1000ms

### Resource Configuration
- **PostgreSQL:** 20 max connections per service
- **Redis:** Jedis pool with 8 connections
- **Kafka:** 3 partitions per topic, Snappy compression
- **JVM Heap:** 512MB-1GB per service

## Optimization Opportunities

### 1. Database Indexing
- Verify indexes on: `transaction_id`, `sender_id`, `receiver_id`, timestamps
- Monitor slow query logs for N+1 patterns

### 2. Kafka Tuning
- Batch size: Currently 20 inserts
- Partition count: Consider load distribution
- Consumer lag: Monitor via Kafka console tools

### 3. Redis Cache Strategy
- Hit ratio target: >80%
- Key patterns: Idempotency keys with TTL management
- Memory monitoring: Watch for key eviction

### 4. Connection Pooling
- HikariCP settings: `maximumPoolSize`, `minimumIdle`
- Monitor pool exhaustion errors

## Debugging Strategies

### Transaction Stuck in PENDING State
1. Check Kafka consumer lag:
   ```bash
   docker exec swiftpay-kafka kafka-consumer-groups.sh \
     --bootstrap-server kafka:29092 \
     --group ledger-service-group \
     --describe
   ```

2. Verify database locks:
   ```sql
   SELECT * FROM pg_locks WHERE pid = <process_id>;
   ```

3. Check service logs:
   ```bash
   docker-compose logs -f ledger-service
   ```

### High Latency Issues
- Check Redis connection pool status
- Monitor PostgreSQL active connections
- Review Kafka partition distribution
- Analyze JVM garbage collection logs

### Memory Leaks
- Monitor heap usage over time
- Check for unclosed database connections
- Review Kafka consumer memory allocation

## Deployment Checklists

### Pre-Production
- [ ] All 3 services passing unit tests
- [ ] Integration tests with real PostgreSQL/Kafka
- [ ] Load test with 250 TPS for 1M transactions
- [ ] Database backups configured
- [ ] Monitoring alerts set up
- [ ] API documentation reviewed
- [ ] Security scan completed

### Production Readiness
- [ ] Database credentials rotated
- [ ] Kafka SSL/TLS enabled
- [ ] API rate limiting configured
- [ ] Distributed tracing enabled
- [ ] Log aggregation configured
- [ ] Disaster recovery plan documented

## AI Agent Guidelines

### When Analyzing Code
1. **Context First:** Understand service boundaries and event flow
2. **Trace Transactions:** Follow a single payment through all 3 services
3. **Check Concurrency:** Verify lock handling and race conditions
4. **Validate Idempotency:** Ensure duplicate request safety

### When Debugging Issues
1. **Start with Events:** Check Kafka topic and consumer lag
2. **Verify State:** Check database transaction status
3. **Review Logs:** Look for timeouts, lock conflicts, or connection errors
4. **Reproduce:** Create minimal test case to isolate issue

### When Optimizing Performance
1. **Profile First:** Use actuator metrics and slow query logs
2. **Identify Bottleneck:** DB, Cache, Network, or CPU?
3. **Test Changes:** Run load test before/after optimization
4. **Monitor Trade-offs:** Ensure no regression in other areas

## Common Issues & Resolutions

| Issue | Cause | Resolution |
|-------|-------|-----------|
| Services not starting | Port conflicts | Check `netstat`, adjust docker-compose ports |
| Transactions stuck PENDING | Consumer lag | Restart ledger-service consumer |
| High memory usage | Unbounded caches | Check Redis eviction, adjust JVM heap |
| Timeout errors | Connection pool exhausted | Increase HikariCP pool size |
| Duplicate transaction accepted | Idempotency expired | Reduce Redis TTL or implement DB-level uniqueness |

## References

- [Spring Boot Best Practices](https://spring.io/projects/spring-boot)
- [Kafka Consumer Patterns](https://kafka.apache.org/documentation/)
- [PostgreSQL Performance Tuning](https://wiki.postgresql.org/wiki/Performance_Optimization)
- [Redis Optimization](https://redis.io/docs/)

---

**Version:** 1.0.0  
**Created:** 2026-09-16  
**For:** AI Systems & Hackathon Teams
