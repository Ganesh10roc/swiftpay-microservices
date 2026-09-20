#!/bin/bash
#
# SwiftPay PCAP Analysis Script
# Analyzes captured PCAP file and generates metrics report
#
# Usage: ./analyze-pcap.sh <pcap_file> [output_report]
# Example: ./analyze-pcap.sh load-test-results/swiftpay.pcap
#

set -e

# Configuration
PCAP_FILE="${1}"
OUTPUT_REPORT="${2:-${PCAP_FILE%.*}-analysis.txt}"
OUTPUT_DIR=$(dirname "$OUTPUT_REPORT")

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Functions
print_header() {
    echo -e "${BLUE}================================================${NC}"
    echo "$1"
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

    if ! command -v tshark &> /dev/null; then
        print_error "tshark not found. Install with: sudo apt-get install wireshark-common"
        exit 1
    fi
    print_success "tshark found"

    if [ ! -f "$PCAP_FILE" ]; then
        print_error "PCAP file not found: $PCAP_FILE"
        exit 1
    fi
    print_success "PCAP file found"
}

# Verify PCAP file
verify_pcap() {
    print_info "Verifying PCAP file..."

    # Check file format
    if ! file "$PCAP_FILE" | grep -q "pcap"; then
        print_warning "File may not be valid PCAP format"
    else
        print_success "Valid PCAP file"
    fi

    # Try to read first packet
    if ! tshark -r "$PCAP_FILE" -c 1 > /dev/null 2>&1; then
        print_error "Cannot read PCAP file"
        exit 1
    fi
    print_success "PCAP file is readable"
}

# Run analysis and generate report
analyze_pcap() {
    print_header "SwiftPay PCAP Analysis"

    {
        echo "======================================================"
        echo "SWIFTPAY PCAP ANALYSIS REPORT"
        echo "======================================================"
        echo "Generated: $(date)"
        echo "File: $PCAP_FILE"
        echo "File Size: $(ls -lh "$PCAP_FILE" | awk '{print $5}')"
        echo ""

        # 1. FILE INFORMATION
        echo "1. FILE INFORMATION"
        echo "======================================================"
        if command -v capinfos &> /dev/null; then
            capinfos "$PCAP_FILE" | head -10
        else
            echo "capinfos not available, using tshark..."
            echo "Packets: $(tshark -r "$PCAP_FILE" -c 0 2>&1 | tail -1)"
        fi
        echo ""

        # 2. PROTOCOL STATISTICS
        echo "2. PROTOCOL STATISTICS"
        echo "======================================================"
        tshark -r "$PCAP_FILE" -q -z io,phs
        echo ""

        # 3. TRAFFIC BY PORT
        echo "3. TRAFFIC BY PORT"
        echo "======================================================"
        echo "Port 8080 (Transaction Gateway):"
        tshark -r "$PCAP_FILE" -Y "tcp.port == 8080" -q | wc -l
        echo ""
        echo "Port 8081 (Ledger Service):"
        tshark -r "$PCAP_FILE" -Y "tcp.port == 8081" -q | wc -l
        echo ""
        echo "Port 8082 (Analytics Worker):"
        tshark -r "$PCAP_FILE" -Y "tcp.port == 8082" -q | wc -l
        echo ""
        echo "Port 9092 (Kafka):"
        tshark -r "$PCAP_FILE" -Y "tcp.port == 9092" -q | wc -l
        echo ""
        echo "Port 5432 (PostgreSQL):"
        tshark -r "$PCAP_FILE" -Y "tcp.port == 5432" -q | wc -l
        echo ""

        # 4. HTTP ANALYSIS
        echo "4. HTTP ANALYSIS"
        echo "======================================================"
        echo "Total HTTP Requests:"
        tshark -r "$PCAP_FILE" -Y "http.request" -q | wc -l
        echo ""

        echo "HTTP Response Codes:"
        tshark -r "$PCAP_FILE" \
            -Y "http.response" \
            -e http.response.code \
            -T fields | sort | uniq -c | sort -rn
        echo ""

        # 5. ERROR ANALYSIS
        echo "5. ERROR ANALYSIS"
        echo "======================================================"
        echo "TCP Retransmissions:"
        tshark -r "$PCAP_FILE" -Y "tcp.analysis.retransmission" -q | wc -l
        echo ""

        echo "TCP Out-of-Order:"
        tshark -r "$PCAP_FILE" -Y "tcp.analysis.out_of_order" -q | wc -l
        echo ""

        echo "TCP Lost Segments:"
        tshark -r "$PCAP_FILE" -Y "tcp.analysis.lost_segment" -q | wc -l
        echo ""

        echo "TCP Timeouts:"
        tshark -r "$PCAP_FILE" -Y "tcp.analysis.ack_lost_segment" -q | wc -l
        echo ""

        # 6. CONVERSATION STATISTICS
        echo "6. CONVERSATION STATISTICS (Top 10)"
        echo "======================================================"
        tshark -r "$PCAP_FILE" -q -z conv,tcp | head -15
        echo ""

        # 7. THROUGHPUT STATISTICS
        echo "7. THROUGHPUT STATISTICS (by second)"
        echo "======================================================"
        tshark -r "$PCAP_FILE" -q -z io,stat,1 | tail -20
        echo ""

        # 8. EXPERT INFO
        echo "8. EXPERT INFO (if available)"
        echo "======================================================"
        tshark -r "$PCAP_FILE" -Y "frame" -q -z expert 2>/dev/null | head -20 || echo "Expert info not available"
        echo ""

    } | tee "$OUTPUT_REPORT"
}

# Generate summary metrics
generate_summary() {
    print_header "Analysis Summary"

    # Extract key metrics
    echo ""
    print_info "Key Metrics:"

    local http_reqs=$(tshark -r "$PCAP_FILE" -Y "http.request" -q 2>/dev/null | wc -l)
    echo "  HTTP Requests: $http_reqs"

    local response_202=$(tshark -r "$PCAP_FILE" -Y "http.response.code == 202" -q 2>/dev/null | wc -l)
    echo "  HTTP 202 (Accepted): $response_202"

    local response_409=$(tshark -r "$PCAP_FILE" -Y "http.response.code == 409" -q 2>/dev/null | wc -l)
    echo "  HTTP 409 (Conflict): $response_409"

    local retrans=$(tshark -r "$PCAP_FILE" -Y "tcp.analysis.retransmission" -q 2>/dev/null | wc -l)
    echo "  TCP Retransmissions: $retrans"

    local lost=$(tshark -r "$PCAP_FILE" -Y "tcp.analysis.lost_segment" -q 2>/dev/null | wc -l)
    echo "  TCP Lost Segments: $lost"

    echo ""
    print_success "Analysis complete!"
    echo "Report saved to: $OUTPUT_REPORT"
}

# Generate HTML report (optional)
generate_html_report() {
    local html_report="${OUTPUT_REPORT%.*}.html"

    print_info "Generating HTML report..."

    cat > "$html_report" << 'EOF'
<!DOCTYPE html>
<html>
<head>
    <title>SwiftPay PCAP Analysis Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        h1 { color: #333; }
        table { border-collapse: collapse; width: 100%; margin: 20px 0; }
        th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }
        th { background-color: #4CAF50; color: white; }
        tr:nth-child(even) { background-color: #f2f2f2; }
        .success { color: green; }
        .error { color: red; }
        .warning { color: orange; }
        pre { background-color: #f4f4f4; padding: 10px; overflow-x: auto; }
    </style>
</head>
<body>
    <h1>SwiftPay Load Test - PCAP Analysis Report</h1>
    <p>Generated: <strong>[DATE]</strong></p>
    <p>PCAP File: <strong>[FILE]</strong></p>

    <h2>Executive Summary</h2>
    <table>
        <tr>
            <th>Metric</th>
            <th>Value</th>
            <th>Status</th>
        </tr>
        <tr>
            <td>Total HTTP Requests</td>
            <td>[HTTP_REQS]</td>
            <td class="success">✓</td>
        </tr>
        <tr>
            <td>HTTP 202 (Success)</td>
            <td>[HTTP_202]</td>
            <td class="success">✓</td>
        </tr>
        <tr>
            <td>HTTP 409 (Duplicate)</td>
            <td>[HTTP_409]</td>
            <td class="success">✓</td>
        </tr>
        <tr>
            <td>TCP Retransmissions</td>
            <td>[RETRANS]</td>
            <td class="success">✓</td>
        </tr>
        <tr>
            <td>Packet Loss</td>
            <td>[LOSS]%</td>
            <td class="success">✓</td>
        </tr>
    </table>

    <h2>Detailed Analysis</h2>
    <p>See attached text report for detailed metrics.</p>

</body>
</html>
EOF

    print_success "HTML report generated: $html_report"
}

# Main execution
main() {
    print_header "SwiftPay PCAP Analysis"
    echo ""

    check_prerequisites
    echo ""

    verify_pcap
    echo ""

    analyze_pcap

    generate_summary

    echo ""
    print_info "Analysis script completed successfully"
}

# Run main if PCAP file provided
if [ -z "$PCAP_FILE" ]; then
    print_error "Usage: $0 <pcap_file> [output_report]"
    print_error "Example: $0 load-test-results/swiftpay.pcap"
    exit 1
fi

main
