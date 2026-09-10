#!/bin/bash
set -e

echo "================================================"
echo "SwiftPay - Startup Script"
echo "================================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}Building Docker images...${NC}"
docker-compose build

echo -e "${BLUE}Starting services...${NC}"
docker-compose up -d

echo ""
echo -e "${YELLOW}Waiting for services to be healthy (~30 seconds)...${NC}"
sleep 30

echo ""
echo -e "${BLUE}Checking service health...${NC}"

# Check Transaction Gateway
if curl -s http://localhost:8080/health | grep -q "UP"; then
    echo -e "${GREEN}✓ Transaction Gateway is UP${NC}"
else
    echo -e "${YELLOW}⚠ Transaction Gateway is starting...${NC}"
fi

# Check Ledger Service
if curl -s http://localhost:8081/health | grep -q "UP"; then
    echo -e "${GREEN}✓ Ledger Service is UP${NC}"
else
    echo -e "${YELLOW}⚠ Ledger Service is starting...${NC}"
fi

# Check Analytics Worker
if curl -s http://localhost:8082/health | grep -q "UP"; then
    echo -e "${GREEN}✓ Analytics Worker is UP${NC}"
else
    echo -e "${YELLOW}⚠ Analytics Worker is starting...${NC}"
fi

echo ""
echo -e "${GREEN}================================================"
echo "Services started successfully!"
echo "================================================${NC}"
echo ""
echo "📊 Swagger UI Documentation:"
echo "   - Transaction Gateway: http://localhost:8080/swagger-ui.html"
echo "   - Ledger Service:      http://localhost:8081/swagger-ui.html"
echo "   - Analytics Worker:    http://localhost:8082/swagger-ui.html"
echo ""
echo "💾 Database:"
echo "   - PostgreSQL: localhost:5432"
echo "   - Credentials: swiftpay / swiftpay_password"
echo ""
echo "🔄 Message Broker:"
echo "   - Kafka: localhost:9092"
echo "   - Zookeeper: localhost:2181"
echo ""
echo "⚡ Cache:"
echo "   - Redis: localhost:6379"
echo ""
echo "📝 To view logs:"
echo "   docker-compose logs -f [service-name]"
echo ""
echo "🛑 To stop services:"
echo "   docker-compose down"
echo ""
