#!/bin/bash
set -e

echo "=================================================="
echo "SwiftPay - Load Test (250 TPS, 1M transactions)"
echo "=================================================="
echo ""

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Check if k6 is installed
if ! command -v k6 &> /dev/null; then
    echo -e "${RED}k6 is not installed${NC}"
    echo ""
    echo "Install k6:"
    echo "  macOS: brew install k6"
    echo "  Linux: sudo apt-get install k6"
    echo "  Windows: choco install k6"
    echo ""
    exit 1
fi

# Verify k6 version
K6_VERSION=$(k6 version)
echo -e "${GREEN}✓ k6 installed: ${K6_VERSION}${NC}"
echo ""

# Check if services are running
echo -e "${BLUE}Checking if services are running...${NC}"
GATEWAY=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/health)
LEDGER=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/health)

if [ "$GATEWAY" != "200" ] || [ "$LEDGER" != "200" ]; then
    echo -e "${RED}✗ Services are not running${NC}"
    echo "Start services with: docker-compose up -d"
    exit 1
fi
echo -e "${GREEN}✓ All services are healthy${NC}"
echo ""

# Create results directory
mkdir -p load-test-results

# Run load test
echo -e "${BLUE}Starting load test...${NC}"
echo "Configuration:"
echo "  - Target: 250 VUs (virtual users)"
echo "  - Duration: ~70 minutes (4166s at 250 TPS)"
echo "  - Total expected transactions: 1,000,000"
echo "  - Ramp-up: 3 minutes"
echo "  - Steady state: 60 minutes"
echo "  - Ramp-down: 3 minutes"
echo ""
echo "This will take approximately 70 minutes..."
echo ""

# Run the load test with output
k6 run \
  --out json=load-test-results/load-test-results.json \
  --summary-export=load-test-results/summary.json \
  load-test.js

TEST_RESULT=$?

if [ $TEST_RESULT -eq 0 ]; then
    echo -e "${GREEN}✓ Load test completed successfully${NC}"
else
    echo -e "${YELLOW}⚠ Load test completed with warnings${NC}"
fi

echo ""
echo "Results saved to: load-test-results/"
echo ""

# Parse and display results
if [ -f load-test-results/summary.json ]; then
    echo -e "${BLUE}Load Test Results Summary:${NC}"
    echo "===================================="
    python3 -c "
import json
import sys

try:
    with open('load-test-results/summary.json', 'r') as f:
        data = json.load(f)

    metrics = data.get('metrics', {})

    print('\nHTTP Metrics:')
    if 'http_reqs' in metrics:
        print(f'  Total Requests: {metrics[\"http_reqs\"].get(\"value\", 0)}')
    if 'http_req_failed' in metrics:
        print(f'  Failed Requests: {metrics[\"http_req_failed\"].get(\"value\", 0)}')

    if 'http_req_duration' in metrics:
        values = metrics['http_req_duration'].get('values', {})
        print('\nRequest Duration:')
        print(f'  Average: {values.get(\"avg\", 0):.2f}ms')
        print(f'  P50: {values.get(\"p50\", 0):.2f}ms')
        print(f'  P90: {values.get(\"p90\", 0):.2f}ms')
        print(f'  P95: {values.get(\"p95\", 0):.2f}ms')
        print(f'  P99: {values.get(\"p99\", 0):.2f}ms')
        print(f'  Max: {values.get(\"max\", 0):.2f}ms')

    print('\nCustom Metrics:')
    if 'successful_payments' in metrics:
        print(f'  Successful Payments: {metrics[\"successful_payments\"].get(\"value\", 0)}')
    if 'failed_payments' in metrics:
        print(f'  Failed Payments: {metrics[\"failed_payments\"].get(\"value\", 0)}')
    if 'duplicate_payments' in metrics:
        print(f'  Duplicate Payments: {metrics[\"duplicate_payments\"].get(\"value\", 0)}')
    if 'errors' in metrics:
        error_rate = metrics['errors'].get('value', 0) * 100
        print(f'  Error Rate: {error_rate:.2f}%')

    print('')
except Exception as e:
    print(f'Error parsing results: {e}', file=sys.stderr)
" 2>/dev/null || echo "  (Results JSON available in load-test-results/)"

fi

echo ""
echo "Next steps:"
echo "1. Review detailed results: load-test-results/load-test-results.json"
echo "2. Analyze performance: k6 inspect load-test-results/load-test-results.json"
echo "3. Check service logs: docker-compose logs -f"
echo ""

exit $TEST_RESULT
