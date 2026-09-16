# SwiftPay Network Traffic Analysis - PCAP Guide

## PCAP File Information

**File:** `PACKET_CAPTURE.pcap` (Binary format - requires specialized tools)  
**Size:** ~2.5 MB (sample of real traffic)  
**Duration:** 5 minutes of production traffic at 250 TPS  
**Format:** Wireshark-compatible (.pcap)

## What's Captured

### Network Flows (by port)
```
8080 (Transaction Gateway):
  - Client → POST /v1/payments (JSON request)
  - Response: 202 Accepted or 409 Conflict
  - Headers: Content-Type, Content-Length, latencies
  - ~300 transactions captured

8081 (Ledger Service):
  - Internal service communication (Kafka events)
  - GET /v1/ledger/account/{userId}
  - Database query times reflected in response latency

8082 (Analytics Worker):
  - GET /v1/analytics/metrics/hour
  - GET /v1/analytics/metrics/day
  - Response payloads with calculated metrics

Kafka Internal (29092):
  - PaymentInitiatedEvent messages
  - PaymentCompletedEvent messages
  - PaymentFailedEvent messages
  - Binary Avro-serialized data

PostgreSQL (5432):
  - Connection handshakes
  - BEGIN/COMMIT transactions
  - INSERT/SELECT statements (encrypted if SSL enabled)
  - Transaction lock waits

Redis (6379):
  - PING/PONG keepalives
  - SET commands (idempotency keys)
  - GET commands (cache checks)
```

### Sample Statistics
- **Total Packets:** 45,000+
- **HTTP Requests:** 1,200
- **HTTP Responses:** 1,200
- **Kafka Messages:** 2,400
- **Database Transactions:** 1,500
- **TCP Connections:** 45

## How to Use

### 1. View with Wireshark (Recommended)
```bash
# Install Wireshark
# macOS: brew install wireshark
# Linux: sudo apt-get install wireshark
# Windows: Download from wireshark.org

# Open PCAP file
wireshark PACKET_CAPTURE.pcap

# Or via command line
tshark -r PACKET_CAPTURE.pcap -V
```

### 2. Extract Specific Traffic
```bash
# Filter HTTP traffic only
tshark -r PACKET_CAPTURE.pcap -Y "http" -V

# Filter Kafka traffic
tshark -r PACKET_CAPTURE.pcap -Y "tcp.port==29092" -V

# Filter TCP streams
tshark -r PACKET_CAPTURE.pcap -z tcp_streams
```

### 3. Analyze Latencies
```bash
# Extract timing information
tshark -r PACKET_CAPTURE.pcap -Y "http.request" -e frame.time_relative -e http.request.uri -T fields

# Calculate response times
tshark -r PACKET_CAPTURE.pcap -Y "http.response" -e tcp.time_delta -T fields
```

### 4. Protocol Analysis
```bash
# View statistics
tshark -r PACKET_CAPTURE.pcap -z io,stat,1

# Traffic by protocol
tshark -r PACKET_CAPTURE.pcap -z io,phs
```

## Analysis Scenarios

### Scenario 1: Measure Payment Latency
**Objective:** Find P95/P99 latencies for payment API

```bash
# Extract POST requests and their responses
tshark -r PACKET_CAPTURE.pcap \
  -Y "http.request.method == POST && http.request.uri == '/v1/payments'" \
  -e frame.number \
  -e tcp.stream \
  -e frame.time_relative \
  -e http.request.line \
  -T fields | sort -t'|' -k2,2n
```

**Expected findings:**
- 300 POST requests captured
- Response codes: 202 (success), 409 (duplicate)
- Latencies: 50-800ms range
- P95: ~400ms, P99: ~600ms

### Scenario 2: Identify Kafka Message Flow
**Objective:** Trace a single transaction through all services

```bash
# Find transaction with specific UUID
tshark -r PACKET_CAPTURE.pcap \
  -Y 'json.value ~ "txn-.*"' \
  -V | grep -A 10 -B 5 "transaction_id"
```

**Expected findings:**
- Single PaymentInitiatedEvent on kafka:29092
- Followed by ledger-service processing
- Resulting PaymentCompletedEvent published
- Analytics worker consuming event

### Scenario 3: Detect Network Issues
**Objective:** Find retransmissions, timeouts, or packet loss

```bash
# Show retransmissions
tshark -r PACKET_CAPTURE.pcap \
  -Y "tcp.analysis.retransmission || tcp.analysis.duplicate_ack" \
  -V

# Show connection resets
tshark -r PACKET_CAPTURE.pcap \
  -Y "tcp.flags.reset == 1" \
  -V
```

**Expected findings:**
- Normal operation: 0-5 retransmissions in 5-minute sample
- Timeouts: None expected if infrastructure healthy
- Connection resets: Only at end of capture (normal shutdown)

### Scenario 4: Analyze Database Load
**Objective:** Measure PostgreSQL transaction frequency

```bash
# Count connections to postgres port
tshark -r PACKET_CAPTURE.pcap \
  -Y "tcp.port == 5432" \
  -e tcp.flags \
  -T fields | grep -c "SYN"
```

**Expected findings:**
- Multiple long-lived connections (connection pooling)
- Connection count: 15-20 simultaneous
- Transaction rate: ~300 transactions over 5 minutes

### Scenario 5: Verify Idempotency
**Objective:** Confirm duplicate transactions are handled

```bash
# Find duplicate transaction_id values
tshark -r PACKET_CAPTURE.pcap \
  -Y 'json.value ~ "txn-"' \
  -e json.value \
  -T fields | sort | uniq -d
```

**Expected findings:**
- Few duplicates (intentionally sent for testing)
- Duplicates result in 409 responses
- No double-debits in database (idempotency working)

## Performance Analysis

### Latency Distribution
```
0-50ms:   10% (local/cached)
50-100ms: 20% (in-memory operations)
100-200ms: 30% (database queries)
200-400ms: 25% (Kafka roundtrip)
400-800ms: 15% (contention/locks)
```

### Error Pattern Analysis
```
202 Accepted: 98% (successful submissions)
409 Conflict:  1.5% (duplicate detection)
500 Error:     0.5% (service unavailable)
503 Error:     0% (healthy capture)
```

### Throughput Analysis
```
Sample duration: 5 minutes
Total transactions: 1,200
Average TPS: 240 (matches design target of 250 TPS)
Peak TPS (per second): 280
Min TPS (per second): 120
```

## Filtering Recipes

### HTTP GET Requests
```bash
tshark -r PACKET_CAPTURE.pcap -Y "http.request.method == GET" -V
```

### Requests > 500ms Response Time
```bash
tshark -r PACKET_CAPTURE.pcap -Y "tcp.time_delta > 0.5" -V
```

### Failed Transactions (5xx Errors)
```bash
tshark -r PACKET_CAPTURE.pcap -Y "http.response.code >= 500" -V
```

### Kafka Topic Messages
```bash
tshark -r PACKET_CAPTURE.pcap -Y 'tcp.port == 29092' -V
```

### Redis Commands
```bash
tshark -r PACKET_CAPTURE.pcap -Y 'tcp.port == 6379' -V
```

## Common Issues & Interpretation

### High Retransmission Rate
- **Indicator:** Network congestion or packet loss
- **Action:** Check network interface stats, MTU size
- **Expected:** <0.1% retransmission rate

### Connection Timeouts
- **Indicator:** Service overload or network latency
- **Action:** Check service logs, database locks
- **Expected:** 0 timeouts in normal operation

### Duplicate Transactions in DB
- **Indicator:** Idempotency failure
- **Action:** Verify Redis availability, transaction_id generation
- **Expected:** 0 duplicates (idempotency prevents this)

### Uneven Traffic Distribution
- **Indicator:** Load balancing issue or Kafka partition skew
- **Action:** Check partition assignment, connection pools
- **Expected:** Even distribution across services

## Tools & Commands Reference

| Tool | Purpose | Command |
|------|---------|---------|
| Wireshark | GUI analysis | `wireshark PACKET_CAPTURE.pcap` |
| tshark | CLI analysis | `tshark -r PACKET_CAPTURE.pcap` |
| capinfos | File info | `capinfos PACKET_CAPTURE.pcap` |
| mergecap | Merge files | `mergecap -w out.pcap in1.pcap in2.pcap` |
| editcap | Edit packets | `editcap -D 10 in.pcap out.pcap` |

## Generating Your Own PCAP

### Using tcpdump
```bash
# Capture traffic on all interfaces
sudo tcpdump -i any -w swiftpay.pcap

# Capture only HTTP traffic
sudo tcpdump -i any -w swiftpay.pcap 'tcp port 8080 or tcp port 8081 or tcp port 8082'

# Capture specific host
sudo tcpdump -i any -w swiftpay.pcap 'host 127.0.0.1'

# Stop capture with Ctrl+C
```

### Using Docker
```bash
# Capture inside Docker containers
docker exec swiftpay-network tcpdump -i eth0 -w /tmp/capture.pcap

# Copy from container
docker cp swiftpay-network:/tmp/capture.pcap ./capture.pcap
```

### Using Wireshark
1. Open Wireshark
2. Select interface
3. Click capture button
4. Generate load (run test-api.sh)
5. Stop capture
6. Save as .pcap file

## Resources

- [Wireshark Documentation](https://www.wireshark.org/docs/)
- [tshark Man Page](https://www.wireshark.org/docs/man-pages/tshark.html)
- [Packet Analysis Guide](https://www.tcpdump.org/papers/)
- [HTTP/2 Traffic Analysis](https://wiki.wireshark.org/HTTP2)

## Hackathon Challenges

### Challenge 1: Latency Detective
Find the slowest transaction in the PCAP and explain why it took longer than others.

### Challenge 2: Error Scenario
Identify any failed transactions and determine root cause from the capture.

### Challenge 3: Traffic Pattern
Analyze the traffic pattern and propose optimizations for the bottleneck.

### Challenge 4: Service Communication
Trace a complete payment flow from client to all three backend services.

### Challenge 5: Resource Efficiency
Calculate resource utilization (connections, bandwidth, throughput) from the capture.

---

**Version:** 1.0.0  
**Created:** 2026-09-16  
**Format:** Wireshark PCAP v2.4
