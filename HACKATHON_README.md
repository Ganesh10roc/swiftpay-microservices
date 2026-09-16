# SwiftPay Hackathon - Complete Starter Kit

## 🎯 Welcome to the SwiftPay Hackathon!

This is a **production-ready fintech payment system** built with Java Spring Boot, PostgreSQL, Kafka, and Redis. Your mission: build, optimize, debug, or extend this real-world microservices architecture.

## 📦 What You're Getting

A complete Hackathon kit with:
- ✅ **3 production-grade microservices** (Transaction Gateway, Ledger Service, Analytics Worker)
- ✅ **Complete infrastructure** (PostgreSQL, Redis, Kafka, Docker Compose)
- ✅ **Full test suite** (Unit tests, Integration tests, Load tests)
- ✅ **25+ Hackathon challenges** across 7 categories
- ✅ **Network traffic captures** (PCAP files for analysis)
- ✅ **AI-friendly documentation** (AI Playbook for code analysis)
- ✅ **End-to-end validation guide** (Complete testing checklist)

## 🚀 Quick Start (5 minutes)

### 1. Prerequisites
```bash
# Required:
- Java 21 JDK
- Maven 3.8+
- Docker 20.10+
- Docker Compose 1.29+

# Optional:
- k6 (for load testing)
- Python 3.8+ (for PCAP generation)
- Wireshark (for network analysis)
```

### 2. Build & Start
```bash
# Build all services
mvn clean package

# Start infrastructure (PostgreSQL, Redis, Kafka, Services)
docker-compose up -d

# Wait for startup (~45 seconds)
sleep 45

# Verify health
curl http://localhost:8080/health
curl http://localhost:8081/health
curl http://localhost:8082/health
```

### 3. Test Payment Flow
```bash
# Create a payment
curl -X POST http://localhost:8080/v1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "transaction_id": "test-001",
    "sender_id": "user001",
    "receiver_id": "user002",
    "amount": 100.00,
    "currency": "USD"
  }'

# Check account balance
curl http://localhost:8081/v1/ledger/account/user001

# View analytics
curl http://localhost:8082/v1/analytics/metrics/hour
```

## 📚 Documentation Guide

| Document | Purpose | Best For |
|----------|---------|----------|
| [README.md](README.md) | Project overview & API reference | Quick reference, API endpoints |
| [ARCHITECTURE.md](ARCHITECTURE.md) | System design & components | Understanding design decisions |
| [IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md) | Build & deployment instructions | Getting started, troubleshooting |
| [AI_PLAYBOOK.md](AI_PLAYBOOK.md) | AI system guide for code analysis | Using AI to analyze/optimize code |
| [HACKATHON_PROMPTS.md](HACKATHON_PROMPTS.md) | 25+ challenges & prompts | Picking your Hackathon task |
| [HACKATHON_END_TO_END.md](HACKATHON_END_TO_END.md) | Complete testing guide | Running full validation |
| [PACKET_CAPTURE.pcap.README.md](PACKET_CAPTURE.pcap.README.md) | Network analysis guide | Analyzing network traffic |
| [VALIDATION_CHECKLIST.md](VALIDATION_CHECKLIST.md) | Pre-flight checks | Quick validation before starting |

## 🎓 Learning Path

### For Beginners (Microservices)
1. Read: [ARCHITECTURE.md](ARCHITECTURE.md)
2. Explore: Service code in `transaction-gateway/`, `ledger-service/`, `analytics-worker/`
3. Try: [HACKATHON_PROMPTS.md](HACKATHON_PROMPTS.md) → "Code Review & Optimization" (Prompt 1.1)

### For Intermediate (System Design)
1. Read: [AI_PLAYBOOK.md](AI_PLAYBOOK.md)
2. Explore: Database schema in `init-db.sql`
3. Try: [HACKATHON_PROMPTS.md](HACKATHON_PROMPTS.md) → "System Design" section

### For Advanced (Performance & Scalability)
1. Read: [PACKET_CAPTURE.pcap.README.md](PACKET_CAPTURE.pcap.README.md)
2. Run: `python3 generate-pcap.py PACKET_CAPTURE.pcap 1000`
3. Try: [HACKATHON_PROMPTS.md](HACKATHON_PROMPTS.md) → "Optimization Challenges"

## 🎯 Hackathon Challenges (25+ Available)

### By Difficulty Level

**Easy (30 min - 1 hour)**
- Code Review & Optimization: Prompt 1.1 (Performance Audit)
- Testing & QA: Prompt 5.1 (Load Test Scenario)
- Documentation: Prompt 6.1 (API Documentation)

**Medium (1 - 2 hours)**
- Feature Development: Prompt 2.1 (Transaction Rollback)
- System Design: Prompt 3.1 (Scalability Assessment)
- Troubleshooting: Prompt 4.1 (Production Incident)

**Hard (2 - 4 hours)**
- Feature Development: Prompt 2.2 (Real-Time Notifications)
- System Design: Prompt 3.3 (Compliance & Audit)
- Optimization Challenges: Prompt 7.3 (Security Hardening)

### By Category

| Category | Prompts | Focus |
|----------|---------|-------|
| Code Review | 1.1, 1.2, 1.3 | Performance, Concurrency, Error Handling |
| Features | 2.1, 2.2, 2.3 | Rollbacks, Real-time, Analytics |
| System Design | 3.1, 3.2, 3.3 | Scalability, Multi-currency, Compliance |
| Troubleshooting | 4.1, 4.2, 4.3 | Incident, Data Consistency, Performance |
| Testing | 5.1, 5.2, 5.3 | Load Test, Chaos, Integration |
| Documentation | 6.1, 6.2, 6.3 | API Docs, ADRs, Runbooks |
| Optimization | 7.1, 7.2, 7.3 | Latency, Cost, Security |

## 🔍 Key Architecture Concepts

### Event-Driven Pattern
```
Client → Transaction Gateway → Kafka → Ledger Service → PostgreSQL
                                    ↓
                            Analytics Worker → Analytics
```

### Three-Tier Service Design
1. **Transaction Gateway** (Port 8080)
   - REST API for payments
   - Idempotency via Redis
   - Event publishing

2. **Ledger Service** (Port 8081)
   - Atomic debit/credit
   - Pessimistic locking
   - Transaction processing

3. **Analytics Worker** (Port 8082)
   - Real-time metrics
   - Transaction aggregation
   - Historical reporting

### Core Technologies
- **Language:** Java 21
- **Framework:** Spring Boot 3.3.0
- **Database:** PostgreSQL 16
- **Message Queue:** Apache Kafka 7.5.0
- **Cache:** Redis 7
- **Container:** Docker & Docker Compose

## 📊 Performance Targets

| Metric | Target |
|--------|--------|
| Throughput | 250 TPS |
| P95 Latency | <500ms |
| P99 Latency | <1000ms |
| Error Rate | <1% |
| Idempotency TTL | 24 hours |
| Kafka Partitions | 3 per topic |
| Database Connections | 20 per service |

## 🛠️ Common Tasks

### Generate PCAP Network Traffic
```bash
# Generate 500 transactions worth of network traffic
python3 generate-pcap.py PACKET_CAPTURE.pcap 500

# Analyze with Wireshark
wireshark PACKET_CAPTURE.pcap

# Or analyze with tshark
tshark -r PACKET_CAPTURE.pcap -Y "http.request" -T fields
```

### Run Load Test
```bash
# Install k6
# macOS: brew install k6
# Linux: sudo apt-get install k6

# Run load test (250 concurrent users, ~1 hour for 1M transactions)
k6 run load-test.js

# Custom settings (100 users, 1 minute)
k6 run -u 100 -d 1m load-test.js
```

### Access Database
```bash
# PostgreSQL
docker exec -it swiftpay-postgres psql -U swiftpay -d swiftpay

# Example queries:
SELECT * FROM transactions LIMIT 10;
SELECT user_id, balance FROM accounts;
SELECT * FROM ledger_entries ORDER BY created_at DESC LIMIT 20;
```

### View Service Logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f transaction-gateway
docker-compose logs -f ledger-service
docker-compose logs -f analytics-worker
```

## 🏆 Evaluation Criteria

Your Hackathon submission will be evaluated on:

1. **Correctness** (30%)
   - Does it solve the challenge?
   - Are there regressions?
   - Is the implementation sound?

2. **Code Quality** (25%)
   - Is it production-ready?
   - Is it well-tested?
   - Are best practices followed?

3. **Performance** (25%)
   - Is it efficient?
   - Are metrics improved?
   - Any performance regressions?

4. **Documentation** (20%)
   - Is the change well-documented?
   - Are trade-offs explained?
   - Is it maintainable?

## ✅ Before You Start

1. Run the validation checklist:
   ```bash
   cat VALIDATION_CHECKLIST.md
   ```

2. Review the end-to-end guide:
   ```bash
   cat HACKATHON_END_TO_END.md
   ```

3. Pick your challenge:
   ```bash
   cat HACKATHON_PROMPTS.md
   ```

4. Study the architecture:
   ```bash
   cat AI_PLAYBOOK.md
   ```

## 🐛 Troubleshooting

### Services Won't Start
- Check Docker is running
- Check ports 8080, 8081, 8082, 5432, 6379, 9092 are available
- Review logs: `docker-compose logs`

### Tests Fail
- Ensure Java 21+: `java -version`
- Ensure Maven 3.8+: `mvn -version`
- Clean rebuild: `mvn clean package`

### High Latency
- Check Docker resource allocation
- Monitor with `docker stats`
- Review database indexes

### Payment Stuck in PENDING
- Check Kafka consumer lag: `docker-compose logs ledger-service`
- Verify database connections not exhausted
- Check Redis is accessible

See [HACKATHON_END_TO_END.md](HACKATHON_END_TO_END.md) for detailed troubleshooting.

## 📞 Support

- **Architecture Questions:** Read [AI_PLAYBOOK.md](AI_PLAYBOOK.md)
- **Build Issues:** Read [IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md)
- **API Usage:** Read [README.md](README.md) API Endpoints section
- **Challenge Help:** Read relevant prompt in [HACKATHON_PROMPTS.md](HACKATHON_PROMPTS.md)
- **Network Analysis:** Read [PACKET_CAPTURE.pcap.README.md](PACKET_CAPTURE.pcap.README.md)

## 📈 What You'll Learn

By working through SwiftPay challenges, you'll gain experience with:

✓ Microservices architecture  
✓ Event-driven systems (Kafka)  
✓ Database transactions & locking  
✓ API design & REST principles  
✓ Performance optimization  
✓ Load testing & benchmarking  
✓ Network analysis (PCAP/Wireshark)  
✓ Docker & containerization  
✓ CI/CD pipeline (GitHub Actions)  
✓ Production debugging  

## 🚀 Next Steps

1. **Setup:** Follow Quick Start above (~5 min)
2. **Validate:** Run VALIDATION_CHECKLIST.md (~15 min)
3. **Choose Challenge:** Pick from HACKATHON_PROMPTS.md
4. **Implement:** Code your solution (1-4 hours depending on difficulty)
5. **Test:** Use HACKATHON_END_TO_END.md to verify (~10 min)
6. **Submit:** Show your results!

## 📝 Challenge Timeline

**Suggested Approach:**
- **Days 1-2:** Setup, validation, and easy challenges
- **Days 2-3:** Medium challenges and deep architecture study
- **Days 3-4:** Hard challenges, optimizations, and polishing
- **Day 4:** Final testing and documentation

## 🎉 Good Luck!

You have everything you need to succeed. The SwiftPay architecture is solid, the documentation is comprehensive, and the challenges are well-designed to teach real-world system engineering.

**Pick a challenge and build something great!** 🚀

---

**Hackathon Kit Version:** 1.0.0  
**SwiftPay Version:** 1.0.0  
**Last Updated:** 2026-09-16  
**Status:** Production Ready ✓

**Got feedback?** Improve these docs and share with the community!
