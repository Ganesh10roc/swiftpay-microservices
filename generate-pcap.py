#!/usr/bin/env python3
"""
SwiftPay PCAP Generator - Creates realistic network traffic captures for the Hackathon
Generates HTTP traffic for payment transactions across all three services
Usage: python3 generate-pcap.py [output_file.pcap] [num_transactions]
"""

import sys
import struct
import random
import time
import json
from datetime import datetime

def write_pcap_header(f):
    """Write PCAP global header"""
    # PCAP magic number, version, reserved, reserved, snaplen, network (Ethernet)
    f.write(struct.pack('<IHHIIII', 0xa1b2c3d4, 2, 4, 0, 0, 65535, 1))

def write_packet_header(f, packet_len, timestamp):
    """Write PCAP packet header"""
    sec, microsec = int(timestamp), int((timestamp % 1) * 1000000)
    f.write(struct.pack('<IIII', sec, microsec, packet_len, packet_len))

def create_http_request(method, path, host, body=None, headers=None):
    """Create HTTP request string"""
    default_headers = {
        'Host': host,
        'User-Agent': 'SwiftPay-Client/1.0',
        'Accept': 'application/json',
        'Content-Type': 'application/json',
    }
    if headers:
        default_headers.update(headers)

    request_line = f"{method} {path} HTTP/1.1\r\n"
    header_lines = "\r\n".join([f"{k}: {v}" for k, v in default_headers.items()])

    if body:
        request = f"{request_line}{header_lines}\r\nContent-Length: {len(body)}\r\n\r\n{body}"
    else:
        request = f"{request_line}{header_lines}\r\n\r\n"

    return request.encode()

def create_http_response(status_code, status_text, body=None, headers=None):
    """Create HTTP response string"""
    default_headers = {
        'Server': 'Spring/3.3.0',
        'Content-Type': 'application/json',
    }
    if headers:
        default_headers.update(headers)

    status_line = f"HTTP/1.1 {status_code} {status_text}\r\n"
    header_lines = "\r\n".join([f"{k}: {v}" for k, v in default_headers.items()])

    if body:
        response = f"{status_line}{header_lines}\r\nContent-Length: {len(body)}\r\n\r\n{body}"
    else:
        response = f"{status_line}{header_lines}\r\n\r\n"

    return response.encode()

def create_ethernet_frame(source_mac, dest_mac, source_ip, dest_ip, source_port, dest_port, payload, is_response=False):
    """Create Ethernet + IP + TCP frame"""
    # Simplified TCP/IP packet (not including all header details)
    # This is a basic representation for PCAP format compatibility

    # Ethernet header (14 bytes)
    eth_header = bytes.fromhex(''.join([x for pair in zip(dest_mac.split(':'), source_mac.split(':'))
                                         for x in pair]))  # Dest MAC, Src MAC
    eth_header += struct.pack('>H', 0x0800)  # IPv4

    # IP header (20 bytes minimum)
    ip_version_ihl = 0x45  # IPv4, 20 byte header
    ip_dscp_ecn = 0x00
    ip_payload_len = 20 + 20 + len(payload)  # IP + TCP + payload
    ip_identification = random.randint(0, 65535)
    ip_flags_fragment = 0x4000  # Don't fragment
    ip_ttl = 64
    ip_protocol = 6  # TCP

    source_ip_packed = struct.pack('>BBBB', *map(int, source_ip.split('.')))
    dest_ip_packed = struct.pack('>BBBB', *map(int, dest_ip.split('.')))

    ip_header = struct.pack('>BBHHHBBH', ip_version_ihl, ip_dscp_ecn, ip_payload_len,
                           ip_identification, ip_flags_fragment, ip_ttl, ip_protocol, 0)
    ip_header += source_ip_packed + dest_ip_packed

    # TCP header (20 bytes minimum)
    tcp_seq = random.randint(0, 2**32 - 1)
    tcp_ack = random.randint(0, 2**32 - 1)
    tcp_data_offset = 0x50  # 20 bytes
    tcp_flags = 0x18 if not is_response else 0x18  # PSH, ACK
    tcp_window = 65535

    tcp_header = struct.pack('>HHIIBBHHH', source_port, dest_port, tcp_seq, tcp_ack,
                            tcp_data_offset, tcp_flags, tcp_window, 0, 0)

    return eth_header + ip_header + tcp_header + payload

def generate_swiftpay_pcap(output_file, num_transactions=100):
    """Generate PCAP file with SwiftPay payment traffic"""

    with open(output_file, 'wb') as f:
        write_pcap_header(f)

        base_time = time.time()
        packet_num = 0

        # Sample users
        users = [f"user{i:03d}" for i in range(1, 51)]

        for txn_num in range(num_transactions):
            sender = random.choice(users)
            receiver = random.choice([u for u in users if u != sender])
            amount = round(random.uniform(10, 1000), 2)
            txn_id = f"txn-{txn_num:06d}-{random.randint(1000, 9999)}"

            # 1. Client POST /v1/payments to Transaction Gateway (8080)
            payment_body = json.dumps({
                "transaction_id": txn_id,
                "sender_id": sender,
                "receiver_id": receiver,
                "amount": amount,
                "currency": "USD"
            })

            request = create_http_request('POST', '/v1/payments', 'localhost:8080', payment_body)
            packet_time = base_time + (txn_num * 0.05)  # 50ms between requests

            frame = create_ethernet_frame('00:00:00:00:00:01', '00:00:00:00:00:02',
                                         '127.0.0.1', '127.0.0.1',
                                         random.randint(49152, 65535), 8080, request)

            write_packet_header(f, len(frame), packet_time)
            f.write(frame)
            packet_num += 1

            # 2. Transaction Gateway response (202 Accepted)
            response_body = json.dumps({
                "status": "SUCCESS",
                "message": "Payment initiated successfully",
                "data": {"id": txn_num, "transactionId": txn_id, "status": "PENDING"}
            })

            response = create_http_response(202, 'Accepted', response_body)
            frame = create_ethernet_frame('00:00:00:00:00:02', '00:00:00:00:00:01',
                                         '127.0.0.1', '127.0.0.1',
                                         8080, random.randint(49152, 65535), response, is_response=True)

            write_packet_header(f, len(frame), packet_time + 0.001)
            f.write(frame)
            packet_num += 1

            # 3. Kafka internal traffic (payment-initiated topic)
            kafka_msg = json.dumps({
                "eventType": "PaymentInitiatedEvent",
                "transactionId": txn_id,
                "senderId": sender,
                "receiverId": receiver,
                "amount": amount
            }).encode()

            frame = create_ethernet_frame('00:00:00:00:00:03', '00:00:00:00:00:04',
                                         '127.0.0.1', '127.0.0.1',
                                         random.randint(49152, 65535), 29092, kafka_msg)

            write_packet_header(f, len(frame), packet_time + 0.002)
            f.write(frame)
            packet_num += 1

            # 4. Ledger Service GET account details
            request = create_http_request('GET', f'/v1/ledger/account/{sender}', 'localhost:8081')
            frame = create_ethernet_frame('00:00:00:00:00:05', '00:00:00:00:00:06',
                                         '127.0.0.1', '127.0.0.1',
                                         random.randint(49152, 65535), 8081, request)

            write_packet_header(f, len(frame), packet_time + 0.003)
            f.write(frame)
            packet_num += 1

            # 5. Ledger Service response with balance
            ledger_response = json.dumps({
                "status": "SUCCESS",
                "data": {
                    "userId": sender,
                    "balance": round(10000 - (amount * 1.02), 2),
                    "currency": "USD"
                }
            })

            response = create_http_response(200, 'OK', ledger_response)
            frame = create_ethernet_frame('00:00:00:00:00:06', '00:00:00:00:00:05',
                                         '127.0.0.1', '127.0.0.1',
                                         8081, random.randint(49152, 65535), response, is_response=True)

            write_packet_header(f, len(frame), packet_time + 0.004)
            f.write(frame)
            packet_num += 1

            # 6. Kafka payment-completed event (success cases)
            if random.random() > 0.02:  # 98% success rate
                kafka_msg = json.dumps({
                    "eventType": "PaymentCompletedEvent",
                    "transactionId": txn_id,
                    "senderId": sender,
                    "receiverId": receiver,
                    "amount": amount,
                    "completedAt": datetime.utcnow().isoformat()
                }).encode()

                frame = create_ethereum_frame('00:00:00:00:00:07', '00:00:00:00:00:08',
                                             '127.0.0.1', '127.0.0.1',
                                             random.randint(49152, 65535), 29092, kafka_msg)

                write_packet_header(f, len(frame), packet_time + 0.005)
                f.write(frame)
                packet_num += 1

            # 7. Analytics GET metrics
            request = create_http_request('GET', '/v1/analytics/metrics/hour', 'localhost:8082')
            frame = create_ethernet_frame('00:00:00:00:00:09', '00:00:00:00:00:0a',
                                         '127.0.0.1', '127.0.0.1',
                                         random.randint(49152, 65535), 8082, request)

            write_packet_header(f, len(frame), packet_time + 0.006)
            f.write(frame)
            packet_num += 1

            # 8. Analytics response with metrics
            analytics_response = json.dumps({
                "status": "SUCCESS",
                "data": {
                    "transactionCount": txn_num + 1,
                    "totalAmount": amount * (txn_num + 1),
                    "averageAmount": amount,
                    "uniqueSenderCount": min(txn_num // 2, 50)
                }
            })

            response = create_http_response(200, 'OK', analytics_response)
            frame = create_ethernet_frame('00:00:00:00:00:0a', '00:00:00:00:00:09',
                                         '127.0.0.1', '127.0.0.1',
                                         8082, random.randint(49152, 65535), response, is_response=True)

            write_packet_header(f, len(frame), packet_time + 0.007)
            f.write(frame)
            packet_num += 1

    print(f"✓ Generated PCAP file: {output_file}")
    print(f"  - Transactions: {num_transactions}")
    print(f"  - Total packets: {packet_num}")
    print(f"  - File size: {os.path.getsize(output_file) / 1024:.2f} KB")

if __name__ == "__main__":
    import os

    output = sys.argv[1] if len(sys.argv) > 1 else "PACKET_CAPTURE.pcap"
    num_txns = int(sys.argv[2]) if len(sys.argv) > 2 else 100

    print(f"Generating SwiftPay PCAP file...")
    print(f"  Output: {output}")
    print(f"  Transactions: {num_txns}")

    generate_swiftpay_pcap(output, num_txns)
    print("\n✓ PCAP generation complete!")
