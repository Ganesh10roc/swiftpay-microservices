#!/bin/bash
#
# Execute SwiftPay Load Test with Real PCAP Capture
# Generates actual network traffic capture for hackathon submission
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
echo -e "${BLUE}SwiftPay Load Test - Real PCAP Capture${NC}"
echo -e "${BLUE}250 TPS × 1,000,000 Transactions${NC}"
echo -e "${BLUE}================================================${NC}"
echo ""

# Wait for services
echo -e "${YELLOW}Waiting for services to be healthy...${NC}"
for i in {1..30}; do
    if curl -s http://localhost:8080/health > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Services ready${NC}"
        break
    fi
    echo "Attempt $i/30... waiting"
    sleep 5
done
echo ""

# Test connectivity
echo -e "${YELLOW}Testing payment endpoint...${NC}"
curl -s -X POST http://localhost:8080/v1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "sender_id": "test_user_1",
    "receiver_id": "test_user_2",
    "amount": 50.00,
    "currency": "USD"
  }' | jq '.' 2>/dev/null || echo "Test request sent"
echo ""

# Create output directory
mkdir -p load-test-results
echo -e "${GREEN}✓ Output directory ready${NC}"
echo ""

# Show instructions
echo -e "${BLUE}================================================${NC}"
echo -e "${YELLOW}READY TO START LOAD TEST${NC}"
echo -e "${BLUE}================================================${NC}"
echo ""
echo -e "${YELLOW}This will generate a real PCAP file by capturing${NC}"
echo -e "${YELLOW}actual network traffic during the load test.${NC}"
echo ""
echo "Next steps:"
echo ""
echo "1. ${YELLOW}Terminal 1 - Start PCAP capture:${NC}"
echo "   ./scripts/capture-pcap.sh load-test-results/swiftpay.pcap docker0"
echo ""
echo "2. ${YELLOW}Terminal 2 - When prompted, run load test:${NC}"
echo "   k6 run load-test.js --out json=load-test-results/results.json"
echo ""
echo "3. ${YELLOW}Wait ~70 minutes for completion${NC}"
echo ""
echo "4. ${YELLOW}Verify PCAP file:${NC}"
echo "   ls -lh load-test-results/swiftpay.pcap"
echo "   tshark -r load-test-results/swiftpay.pcap -z io,stat,1"
echo ""
echo -e "${YELLOW}PCAP File Details:${NC}"
echo "   - Format: Wireshark-compatible (.pcap)"
echo "   - Expected size: ~2.5 GB"
echo "   - Contains: Real network traffic from 1M transactions"
echo "   - Ports monitored: 8080, 8081, 8082, 9092, 5432, 6379"
echo ""
echo -e "${GREEN}Status: Ready for hackathon submission${NC}"
echo ""
