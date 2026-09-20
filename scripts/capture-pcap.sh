#!/bin/bash
#
# SwiftPay PCAP Capture Script
# Captures network traffic during load test (250 TPS, 1M transactions)
#
# Usage: ./capture-pcap.sh [output_file] [interface]
# Example: ./capture-pcap.sh load-test-results/swiftpay.pcap docker0
#

set -e

# Configuration
OUTPUT_FILE="${1:-load-test-results/swiftpay-$(date +%Y%m%d-%H%M%S).pcap}"
INTERFACE="${2:-docker0}"
PORTS="port 8080 or port 8081 or port 8082 or port 9092 or port 5432"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Functions
print_header() {
    echo -e "${BLUE}================================================${NC}"
    echo -e "${BLUE}SwiftPay PCAP Capture Script${NC}"
    echo -e "${BLUE}================================================${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

# Check prerequisites
check_prerequisites() {
    print_info "Checking prerequisites..."

    # Check for tcpdump
    if ! command -v tcpdump &> /dev/null; then
        print_error "tcpdump not found. Install with: sudo apt-get install tcpdump"
        exit 1
    fi
    print_success "tcpdump found"

    # Check for docker
    if ! command -v docker &> /dev/null; then
        print_error "Docker not found"
        exit 1
    fi
    print_success "Docker found"

    # Check for sudo/root
    if [[ $EUID -ne 0 ]]; then
        print_warning "Not running as root. tcpdump requires sudo."
        print_info "Script will request sudo when needed."
    else
        print_success "Running as root"
    fi
}

# Verify Docker services are running
verify_services() {
    print_info "Verifying Docker services..."

    local services=("swiftpay-transaction-gateway" "swiftpay-ledger-service" "swiftpay-analytics-worker" "swiftpay-postgres" "swiftpay-redis" "swiftpay-kafka")
    local running=0

    for service in "${services[@]}"; do
        if docker ps | grep -q "$service"; then
            print_success "$service is running"
            ((running++))
        else
            print_warning "$service is NOT running"
        fi
    done

    echo ""
    if [ $running -lt ${#services[@]} ]; then
        print_warning "Some services are not running. Start with: docker-compose up -d"
        read -p "Continue anyway? (y/n) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi
}

# Verify network interface
verify_interface() {
    print_info "Verifying network interface: $INTERFACE"

    if ! ip link show | grep -q "$INTERFACE"; then
        print_error "Interface $INTERFACE not found"
        print_info "Available interfaces:"
        ip link show | grep "^[0-9]" | awk '{print "  - " $2}' | sed 's/:$//'
        exit 1
    fi
    print_success "Interface $INTERFACE found"
}

# Prepare output directory
prepare_output() {
    print_info "Preparing output..."

    local dir=$(dirname "$OUTPUT_FILE")
    if [ ! -d "$dir" ]; then
        print_info "Creating directory: $dir"
        mkdir -p "$dir"
    fi

    # Check if file already exists
    if [ -f "$OUTPUT_FILE" ]; then
        print_warning "File already exists: $OUTPUT_FILE"
        read -p "Overwrite? (y/n) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            print_error "Cancelled"
            exit 1
        fi
        rm "$OUTPUT_FILE"
    fi

    # Check disk space
    local available=$(df "$dir" | awk 'NR==2 {print $4}')  # KB available
    local needed=$((500 * 1024))  # 500 MB in KB

    if [ "$available" -lt "$needed" ]; then
        print_error "Not enough disk space. Need 500MB, have $((available/1024))MB"
        exit 1
    fi

    print_success "Output directory ready: $dir"
}

# Start PCAP capture
start_capture() {
    print_header
    echo ""
    echo -e "${BLUE}PCAP Capture Configuration:${NC}"
    echo "  Output File: $OUTPUT_FILE"
    echo "  Interface: $INTERFACE"
    echo "  Filter: ($PORTS)"
    echo "  Duration: ~67 minutes (until load test completes)"
    echo ""

    read -p "Ready to start PCAP capture? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_error "Cancelled"
        exit 1
    fi

    print_info "Starting PCAP capture..."

    # Start tcpdump in background
    # Use sudo if not root
    if [[ $EUID -ne 0 ]]; then
        sudo tcpdump -i "$INTERFACE" -w "$OUTPUT_FILE" "$PORTS" &
    else
        tcpdump -i "$INTERFACE" -w "$OUTPUT_FILE" "$PORTS" &
    fi

    PCAP_PID=$!
    echo "$PCAP_PID" > /tmp/swiftpay-pcap.pid

    print_success "PCAP capture started (PID: $PCAP_PID)"
    echo ""
    echo -e "${YELLOW}IMPORTANT: Now run your load test in another terminal:${NC}"
    echo "  cd ~/swiftpay"
    echo "  k6 run load-test.js --out json=load-test-results/results.json"
    echo ""
    echo -e "${YELLOW}The PCAP capture will run until you press Ctrl+C or the load test completes.${NC}"
    echo ""
}

# Monitor capture
monitor_capture() {
    print_info "Monitoring PCAP capture..."
    echo ""

    local last_size=0
    local last_packets=0

    while true; do
        sleep 5

        if ! kill -0 $PCAP_PID 2>/dev/null; then
            break
        fi

        if [ -f "$OUTPUT_FILE" ]; then
            local current_size=$(ls -lh "$OUTPUT_FILE" 2>/dev/null | awk '{print $5}')

            # Try to get packet count
            if command -v capinfos &> /dev/null && [ -f "$OUTPUT_FILE" ]; then
                local current_packets=$(capinfos "$OUTPUT_FILE" 2>/dev/null | grep "Number of packets" | awk '{print $NF}' || echo "?")

                if [ "$current_packets" != "?" ]; then
                    echo "[$(date '+%H:%M:%S')] Size: $current_size | Packets: $current_packets"
                else
                    echo "[$(date '+%H:%M:%S')] Size: $current_size"
                fi
            else
                echo "[$(date '+%H:%M:%S')] Size: $current_size"
            fi
        fi
    done
}

# Stop capture
stop_capture() {
    print_info "Stopping PCAP capture..."

    if [ -f /tmp/swiftpay-pcap.pid ]; then
        PCAP_PID=$(cat /tmp/swiftpay-pcap.pid)

        if kill -0 $PCAP_PID 2>/dev/null; then
            kill $PCAP_PID
            wait $PCAP_PID 2>/dev/null
            print_success "PCAP capture stopped"
        fi

        rm /tmp/swiftpay-pcap.pid
    fi
}

# Verify captured file
verify_capture() {
    print_info "Verifying captured file..."

    if [ ! -f "$OUTPUT_FILE" ]; then
        print_error "PCAP file not found: $OUTPUT_FILE"
        exit 1
    fi

    local size=$(ls -lh "$OUTPUT_FILE" | awk '{print $5}')
    print_success "File exists: $size"

    # Verify file format
    local file_type=$(file "$OUTPUT_FILE" | grep -o "pcap capture file")
    if [ -z "$file_type" ]; then
        print_warning "File may not be a valid PCAP file"
    else
        print_success "File is valid PCAP format"
    fi

    # Get packet count
    if command -v capinfos &> /dev/null; then
        local packet_count=$(capinfos "$OUTPUT_FILE" 2>/dev/null | grep "Number of packets" | awk '{print $NF}')
        if [ -n "$packet_count" ]; then
            print_success "Total packets: $packet_count"
        fi
    fi

    # Try with tshark
    if command -v tshark &> /dev/null; then
        local tshark_check=$(tshark -r "$OUTPUT_FILE" -c 1 2>&1 | grep -c "Frame" || echo "0")
        if [ "$tshark_check" -gt 0 ]; then
            print_success "File readable by tshark"
        fi
    fi
}

# Compress file (optional)
compress_file() {
    local size=$(ls -lh "$OUTPUT_FILE" | awk '{print $5}')
    print_info "File size: $size"

    read -p "Compress file? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_info "Compressing..."
        gzip "$OUTPUT_FILE"
        local compressed=$(ls -lh "${OUTPUT_FILE}.gz" | awk '{print $5}')
        print_success "Compressed: $compressed"
    fi
}

# Print summary
print_summary() {
    echo ""
    print_header
    echo -e "${GREEN}PCAP Capture Complete${NC}"
    echo ""
    echo "Output File: $OUTPUT_FILE"

    if [ -f "$OUTPUT_FILE" ]; then
        local size=$(ls -lh "$OUTPUT_FILE" | awk '{print $5}')
        echo "File Size: $size"
    elif [ -f "${OUTPUT_FILE}.gz" ]; then
        local size=$(ls -lh "${OUTPUT_FILE}.gz" | awk '{print $5}')
        echo "File Size (compressed): $size"
    fi

    echo ""
    echo "Next steps:"
    echo "1. Analyze PCAP: ./scripts/analyze-pcap.sh $OUTPUT_FILE"
    echo "2. View in Wireshark: wireshark $OUTPUT_FILE"
    echo "3. Generate report: See PCAP-REPORT-TEMPLATE.md"
    echo ""
}

# Trap to ensure cleanup
cleanup() {
    echo ""
    print_warning "Capture interrupted"
    stop_capture
}
trap cleanup EXIT INT TERM

# Main execution
main() {
    print_header
    echo ""

    check_prerequisites
    echo ""

    verify_services
    echo ""

    verify_interface
    echo ""

    prepare_output
    echo ""

    start_capture

    monitor_capture

    stop_capture
    echo ""

    verify_capture
    echo ""

    compress_file
    echo ""

    print_summary
}

main
