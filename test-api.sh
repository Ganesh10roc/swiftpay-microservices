#!/bin/bash

echo "================================================"
echo "SwiftPay - API Test Script"
echo "================================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

BASE_URL="http://localhost:8080"
LEDGER_URL="http://localhost:8081"
ANALYTICS_URL="http://localhost:8082"

# Generate unique transaction ID
TRANSACTION_ID="txn-$(date +%s)-$RANDOM"

echo -e "${BLUE}Test 1: Health Check${NC}"
echo "--------------------"
if curl -s $BASE_URL/health | grep -q "UP"; then
    echo -e "${GREEN}✓ Transaction Gateway is healthy${NC}"
else
    echo -e "${RED}✗ Transaction Gateway is not responding${NC}"
    exit 1
fi

if curl -s $LEDGER_URL/health | grep -q "UP"; then
    echo -e "${GREEN}✓ Ledger Service is healthy${NC}"
else
    echo -e "${RED}✗ Ledger Service is not responding${NC}"
    exit 1
fi
echo ""

echo -e "${BLUE}Test 2: Get Initial Account Balance${NC}"
echo "------------------------------------"
echo "Getting balance for user001..."
curl -s -X GET "$LEDGER_URL/v1/ledger/account/user001" \
  -H "Content-Type: application/json" | jq '.'
echo ""
echo ""

echo -e "${BLUE}Test 3: Initiate Payment${NC}"
echo "------------------------"
echo "Transaction ID: $TRANSACTION_ID"
echo "Sending: user001 → user002, amount: 500.00"
RESPONSE=$(curl -s -X POST "$BASE_URL/v1/payments" \
  -H "Content-Type: application/json" \
  -d "{
    \"transaction_id\": \"$TRANSACTION_ID\",
    \"sender_id\": \"user001\",
    \"receiver_id\": \"user002\",
    \"amount\": 500.00,
    \"currency\": \"USD\"
  }")

echo "$RESPONSE" | jq '.'

# Extract status from response
STATUS=$(echo "$RESPONSE" | jq -r '.data.status' 2>/dev/null)
echo -e "\nTransaction Status: $STATUS"
echo ""

echo -e "${BLUE}Test 4: Wait for Processing${NC}"
echo "----------------------------"
echo "Waiting 5 seconds for Kafka processing..."
sleep 5
echo ""

echo -e "${BLUE}Test 5: Get Updated Account Balance${NC}"
echo "-----------------------------------"
echo "Getting balance for user001 (should be decreased)..."
BALANCE=$(curl -s -X GET "$LEDGER_URL/v1/ledger/account/user001" \
  -H "Content-Type: application/json" | jq '.data.balance')
echo "user001 Balance: $BALANCE"
echo ""

echo "Getting balance for user002 (should be increased)..."
curl -s -X GET "$LEDGER_URL/v1/ledger/account/user002" \
  -H "Content-Type: application/json" | jq '.'
echo ""

echo -e "${BLUE}Test 6: Get Transaction History${NC}"
echo "-------------------------------"
echo "Transaction history for user001..."
curl -s -X GET "$LEDGER_URL/v1/ledger/history/user001?page=0&size=5" \
  -H "Content-Type: application/json" | jq '.'
echo ""

echo -e "${BLUE}Test 7: Get Analytics Metrics${NC}"
echo "-----------------------------"
echo "Metrics for last hour..."
curl -s -X GET "$ANALYTICS_URL/v1/analytics/metrics/hour" \
  -H "Content-Type: application/json" | jq '.'
echo ""

echo -e "${BLUE}Test 8: Duplicate Transaction Test (Idempotency)${NC}"
echo "------------------------------------------------"
echo "Sending same transaction again (should be rejected)..."
curl -s -X POST "$BASE_URL/v1/payments" \
  -H "Content-Type: application/json" \
  -d "{
    \"transaction_id\": \"$TRANSACTION_ID\",
    \"sender_id\": \"user001\",
    \"receiver_id\": \"user002\",
    \"amount\": 500.00,
    \"currency\": \"USD\"
  }" | jq '.'
echo ""

echo -e "${BLUE}Test 9: Get Transaction Details${NC}"
echo "-------------------------------"
echo "Getting details for transaction: $TRANSACTION_ID"
curl -s -X GET "$BASE_URL/v1/payments/$TRANSACTION_ID" \
  -H "Content-Type: application/json" | jq '.'
echo ""

echo -e "${GREEN}================================================"
echo "All tests completed!"
echo "================================================${NC}"
echo ""
echo "📊 API Documentation:"
echo "   - Transaction Gateway: http://localhost:8080/swagger-ui.html"
echo "   - Ledger Service:      http://localhost:8081/swagger-ui.html"
echo "   - Analytics Worker:    http://localhost:8082/swagger-ui.html"
echo ""
