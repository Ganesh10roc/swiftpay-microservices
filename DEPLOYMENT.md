# SwiftPay - Deployment Guide

## Local Development

### Prerequisites
```bash
docker --version          # Docker 20.10+
docker-compose --version  # Docker Compose 2.0+
```

### Development Setup
```bash
# Clone and navigate
git clone https://github.com/Ganesh10roc/swiftpay-microservices.git
cd swiftpay-microservices

# Start services
docker-compose up -d

# View logs
docker-compose logs -f transaction-gateway
docker-compose logs -f ledger-service
docker-compose logs -f analytics-worker

# Stop services
docker-compose down
```

### Database Setup
PostgreSQL automatically initializes via `init-db.sql`:
- Creates `swiftpay` database
- Creates tables: `transactions`, `accounts`, `ledger_entries`, `payment_analytics`
- Inserts sample data if needed

### Configuration

#### Transaction Gateway (`transaction-gateway/src/main/resources/application.yml`)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/swiftpay
    username: swiftpay
    password: swiftpay_password
  redis:
    host: redis
    port: 6379
  kafka:
    bootstrap-servers: kafka:29092
```

#### Ledger Service (`ledger-service/src/main/resources/application.yml`)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/swiftpay
  kafka:
    bootstrap-servers: kafka:29092
    consumer:
      group-id: ledger-service-group
      auto-offset-reset: earliest
```

#### Analytics Worker (`analytics-worker/src/main/resources/application.yml`)
```yaml
spring:
  kafka:
    bootstrap-servers: kafka:29092
    consumer:
      group-id: analytics-worker-group
```

---

## Docker Deployment

### Build Docker Images
```bash
# Build all services
docker-compose build

# Build specific service
docker-compose build transaction-gateway
```

### Production Deployment Checklist

- [ ] Enable authentication (OAuth 2.0 / API Keys)
- [ ] Set up SSL/TLS certificates
- [ ] Configure rate limiting
- [ ] Enable request logging and monitoring
- [ ] Set up log aggregation (ELK, Splunk)
- [ ] Configure alerting (Prometheus + AlertManager)
- [ ] Set up database backups
- [ ] Enable connection pooling tuning
- [ ] Configure auto-scaling policies
- [ ] Set up health checks and readiness probes

### Kubernetes Deployment

Deploy to K8s cluster using Helm or raw manifests:

```bash
# Example K8s deployment
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/postgres-pvc.yaml
kubectl apply -f k8s/postgres-deployment.yaml
kubectl apply -f k8s/redis-deployment.yaml
kubectl apply -f k8s/kafka-deployment.yaml
kubectl apply -f k8s/transaction-gateway-deployment.yaml
kubectl apply -f k8s/ledger-service-deployment.yaml
kubectl apply -f k8s/analytics-worker-deployment.yaml
kubectl apply -f k8s/services.yaml
kubectl apply -f k8s/ingress.yaml
```

---

## Monitoring & Observability

### Health Checks
```bash
# Check all services
curl http://localhost:8080/health
curl http://localhost:8081/health
curl http://localhost:8082/health
```

### Logs
```bash
# View real-time logs
docker-compose logs -f

# Follow specific service
docker-compose logs -f transaction-gateway

# Export logs
docker-compose logs > swiftpay.log
```

### Metrics
- Spring Boot Actuator: `/actuator/metrics`
- Prometheus: Configure in `application.yml`
- Grafana: Visualize metrics

### Load Testing
```bash
# Generate 1M transaction load test
python generate-submission.py

# With K6
k6 run load-test.js

# With custom configuration
k6 run load-test.js -e TARGET_ENDPOINT=http://your-endpoint
```

---

## Troubleshooting

### Services Won't Start

**Issue:** Kafka health check failing
**Solution:** 
```bash
docker-compose restart kafka
# Wait 30 seconds
docker-compose logs kafka | grep ERROR
```

**Issue:** Redis connection refused
**Solution:**
```bash
docker-compose restart redis
docker exec swiftpay-redis redis-cli ping
```

**Issue:** Database migrations failing
**Solution:**
```bash
docker-compose exec postgres psql -U swiftpay -d swiftpay -c "\dt"
# Verify tables exist
```

### High Latency

1. Check PostgreSQL connections: `ps aux | grep postgres`
2. Check Redis memory: `redis-cli info memory`
3. Check Kafka lag: `kafka-consumer-groups --describe --group ledger-service-group`
4. Monitor CPU/Memory: `docker stats`

### Transaction Failures

- Check transaction gateway logs for validation errors
- Verify Redis is accessible: `docker exec swiftpay-redis redis-cli ping`
- Verify Kafka topics exist: `docker exec swiftpay-kafka kafka-topics --list --bootstrap-server localhost:9092`

---

## Performance Tuning

### PostgreSQL
```sql
-- Increase connection pool
-- In application.yml: hikari.maximum-pool-size: 50

-- Add indexes
CREATE INDEX idx_transactions_sender ON transactions(sender_id);
CREATE INDEX idx_transactions_receiver ON transactions(receiver_id);
CREATE INDEX idx_accounts_user ON accounts(user_id);
```

### Redis
```bash
# Increase memory
docker exec swiftpay-redis redis-cli CONFIG SET maxmemory 2gb
```

### Kafka
```bash
# Increase partitions for parallelism
docker exec swiftpay-kafka kafka-topics --alter \
  --topic payment-initiated \
  --partitions 10
```

---

## Scaling

### Horizontal Scaling
- Deploy multiple instances of each service behind a load balancer
- Use Kubernetes for auto-scaling based on CPU/memory

### Database Scaling
- Read replicas for query offloading
- Sharding for large datasets

### Cache Scaling
- Redis cluster mode for distributed caching
- Implement cache warming strategies

## Backup & Recovery

```bash
# PostgreSQL backup
docker exec swiftpay-postgres pg_dump -U swiftpay swiftpay > backup.sql

# Restore
docker exec -i swiftpay-postgres psql -U swiftpay swiftpay < backup.sql

# Redis backup
docker exec swiftpay-redis redis-cli BGSAVE
docker cp swiftpay-redis:/data/dump.rdb ./redis-backup.rdb
```
