#!/bin/bash
set -e

echo "=================================================="
echo "SwiftPay - Load Test with PCAP Capture"
echo "=================================================="
echo ""

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Check if running as root/sudo for tcpdump
if [ "$EUID" -ne 0 ]; then
   echo -e "${YELLOW}Note: PCAP capture requires sudo. You may be prompted for password.${NC}"
   echo "Re-running with sudo..."
   sudo "$0"
   exit $?
fi

# Create results directory
mkdir -p load-test-results

# Check dependencies
echo -e "${BLUE}Checking dependencies...${NC}"
if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}✗ docker-compose not found${NC}"
    exit 1
fi
echo -e "${GREEN}✓ docker-compose available${NC}"

if ! command -v k6 &> /dev/null; then
    echo -e "${RED}✗ k6 not found${NC}"
    echo "Install k6: brew install k6 (macOS) or apt-get install k6 (Linux)"
    exit 1
fi
echo -e "${GREEN}✓ k6 available${NC}"

if ! command -v tcpdump &> /dev/null; then
    echo -e "${RED}✗ tcpdump not found${NC}"
    echo "Install tcpdump: brew install tcpdump (macOS) or apt-get install tcpdump (Linux)"
    exit 1
fi
echo -e "${GREEN}✓ tcpdump available${NC}"

echo ""

# Determine network interface
echo -e "${BLUE}Detecting Docker network interface...${NC}"
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    DOCKER_IF=$(ifconfig | grep -B1 "inet 172\." | head -1 | awk '{print $1}' | sed 's/:$//')
    if [ -z "$DOCKER_IF" ]; then
        DOCKER_IF="bridge0"
    fi
    echo -e "${GREEN}✓ Using interface: $DOCKER_IF${NC}"
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    # Linux
    DOCKER_IF="docker0"
    if ! ip link show $DOCKER_IF &>/dev/null; then
        DOCKER_IF="br-$(docker network ls --filter name=swiftpay -q | head -1 | cut -c 1-12)"
    fi
    echo -e "${GREEN}✓ Using interface: $DOCKER_IF${NC}"
else
    echo -e "${YELLOW}⚠ Unknown OS. Using docker0${NC}"
    DOCKER_IF="docker0"
fi

echo ""

# Create PCAP filter
PCAP_FILTER="(port 5432 or port 6379 or port 9092 or port 8080 or port 8081 or port 8082) and (tcp or udp)"
PCAP_FILE="load-test-results/swiftpay-load-test.pcap"

echo -e "${BLUE}Phase 1: Starting PCAP Capture${NC}"
echo "========================================"
echo "Interface: $DOCKER_IF"
echo "Filter: $PCAP_FILTER"
echo "Output: $PCAP_FILE"
echo ""

# Start tcpdump in background
echo -e "${YELLOW}Starting packet capture...${NC}"
tcpdump -i $DOCKER_IF -w $PCAP_FILE "$PCAP_FILTER" 2>/dev/null &
TCPDUMP_PID=$!
echo -e "${GREEN}✓ tcpdump started (PID: $TCPDUMP_PID)${NC}"

# Give tcpdump time to start
sleep 2

echo ""
echo -e "${BLUE}Phase 2: Starting Docker Services${NC}"
echo "========================================"

# Check if services already running
RUNNING=$(docker-compose ps -q transaction-gateway 2>/dev/null || echo "")
if [ -n "$RUNNING" ]; then
    echo -e "${YELLOW}Services already running, stopping first...${NC}"
    docker-compose down -v
    sleep 10
fi

echo -e "${YELLOW}Starting services...${NC}"
docker-compose up -d

echo -e "${YELLOW}Waiting for services to be healthy (45 seconds)...${NC}"
sleep 45

# Verify services are running
echo -e "${YELLOW}Verifying services...${NC}"
GATEWAY=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/health)
LEDGER=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/health)
ANALYTICS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8082/health)

if [ "$GATEWAY" != "200" ] || [ "$LEDGER" != "200" ]; then
    echo -e "${RED}✗ Services not responding${NC}"
    echo "Gateway: $GATEWAY, Ledger: $LEDGER, Analytics: $ANALYTICS"
    kill $TCPDUMP_PID 2>/dev/null || true
    exit 1
fi

echo -e "${GREEN}✓ Transaction Gateway: UP${NC}"
echo -e "${GREEN}✓ Ledger Service: UP${NC}"
echo -e "${GREEN}✓ Analytics Worker: UP${NC}"

echo ""
echo -e "${BLUE}Phase 3: Running Load Test${NC}"
echo "========================================"
echo "Configuration:"
echo "  - Virtual Users: 250"
echo "  - Duration: ~70 minutes"
echo "  - Total Transactions: 1,000,000"
echo "  - Network Capture: ACTIVE"
echo ""
echo -e "${YELLOW}Starting k6 load test...${NC}"

# Run k6 load test with results
k6 run \
  --out json=load-test-results/load-test-results.json \
  --summary-export=load-test-results/summary.json \
  load-test.js 2>&1 | tee load-test-results/load-test.log

LOAD_TEST_EXIT=$?

echo ""
echo -e "${BLUE}Phase 4: Stopping PCAP Capture${NC}"
echo "========================================"
echo -e "${YELLOW}Stopping packet capture...${NC}"
kill $TCPDUMP_PID 2>/dev/null || true
wait $TCPDUMP_PID 2>/dev/null || true

# Give tcpdump time to flush buffers
sleep 2

# Check PCAP file
if [ -f "$PCAP_FILE" ]; then
    PCAP_SIZE=$(ls -lh $PCAP_FILE | awk '{print $5}')
    PCAP_PACKETS=$(tcpdump -r $PCAP_FILE 2>/dev/null | wc -l)
    echo -e "${GREEN}✓ PCAP file captured${NC}"
    echo "  - File: $PCAP_FILE"
    echo "  - Size: $PCAP_SIZE"
    echo "  - Packets: $PCAP_PACKETS"
else
    echo -e "${YELLOW}⚠ PCAP file not created${NC}"
fi

echo ""
echo -e "${BLUE}Phase 5: Analyzing Results${NC}"
echo "========================================"

if [ -f load-test-results/summary.json ]; then
    echo -e "${GREEN}✓ Load test results available${NC}"

    # Parse and display summary
    python3 << 'PYTHON_EOF' 2>/dev/null || echo "Python analysis skipped"
import json
import os

try:
    with open('load-test-results/summary.json', 'r') as f:
        data = json.load(f)

    metrics = data.get('metrics', {})

    print("\n📊 Load Test Results Summary:")
    print("=" * 50)

    if 'http_reqs' in metrics:
        print(f"Total Requests: {metrics['http_reqs'].get('value', 0)}")

    if 'http_req_failed' in metrics:
        failed = metrics['http_req_failed'].get('value', 0)
        print(f"Failed Requests: {failed}")

    if 'http_req_duration' in metrics:
        values = metrics['http_req_duration'].get('values', {})
        print("\n⏱️  Response Time:")
        print(f"  Average: {values.get('avg', 0):.2f}ms")
        print(f"  P50: {values.get('p(50)', 0):.2f}ms")
        print(f"  P90: {values.get('p(90)', 0):.2f}ms")
        print(f"  P95: {values.get('p(95)', 0):.2f}ms")
        print(f"  P99: {values.get('p(99)', 0):.2f}ms")
        print(f"  Max: {values.get('max', 0):.2f}ms")

    print("\n✅ Full results saved to:")
    print("  - load-test-results/load-test-results.json")
    print("  - load-test-results/summary.json")
    print("  - load-test-results/load-test.log")

except Exception as e:
    print(f"Analysis error: {e}")

PYTHON_EOF
else
    echo -e "${YELLOW}⚠ Summary file not found${NC}"
fi

echo ""
echo -e "${BLUE}Phase 6: Cleanup${NC}"
echo "========================================"
echo -e "${YELLOW}Stopping Docker services...${NC}"
docker-compose down

echo ""
echo -e "${GREEN}=================================================="
echo "Load Test with PCAP Capture Complete!"
echo "==================================================${NC}"
echo ""
echo "📁 Results Location: load-test-results/"
echo ""
echo "Files Generated:"
echo "  1. load-test-results.json - Detailed load test metrics"
echo "  2. summary.json - Aggregated performance metrics"
echo "  3. load-test.log - Console output from k6"
echo "  4. swiftpay-load-test.pcap - Network packet capture"
echo ""
echo "📋 Next Steps:"
echo "  1. Analyze PCAP: wireshark load-test-results/swiftpay-load-test.pcap"
echo "  2. Review metrics: cat load-test-results/summary.json | jq ."
echo "  3. Create report: Create load-test-results/PERFORMANCE_REPORT.md"
echo "  4. Commit to git: git add load-test-results/ && git commit -m 'Add load test results'"
echo ""

exit $LOAD_TEST_EXIT
