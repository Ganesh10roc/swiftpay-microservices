# SwiftPay Hackathon - AI Prompts & Challenges

## Prompt Set 1: Code Review & Optimization

### Prompt 1.1: Performance Audit
```
Analyze the SwiftPay transaction-gateway service for performance bottlenecks.
Focus on:
1. Redis idempotency key lookup efficiency
2. Kafka producer batch configuration
3. PostgreSQL connection pool settings
4. Jackson JSON serialization overhead

Provide 3 specific optimizations with expected impact (latency reduction %).
```

### Prompt 1.2: Concurrency Issues
```
Review the payment processing flow in ledger-service for race conditions.
Specifically check:
1. Account locking mechanism during concurrent transfers
2. Balance validation timing window
3. Ledger entry insertion atomicity
4. Kafka consumer thread safety

Identify any potential data inconsistency scenarios and propose fixes.
```

### Prompt 1.3: Error Handling Improvements
```
Evaluate the error handling in PaymentService (transaction-gateway).
Current state:
- Idempotency check returns 409 Conflict
- Insufficient funds handled at ledger-service level
- Kafka failures trigger retries

Propose:
1. Better error messages for client debugging
2. Circuit breaker pattern for Kafka timeouts
3. Graceful degradation strategy
4. Enhanced logging for observability
```

## Prompt Set 2: Feature Development

### Prompt 2.1: Transaction Rollback
```
Design a feature to allow payment reversals/refunds.
Requirements:
1. Original transaction must be COMPLETED
2. Refund creates separate transaction with reversed amounts
3. Ledger entries should show refund chain
4. Analytics should track refund rates

Provide:
- Database schema changes
- API endpoint specification
- Event flow diagram
- Test scenarios
```

### Prompt 2.2: Real-Time Notifications
```
Add WebSocket support for real-time payment status updates.
Constraints:
1. Maintain idempotency guarantees
2. Support 10k concurrent WebSocket connections
3. No breaking changes to existing REST API
4. Compatible with existing event-driven architecture

Design:
- Connection lifecycle management
- Message queue strategy
- Failure recovery mechanism
- Client subscription model
```

### Prompt 2.3: Advanced Analytics
```
Extend analytics-worker to support:
1. Anomaly detection (unusual transaction patterns)
2. User risk scoring
3. Fraud indicators
4. Geographic distribution analysis

Constraints:
- Use only existing Kafka events
- Maintain sub-100ms query latency
- Support ML model integration

Provide:
- New metrics endpoints
- Database schema additions
- ML integration strategy
```

## Prompt Set 3: System Design

### Prompt 3.1: Scalability Assessment
```
SwiftPay currently targets 250 TPS. Design for 10,000 TPS.

Analyze:
1. Database bottlenecks and sharding strategy
2. Kafka partition distribution
3. Service replication across availability zones
4. Caching layer optimization

Provide:
- Architecture diagram
- Cost estimates
- Implementation phases
- Risk mitigation
```

### Prompt 3.2: Multi-Currency Support
```
Extend SwiftPay to support cross-currency transfers with real-time exchange rates.

Design considerations:
1. Exchange rate source and caching
2. Currency conversion accuracy (rounding rules)
3. Regulatory compliance per currency
4. Settlement delays for international transfers

Provide:
- Schema changes
- API modifications
- New service requirements
- Testing strategy
```

### Prompt 3.3: Compliance & Audit
```
Add compliance features for financial regulations:
1. Transaction limits per user/day
2. Know Your Customer (KYC) verification
3. Anti-Money Laundering (AML) checks
4. Complete audit trail with immutability

Design:
- Data governance approach
- Audit log architecture
- Compliance reporting
- Integration points
```

## Prompt Set 4: Troubleshooting

### Prompt 4.1: Production Incident
```
Production Alert: 40% of transactions stuck in PENDING state for 5+ minutes.
System metrics:
- Ledger-service consumer lag: 50,000 messages
- PostgreSQL: 20/20 connections in use
- Redis: Normal operation
- Kafka: All brokers healthy

Diagnose:
1. Root cause
2. Immediate mitigation steps
3. Prevention measures
4. Post-incident actions
```

### Prompt 4.2: Data Inconsistency
```
Database inconsistency detected:
- Ledger entries for sender exist but receiver account has no credit
- Transaction status is COMPLETED
- No corresponding PaymentFailedEvent

Investigate:
1. Possible causes
2. Detection mechanism
3. Reconciliation strategy
4. Prevention implementation
```

### Prompt 4.3: Performance Degradation
```
Performance has degraded over 2 weeks:
- P95 latency: 200ms → 1200ms
- P99 latency: 400ms → 3500ms
- Error rate: 0.1% → 2.5%

Investigation clues:
- Database size increased 3x
- No code changes in period
- Memory usage normal
- Kafka lag stable

Analyze:
1. Likely causes
2. Diagnostic queries
3. Quick fixes
4. Long-term solutions
```

## Prompt Set 5: Testing & QA

### Prompt 5.1: Load Test Scenario
```
Create a k6 load test that simulates realistic payment patterns:
1. User population: 10,000 unique users
2. Baseline balance: $10,000 per user
3. Transfer patterns:
   - 60% small transfers ($10-100)
   - 30% medium transfers ($100-1000)
   - 10% large transfers ($1000+)
4. Concurrent users: Start 100, scale to 500
5. Duration: 30 minutes

Test objectives:
- Identify P95/P99 latency targets
- Measure error rate at scale
- Detect resource exhaustion
- Verify idempotency under load

Provide: k6 script with custom metrics
```

### Prompt 5.2: Chaos Engineering
```
Design chaos tests for SwiftPay:

Failure scenarios:
1. PostgreSQL connection pool exhaustion
2. Kafka broker shutdown (during peak load)
3. Redis cache invalidation
4. Network partition between services
5. Slow database queries

For each:
- Detection mechanism
- Expected system behavior
- Recovery time target
- Mitigation strategy

Provide: Testcontainers-based test framework
```

### Prompt 5.3: Integration Test Coverage
```
Current integration tests cover happy path only.
Design comprehensive test suite:

Areas to cover:
1. Idempotency edge cases (expired vs valid keys)
2. Concurrent transfers (race condition scenarios)
3. Kafka failure and retry logic
4. Database constraint violations
5. Partial failure handling (e.g., event published but DB failed)

Provide:
- Test case specifications
- JUnit 5 test code
- Test data factory patterns
- Testcontainers setup
```

## Prompt Set 6: Documentation & Analysis

### Prompt 6.1: API Documentation
```
Generate comprehensive OpenAPI/Swagger documentation for SwiftPay APIs including:
1. All request/response schemas
2. Error codes and meanings
3. Rate limiting rules
4. Authentication/authorization requirements
5. Example requests and responses
6. Webhook specifications (if applicable)

Output: OpenAPI 3.0 specification (JSON/YAML)
```

### Prompt 6.2: Architecture Decision Record (ADR)
```
Write ADRs (Architecture Decision Records) for:
1. Why event-driven architecture (vs. synchronous RPC)?
2. Why pessimistic locking (vs. optimistic locking)?
3. Why Kafka partitions (vs. single queue)?
4. Why 24-hour idempotency TTL (vs. permanent)?

Format each as:
- Decision title
- Context
- Decision
- Consequences
- Alternatives considered
```

### Prompt 6.3: Runbook for Operators
```
Create operation runbooks for common scenarios:

1. Healthy Startup Verification
   - Health check endpoints
   - Expected log messages
   - Resource validation

2. Scale Up/Down Procedure
   - Readiness checks
   - Graceful shutdown
   - Consumer group rebalancing

3. Database Backup/Restore
   - Backup strategy
   - Recovery testing
   - Validation queries

4. Incident Response
   - Diagnosis steps
   - Escalation contacts
   - Communication templates

Format: Markdown with step-by-step procedures and checkpoints
```

## Prompt Set 7: Optimization Challenges

### Prompt 7.1: Latency Optimization Challenge
```
Mission: Reduce P95 latency from 500ms to 200ms.

Current constraints:
- Network hop count: 3 (client→gateway→ledger→db)
- Message serialization: Jackson (auto-detection)
- Database query: Indexed but not optimized
- Redis operations: Sequential checks

Approach:
1. Profile to identify bottleneck
2. Propose 3 optimizations (client visible, no breaking changes)
3. Estimate impact on latency and throughput
4. Trade-offs analysis

Provide: Benchmark code and results
```

### Prompt 7.2: Cost Optimization Challenge
```
Mission: Reduce cloud infrastructure costs by 30%.

Current infrastructure:
- 3 service instances (m5.large each)
- PostgreSQL instance (db.r5.2xlarge)
- Redis cluster (cache.r5.large)
- Kafka cluster (3 brokers, m5.xlarge)
- Network data transfer: 1TB/month

Options:
1. Right-size instances
2. Implement caching strategy
3. Database optimization
4. Auto-scaling configuration
5. Reserved instances

Provide:
- Cost breakdown before/after
- Implementation phases
- Risk assessment
- ROI calculation
```

### Prompt 7.3: Security Hardening Challenge
```
Mission: Achieve SOC 2 Type II compliance.

Current gaps:
1. No TLS between services
2. No authentication/authorization
3. No rate limiting
4. Limited audit logging
5. Secrets in environment variables

Design security improvements:
1. Internal service communication (mTLS)
2. API authentication (JWT/OAuth)
3. Rate limiting (per user/IP)
4. Comprehensive audit logging
5. Secrets management (Vault/KMS)

Provide:
- Implementation roadmap
- Code examples
- Testing strategy
- Compliance checklist
```

## Usage Guidelines

### For Hackathon Teams
1. **Pick your challenge:** Choose 1-3 prompts matching your expertise
2. **Time allocation:** 30-45 minutes analysis, 1-2 hours implementation
3. **Deliverables:** Code, documentation, or design diagram
4. **Validation:** Test against existing system; verify no regressions

### For AI Systems
1. **Context:** Always reference AI_PLAYBOOK.md
2. **Code style:** Follow existing Spring Boot conventions
3. **Testing:** Include unit/integration tests
4. **Documentation:** Add comments for non-obvious decisions

### Evaluation Criteria
- **Completeness:** Addresses all requirements
- **Quality:** Production-ready code
- **Performance:** Measurable improvement
- **Testing:** Comprehensive test coverage
- **Documentation:** Clear explanation of changes

---

**Version:** 1.0.0  
**Created:** 2026-09-16  
**Total Challenges:** 25+
