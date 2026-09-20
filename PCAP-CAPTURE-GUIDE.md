# SwiftPay - PCAP Capture Guide

**Purpose:** Capture network traffic during 250 TPS load test (1M transactions)  
**Duration:** ~67 minutes  
**Output:** Network packet capture file for analysis

---

## ✅ QUICK START (5 minutes)

### Step 1: Prepare Capture
```bash
cd ~/swiftpay
mkdir -p load-test-results
chmod 777 load-test-results
```

### Step 2: Start PCAP Capture (BEFORE load test)
```bash
# For Linux/Mac
sudo tcpdump -i docker0 -w load-test-results/swiftpay.pcap \
  'port 8080 or port 8081 or port 8082 or port 9092 or port 5432' &

# Save the PID
PCAP_PID=$!
echo "PCAP capture started (PID: $PCAP_PID)"
```

### Step 3: Run Load Test (67 minutes at 250 TPS)
```bash
k6 run load-test.js --out json=load-test-results/results.json
```

### Step 4: Stop PCAP Capture (AFTER load test)
```bash
sudo kill $PCAP_PID
wait $PCAP_PID 2>/dev/null
echo "PCAP capture stopped"
```

### Step 5: Verify PCAP File
```bash
ls -lh load-test-results/swiftpay.pcap
file load-test-results/swiftpay.pcap
```

---

## 📋 DETAILED INSTRUCTIONS

### Prerequisites

**Required Tools:**
```bash
# Check if tcpdump installed
which tcpdump
# If not: sudo apt-get install tcpdump

# Check if k6 installed
which k6
# If not: see k6 installation guide

# Check Docker
docker ps
docker network ls | grep swiftpay
```

**System Requirements:**
- ≥ 2 GB free disk space
- ≥ 2 GB free RAM
- sudo/root access (for tcpdump)
- Docker running with services started

---

### Network Interface Detection

**Find your Docker bridge interface:**

```bash
# Linux
ip link show | grep -E "docker0|br-"

# Mac
ifconfig | grep -E "docker0|veth"

# Windows (WSL2)
ip link show | grep -E "docker0|eth"
```

**Output Example:**
```
2: docker0: <BROADCAST,RUNNING,MULTICAST> mtu 1500
    link/ether 02:42:ac:11:00:01 brd ff:ff:ff:ff:ff:ff
    inet 172.17.0.1/16 scope global docker0
```

**Interface to use:** `docker0` (or the bridge network interface)

---

### PCAP Capture Command - Detailed

#### Option 1: Capture All Traffic (Simplest)
```bash
sudo tcpdump -i docker0 -w load-test-results/swiftpay.pcap
```

**Pros:** Captures everything  
**Cons:** Large file size (~500MB - 2GB)

---

#### Option 2: Filtered Capture (Recommended)
```bash
# Capture only relevant ports
sudo tcpdump -i docker0 -w load-test-results/swiftpay.pcap \
  'port 8080 or port 8081 or port 8082 or port 9092 or port 5432'
```

**Breakdown:**
- `8080` = Transaction Gateway
- `8081` = Ledger Service  
- `8082` = Analytics Worker
- `9092` = Kafka
- `5432` = PostgreSQL

**Pros:** Smaller file, focused capture  
**Cons:** Misses some inter-container traffic

---

#### Option 3: Advanced Capture (Snaplen Control)
```bash
# Capture only first 256 bytes of each packet (faster)
sudo tcpdump -i docker0 -s 256 -w load-test-results/swiftpay.pcap \
  'port 8080 or port 8081 or port 8082 or port 9092 or port 5432'
```

**Pros:** Faster, smaller files  
**Cons:** Truncated packets (enough for latency analysis)

---

#### Option 4: Ring Buffer (for very long captures)
```bash
# Rotate file every 100MB
sudo tcpdump -i docker0 -w load-test-results/swiftpay.pcap \
  -C 100 -W 10 \
  'port 8080 or port 8081 or port 8082 or port 9092 or port 5432'
```

**Parameters:**
- `-C 100` = rotate at 100MB
- `-W 10` = keep max 10 files

---

### tcpdump Flags Explained

```
-i docker0              # Interface to capture from
-w filename             # Write to file (binary format)
-s snaplen              # Bytes to capture per packet (0 = full)
-B bufsize              # Kernel buffer size (1000 = 1MB)
-n                      # No DNS lookups (faster)
-q                      # Quiet (less verbose)
-l                      # Line buffered output
--print                 # Print interface list
--list-interfaces       # List all interfaces
```

---

## 🚀 COMPLETE CAPTURE WORKFLOW

### Setup Phase (5 minutes)
```bash
#!/bin/bash
set -e

echo "🔧 PCAP Capture Setup"
echo "===================="

# Create directory
mkdir -p load-test-results
chmod 777 load-test-results

# Verify Docker
echo "✓ Checking Docker..."
docker ps -q | wc -l
docker network ls | grep swiftpay

# Verify services running
echo "✓ Checking services..."
curl -s http://localhost:8080/health | jq .status
curl -s http://localhost:8081/health | jq .status
curl -s http://localhost:8082/health | jq .status

echo "✓ Setup complete!"
```

### Capture Phase (70 minutes total)
```bash
#!/bin/bash
set -e

echo "📡 Starting PCAP Capture..."
echo "=========================="

PCAP_FILE="load-test-results/swiftpay-$(date +%Y%m%d-%H%M%S).pcap"

echo "Output file: $PCAP_FILE"
echo "Duration: ~67 minutes"
echo ""
echo "Starting capture..."

# Start capture in background
sudo tcpdump -i docker0 -w "$PCAP_FILE" \
  'port 8080 or port 8081 or port 8082 or port 9092 or port 5432' &

PCAP_PID=$!
echo "PCAP PID: $PCAP_PID"
echo "Waiting 2 seconds before load test..."
sleep 2

# Run load test
echo "🚀 Starting load test (250 TPS, 1M transactions)..."
k6 run load-test.js \
  --out json=load-test-results/results.json \
  --summary-export=load-test-results/summary.json

echo "⏹️  Load test complete. Stopping PCAP..."
sleep 2

# Stop capture
sudo kill $PCAP_PID
wait $PCAP_PID 2>/dev/null

echo "✓ PCAP capture stopped"
echo ""
echo "📊 Results:"
ls -lh "$PCAP_FILE"
file "$PCAP_FILE"
```

---

## 📊 POST-CAPTURE VERIFICATION

### Check File Integrity
```bash
# Verify PCAP file
file load-test-results/swiftpay.pcap
# Expected: "pcap capture file - version 2.4"

# Check file size
ls -lh load-test-results/swiftpay.pcap
# Expected: 50MB - 500MB

# Verify readable by tcpdump
tcpdump -r load-test-results/swiftpay.pcap -c 5
# Should show first 5 packets

# Verify readable by Wireshark
capinfos load-test-results/swiftpay.pcap
# Shows: number of packets, duration, etc.
```

### Compress if Needed
```bash
# If file > 100MB, compress
gzip load-test-results/swiftpay.pcap
# Creates: swiftpay.pcap.gz

# Check compression ratio
ls -lh load-test-results/swiftpay*
# Should be 20-30% of original
```

---

## 🐛 TROUBLESHOOTING

### Issue: "Operation not permitted"
**Cause:** tcpdump requires root  
**Fix:** Use `sudo`
```bash
sudo tcpdump -i docker0 -w load-test-results/swiftpay.pcap
```

### Issue: "No such device"
**Cause:** Wrong interface name  
**Fix:** Find correct interface
```bash
# Linux/Mac
ip link show | grep -E "docker|br"

# Use the correct interface (usually docker0)
sudo tcpdump -i docker0 ...
```

### Issue: "File exists" error
**Cause:** File already exists  
**Fix:** Use different filename or timestamp
```bash
# Use timestamp to avoid conflicts
PCAP_FILE="load-test-results/swiftpay-$(date +%s).pcap"
sudo tcpdump -i docker0 -w "$PCAP_FILE" ...
```

### Issue: File is too large
**Cause:** Capturing too much traffic  
**Fix:** Use snaplen or filter
```bash
# Limit to first 256 bytes per packet
sudo tcpdump -i docker0 -s 256 -w load-test-results/swiftpay.pcap ...

# Or use ring buffer
sudo tcpdump -i docker0 -w load-test-results/swiftpay.pcap -C 100 -W 5 ...
```

### Issue: "Permission denied" when writing
**Cause:** No write access to directory  
**Fix:** Create directory with proper permissions
```bash
mkdir -p load-test-results
chmod 777 load-test-results
```

---

## 📋 CAPTURE CHECKLIST

```
Before Load Test:
[ ] Docker services running (3/3 services UP)
[ ] load-test-results directory exists and writable
[ ] Enough disk space (500MB minimum)
[ ] Found correct network interface (docker0)
[ ] Test tcpdump command works with: 
    sudo tcpdump -i docker0 -c 5 -Q in port 8080

During Load Test:
[ ] PCAP capture started before load test
[ ] Can see tcpdump process: ps aux | grep tcpdump
[ ] Load test running properly
[ ] PCAP file is growing: ls -lh load-test-results/swiftpay.pcap

After Load Test:
[ ] PCAP capture stopped
[ ] PCAP file exists and has data
[ ] File is valid: file load-test-results/swiftpay.pcap
[ ] File is readable: tcpdump -r load-test-results/swiftpay.pcap -c 5
[ ] File size is reasonable (50MB - 500MB)
[ ] Compressed if needed
```

---

## 🎯 EXPECTED RESULTS

**For 250 TPS x 1M transactions (67 minutes):**

```
File Size:
  Uncompressed: 150-300 MB (depends on filter)
  Compressed: 30-50 MB (gzip)

Packet Count:
  Total packets: 5-10 million
  HTTP requests: ~1 million
  Kafka messages: ~2 million
  DB transactions: ~1 million

Capture Time:
  Recording time: ~67 minutes
  File write time: ~2-5 minutes
  Total: ~70 minutes

File Properties:
  Format: PCAP v2.4 (libpcap)
  Byte order: Little-endian
  Version: 2.4
```

---

## 🔗 REFERENCES

**tcpdump Documentation:**
- Man page: `man tcpdump`
- Filter syntax: `man pcap-filter`
- Examples: https://www.tcpdump.org/papers/sniffing-faq.html

**Docker Networking:**
- Bridge networks: https://docs.docker.com/network/bridge/
- Interface names vary by OS

**PCAP Format:**
- RFC 1549 (PCAP format)
- libpcap documentation

---

**Version:** 1.0.0  
**Date:** 2026-09-20  
**Status:** Ready for use
