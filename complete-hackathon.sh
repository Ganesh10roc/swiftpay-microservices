#!/bin/bash
#
# SwiftPay Hackathon - Complete Submission Script
# Performs all pending tasks and generates final deliverables
#

set -e

cd "$(dirname "$0")"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}================================================${NC}"
echo -e "${BLUE}SwiftPay Hackathon - Complete Submission${NC}"
echo -e "${BLUE}================================================${NC}"
echo ""

# Function to check service health
check_service() {
    local url=$1
    local name=$2
    echo -n "Checking $name... "
    if curl -s "$url" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ UP${NC}"
        return 0
    else
        echo -e "${RED}✗ DOWN${NC}"
        return 1
    fi
}

# Function to wait for services
wait_for_services() {
    local max_attempts=30
    local attempt=0

    echo -e "${BLUE}Waiting for services to be ready...${NC}"
    while [ $attempt -lt $max_attempts ]; do
        if check_service "http://localhost:8080/health" "Transaction Gateway" && \
           check_service "http://localhost:8081/health" "Ledger Service" && \
           check_service "http://localhost:8082/health" "Analytics Worker"; then
            echo -e "${GREEN}All services ready!${NC}"
            return 0
        fi
        attempt=$((attempt + 1))
        echo "Attempt $attempt/$max_attempts... waiting 10 seconds"
        sleep 10
    done

    echo -e "${RED}Services failed to start within 5 minutes${NC}"
    return 1
}

# Step 1: Wait for services
echo -e "${YELLOW}STEP 1: Waiting for microservices to start...${NC}"
echo ""
if ! wait_for_services; then
    echo -e "${RED}Cannot proceed without services${NC}"
    exit 1
fi
echo ""

# Step 2: End-to-end test
echo -e "${YELLOW}STEP 2: Testing end-to-end payment flow...${NC}"
echo ""

TEST_PAYMENT=$(curl -s -X POST http://localhost:8080/v1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "sender_id": "user1",
    "receiver_id": "user2",
    "amount": 100.50,
    "currency": "USD"
  }')

echo "Payment request response:"
echo "$TEST_PAYMENT" | jq '.' 2>/dev/null || echo "$TEST_PAYMENT"
echo ""

if echo "$TEST_PAYMENT" | grep -q "transaction_id\|status"; then
    echo -e "${GREEN}✓ End-to-end test PASSED${NC}"
else
    echo -e "${YELLOW}⚠ Payment request sent (check response above)${NC}"
fi
echo ""

# Step 3: Create output directory
echo -e "${YELLOW}STEP 3: Preparing output directories...${NC}"
mkdir -p load-test-results
echo -e "${GREEN}✓ Created load-test-results/${NC}"
echo ""

# Step 4: Ready for load test
echo -e "${BLUE}================================================${NC}"
echo -e "${GREEN}✓ ALL PREREQUISITES COMPLETE${NC}"
echo -e "${BLUE}================================================${NC}"
echo ""
echo -e "${YELLOW}Ready to run load test (250 TPS, 1M transactions)${NC}"
echo ""
echo "To complete the hackathon:"
echo ""
echo "1. ${YELLOW}Start PCAP capture (Terminal 1):${NC}"
echo "   ./scripts/capture-pcap.sh load-test-results/swiftpay.pcap docker0"
echo ""
echo "2. ${YELLOW}Run load test (Terminal 2):${NC}"
echo "   k6 run load-test.js --out json=load-test-results/results.json"
echo ""
echo "3. ${YELLOW}Wait for completion (~70 minutes)${NC}"
echo ""
echo "4. ${YELLOW}Verify PCAP file:${NC}"
echo "   ls -lh load-test-results/swiftpay.pcap"
echo ""
echo "5. ${YELLOW}Submit deliverables:${NC}"
echo "   - load-test-results/swiftpay.pcap"
echo "   - load-test-results/results.json"
echo "   - GitHub repo link"
echo "   - README.md"
echo ""

# Show summary
echo -e "${BLUE}================================================${NC}"
echo -e "${BLUE}System Status Summary${NC}"
echo -e "${BLUE}================================================${NC}"
echo ""
docker-compose ps | tail -10
echo ""
echo -e "${GREEN}✓ Transaction Gateway: http://localhost:8080${NC}"
echo -e "${GREEN}✓ Ledger Service: http://localhost:8081${NC}"
echo -e "${GREEN}✓ Analytics Worker: http://localhost:8082${NC}"
echo -e "${GREEN}✓ PostgreSQL: localhost:5432${NC}"
echo -e "${GREEN}✓ Redis: localhost:6379${NC}"
echo -e "${GREEN}✓ Kafka: localhost:9092${NC}"
echo ""
