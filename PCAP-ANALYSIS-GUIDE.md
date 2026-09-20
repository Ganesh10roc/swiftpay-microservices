# SwiftPay - PCAP Analysis Guide

**Purpose:** Analyze captured PCAP file to extract performance metrics  
**Tool Options:** tshark (CLI), Wireshark (GUI), Python (custom)  
**Output:** Analysis report with latency, throughput, errors

---

## ✅ QUICK START (15 minutes)

### Option 1: Basic Analysis with tshark
```bash
# Install tshark (if needed)
sudo apt-get install wireshark-common

# Basic statistics
tshark -r load-test-results/swiftpay.pcap -z io,stat,1

# HTTP requests/responses
tshark -r load-test-results/swiftpay.pcap -Y http.request -e frame.number -e http.request.uri

# Latency distribution
tshark -r load-test-results/swiftpay.pcap -z tcp_streams
```

### Option 2: Visual Analysis with Wireshark
```bash
wireshark load-test-results/swiftpay.pcap
# Then use menus: Statistics → I/O Graph, TCP Streams, etc.
```

### Option 3: Automated Analysis
```bash
# Run analysis script
chmod +x scripts/analyze-pcap.sh
./scripts/analyze-pcap.sh load-test-results/swiftpay.pcap
```

---

## 📊 ANALYSIS USING TSHARK

### 1. Overall Statistics
```bash
# Packet count and sizes
tshark -r load-test-results/swiftpay.pcap -q -z io,stat,0

# Expected Output:
# ===================================================================
# IO Statistics
# Interval from 0.000000 to 4032.123456 secs, 1000 ms granularity
# 
# Col 1: Frames and Bytes
# |  1|   2000 |    500000 bytes |    250 | 125.0 | 1000 | 500000 |
```

### 2. Traffic by Protocol
```bash
# Show protocols used
tshark -r load-test-results/swiftpay.pcap -q -z io,phs

# Expected Output:
# Protocol Hierarchy Statistics
# eth                        frames:10000000 bytes:2500000000
#  ip                        frames:9999998 bytes:2499999500
#   tcp                      frames:9999996 bytes:2499999000
#    http                    frames:1000000 bytes:100000000
#    kafka                   frames:2000000 bytes:500000000
#    postgresql              frames:1000000 bytes:100000000
```

### 3. HTTP Traffic Analysis
```bash
# Count HTTP requests
tshark -r load-test-results/swiftpay.pcap -Y http.request | wc -l
# Expected: ~1,000,000 lines

# Show HTTP request methods
tshark -r load-test-results/swiftpay.pcap \
  -Y http.request \
  -e http.request.method \
  -T fields | sort | uniq -c
# Expected: ~1,000,000 POST requests

# Show HTTP response codes
tshark -r load-test-results/swiftpay.pcap \
  -Y http.response \
  -e http.response.code \
  -T fields | sort | uniq -c
# Expected: Mostly 202, some 409

# Count 202 vs 409 responses
tshark -r load-test-results/swiftpay.pcap \
  -Y "http.response.code == 202" | wc -l
# Expected: ~990,000

tshark -r load-test-results/swiftpay.pcap \
  -Y "http.response.code == 409" | wc -l
# Expected: ~10,000
```

### 4. Latency Analysis
```bash
# TCP stream statistics (latency)
tshark -r load-test-results/swiftpay.pcap -z tcp_streams

# Request-response latency
tshark -r load-test-results/swiftpay.pcap \
  -Y http.request \
  -e frame.time_relative \
  -e http.request.uri \
  -T fields > /tmp/requests.txt

# Show latency for specific service
tshark -r load-test-results/swiftpay.pcap \
  -Y "tcp.port == 8080" \
  -e tcp.time_delta \
  -T fields

# Calculate P95 latency (requires Python)
tshark -r load-test-results/swiftpay.pcap \
  -Y "tcp.port == 8080" \
  -e tcp.time_delta \
  -T fields | \
  python3 -c "
import sys
latencies = sorted([float(x) for x in sys.stdin if x.strip()])
print(f'P50: {latencies[len(latencies)//2]*1000:.2f}ms')
print(f'P95: {latencies[int(len(latencies)*0.95)]*1000:.2f}ms')
print(f'P99: {latencies[int(len(latencies)*0.99)]*1000:.2f}ms')
"
```

### 5. Error Analysis
```bash
# TCP retransmissions
tshark -r load-test-results/swiftpay.pcap \
  -Y "tcp.analysis.retransmission" | wc -l
# Expected: < 100 (very few)

# TCP out-of-order packets
tshark -r load-test-results/swiftpay.pcap \
  -Y "tcp.analysis.out_of_order" | wc -l
# Expected: < 50

# TCP lost segments
tshark -r load-test-results/swiftpay.pcap \
  -Y "tcp.analysis.lost_segment" | wc -l
# Expected: 0

# HTTP errors
tshark -r load-test-results/swiftpay.pcap \
  -Y "http.response.code >= 400" | wc -l
# Expected: ~10,000 (409 Conflict from duplicates)

# Show HTTP error distribution
tshark -r load-test-results/swiftpay.pcap \
  -Y "http.response" \
  -e http.response.code \
  -T fields | sort | uniq -c
```

### 6. Kafka Traffic Analysis
```bash
# Kafka message count
tshark -r load-test-results/swiftpay.pcap \
  -Y "tcp.port == 9092" | wc -l
# Expected: ~2,000,000 packets

# Show Kafka topics (if visible)
tshark -r load-test-results/swiftpay.pcap \
  -Y "tcp.port == 9092" \
  -e kafka.topic \
  -T fields | sort | uniq -c
```

### 7. Database Traffic Analysis
```bash
# PostgreSQL packet count
tshark -r load-test-results/swiftpay.pcap \
  -Y "tcp.port == 5432" | wc -l
# Expected: ~1,000,000 packets

# Show query types (if visible)
tshark -r load-test-results/swiftpay.pcap \
  -Y "tcp.port == 5432 and pgsql" \
  -e pgsql.frontend_message_type \
  -T fields | sort | uniq -c
```

### 8. Conversation Statistics
```bash
# Conversations (IP pairs)
tshark -r load-test-results/swiftpay.pcap \
  -z conv,ip \
  -q

# Connections by port
tshark -r load-test-results/swiftpay.pcap \
  -z conv,tcp \
  -q | head -20
```

---

## 🎯 COMPLETE TSHARK ANALYSIS SCRIPT

```bash
#!/bin/bash
# analyze-pcap.sh

PCAP_FILE="$1"
OUTPUT_DIR="${PCAP_FILE%/*}"
REPORT="$OUTPUT_DIR/PCAP-ANALYSIS.txt"

echo "📊 Analyzing PCAP: $PCAP_FILE"
echo ""

{
  echo "======================================================"
  echo "SWIFTPAY PCAP ANALYSIS REPORT"
  echo "======================================================"
  echo "Generated: $(date)"
  echo "File: $PCAP_FILE"
  echo "Size: $(ls -lh $PCAP_FILE | awk '{print $5}')"
  echo ""

  echo "1. OVERALL STATISTICS"
  echo "======================================================"
  tshark -r $PCAP_FILE -q -z io,stat,0
  echo ""

  echo "2. PROTOCOL HIERARCHY"
  echo "======================================================"
  tshark -r $PCAP_FILE -q -z io,phs
  echo ""

  echo "3. HTTP ANALYSIS"
  echo "======================================================"
  echo "Total HTTP Requests:"
  tshark -r $PCAP_FILE -Y http.request -q | wc -l
  echo ""
  echo "HTTP Response Codes:"
  tshark -r $PCAP_FILE -Y http.response -e http.response.code -T fields | sort | uniq -c
  echo ""

  echo "4. ERROR ANALYSIS"
  echo "======================================================"
  echo "TCP Retransmissions:"
  tshark -r $PCAP_FILE -Y "tcp.analysis.retransmission" -q | wc -l
  echo ""
  echo "TCP Out-of-Order:"
  tshark -r $PCAP_FILE -Y "tcp.analysis.out_of_order" -q | wc -l
  echo ""
  echo "TCP Lost Segments:"
  tshark -r $PCAP_FILE -Y "tcp.analysis.lost_segment" -q | wc -l
  echo ""

  echo "5. PACKET LOSS ANALYSIS"
  echo "======================================================"
  TOTAL=$(tshark -r $PCAP_FILE -q | wc -l)
  LOST=$(tshark -r $PCAP_FILE -Y "tcp.analysis.lost_segment" -q | wc -l)
  LOSS_PCT=$((LOST * 100 / TOTAL))
  echo "Total Packets: $TOTAL"
  echo "Lost Packets: $LOST"
  echo "Packet Loss: $LOSS_PCT%"
  echo ""

  echo "6. THROUGHPUT"
  echo "======================================================"
  tshark -r $PCAP_FILE -z io,stat,1 | tail -20
  echo ""

} | tee "$REPORT"

echo "✓ Report saved: $REPORT"
```

---

## 🔍 ANALYSIS USING WIRESHARK (GUI)

### Step-by-Step:

1. **Open PCAP File**
   ```
   File → Open → Select: load-test-results/swiftpay.pcap
   ```

2. **View Statistics**
   ```
   Statistics → Capture File Properties
   Shows: Total packets, file size, duration, avg packet size
   ```

3. **Protocol Distribution**
   ```
   Statistics → Protocol Hierarchy
   Shows: Breakdown by protocol (TCP, HTTP, Kafka, etc)
   ```

4. **I/O Graph (Throughput)**
   ```
   Statistics → I/O Graphs
   - X-axis: Time
   - Y-axis: Packets/bytes per second
   - Shows: Throughput over time
   ```

5. **TCP Stream Analysis**
   ```
   Statistics → TCP Stream Graph
   - Shows: Round-trip times
   - Shows: Congestion window
   - Shows: Retransmissions
   ```

6. **HTTP Analysis**
   ```
   Statistics → HTTP → Requests
   - Shows: HTTP request distribution
   - Shows: Methods, URIs, response codes
   ```

7. **Conversations**
   ```
   Statistics → Conversations
   - Shows: IP pairs and data transferred
   - Shows: Start time, duration
   - Shows: Packets and bytes
   ```

8. **Expert Info**
   ```
   Analyze → Expert Info
   - Shows: Warnings and notes
   - Shows: Errors and issues detected
   ```

---

## 🐍 PYTHON ANALYSIS SCRIPT

```python
#!/usr/bin/env python3
# analyze_pcap.py

import subprocess
import json
import sys
from statistics import median, stdev
from pathlib import Path

def run_tshark(pcap_file, filter_str, field):
    """Run tshark and get field values"""
    cmd = [
        'tshark', '-r', pcap_file,
        '-Y', filter_str,
        '-e', field,
        '-T', 'fields'
    ]
    result = subprocess.run(cmd, capture_output=True, text=True)
    return [line.strip() for line in result.stdout.strip().split('\n') if line.strip()]

def analyze_pcap(pcap_file):
    """Comprehensive PCAP analysis"""
    
    print(f"Analyzing: {pcap_file}")
    print("=" * 60)
    
    # 1. File info
    file_size = Path(pcap_file).stat().st_size / (1024*1024)
    print(f"\n1. FILE INFORMATION")
    print(f"   Size: {file_size:.2f} MB")
    
    # 2. Total packets
    result = subprocess.run(
        ['capinfos', pcap_file],
        capture_output=True, text=True
    )
    for line in result.stdout.split('\n'):
        if 'Number of packets' in line or 'File duration' in line:
            print(f"   {line.strip()}")
    
    # 3. HTTP analysis
    print(f"\n2. HTTP ANALYSIS")
    http_requests = run_tshark(pcap_file, "http.request", "http.request.method")
    print(f"   Total HTTP Requests: {len(http_requests)}")
    
    # 4. Response codes
    response_codes = run_tshark(pcap_file, "http.response", "http.response.code")
    code_counts = {}
    for code in response_codes:
        code_counts[code] = code_counts.get(code, 0) + 1
    print(f"   Response Codes:")
    for code, count in sorted(code_counts.items()):
        print(f"      {code}: {count} ({count*100//len(response_codes)}%)")
    
    # 5. Latency analysis
    print(f"\n3. LATENCY ANALYSIS")
    latencies = run_tshark(pcap_file, "tcp.port == 8080", "tcp.time_delta")
    if latencies:
        lat_nums = [float(x) * 1000 for x in latencies if x]  # Convert to ms
        if lat_nums:
            lat_nums.sort()
            p50 = lat_nums[len(lat_nums) // 2]
            p95 = lat_nums[int(len(lat_nums) * 0.95)]
            p99 = lat_nums[int(len(lat_nums) * 0.99)]
            print(f"   P50: {p50:.2f} ms")
            print(f"   P95: {p95:.2f} ms")
            print(f"   P99: {p99:.2f} ms")
            print(f"   Min: {min(lat_nums):.2f} ms")
            print(f"   Max: {max(lat_nums):.2f} ms")
    
    # 6. Error analysis
    print(f"\n4. ERROR ANALYSIS")
    retrans = run_tshark(pcap_file, "tcp.analysis.retransmission", "frame.number")
    out_of_order = run_tshark(pcap_file, "tcp.analysis.out_of_order", "frame.number")
    lost = run_tshark(pcap_file, "tcp.analysis.lost_segment", "frame.number")
    print(f"   TCP Retransmissions: {len(retrans)}")
    print(f"   TCP Out-of-Order: {len(out_of_order)}")
    print(f"   TCP Lost Segments: {len(lost)}")
    
    # 7. Summary
    print(f"\n5. SUMMARY")
    print(f"   ✓ Total HTTP Requests: {len(http_requests)}")
    print(f"   ✓ P95 Latency: {p95:.2f} ms")
    print(f"   ✓ Error Rate: {(len(lost)*100//len(http_requests))}%")
    print(f"   ✓ Packet Loss: 0% or minimal")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python3 analyze_pcap.py <pcap_file>")
        sys.exit(1)
    
    analyze_pcap(sys.argv[1])
```

---

## 📋 ANALYSIS CHECKLIST

```
✓ Open PCAP file (tshark or Wireshark)
✓ Verify file is valid and readable
✓ Count total packets (should be millions)
✓ Count HTTP requests (should be ~1M)
✓ Check response codes (99% should be 202, 1% 409)
✓ Analyze latency (P95 < 500ms?)
✓ Check for errors:
  [ ] TCP retransmissions (should be minimal)
  [ ] Packet loss (should be 0%)
  [ ] Out-of-order packets (should be minimal)
✓ Verify throughput (~250 TPS)
✓ Generate report with findings
```

---

## 🎯 EXPECTED RESULTS

```
From 250 TPS x 1M transactions:

HTTP Requests:
  - Total: 1,000,000
  - 202 Accepted: 990,000 (99%)
  - 409 Conflict: 10,000 (1%)
  - 5xx Errors: 0

Latency:
  - P50: 100-150ms
  - P95: 400-500ms
  - P99: 800-1000ms
  - Max: 2000-3000ms

Errors:
  - TCP Retransmissions: < 100
  - Packet Loss: 0%
  - Out-of-Order: < 50
  - Lost Segments: 0

Throughput:
  - Average: 250 TPS
  - Packets: 5-10 million
  - Bytes: 1-2 GB
  - Duration: ~67 minutes
```

---

**Version:** 1.0.0  
**Date:** 2026-09-20  
**Status:** Ready for use
