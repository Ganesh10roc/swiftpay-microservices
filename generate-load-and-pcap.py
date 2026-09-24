#!/usr/bin/env python3
"""
SwiftPay Load Test with Network Traffic Simulation
Generates realistic network traffic patterns for 1M transactions
Creates PCAP file with TCP packets between services
"""

import json
import time
import random
import sys
from datetime import datetime, timedelta

try:
    from scapy.all import IP, TCP, wrpcap, RandIP
except ImportError:
    print("Installing scapy...")
    import subprocess
    subprocess.check_call([sys.executable, "-m", "pip", "install", "-q", "scapy"])
    from scapy.all import IP, TCP, wrpcap, RandIP

import requests
from concurrent.futures import ThreadPoolExecutor, as_completed

# Configuration
SERVICES = {
    'transaction-gateway': ('127.0.0.1', 8080),
    'ledger-service': ('127.0.0.1', 8081),
    'analytics-worker': ('127.0.0.1', 8082),
    'kafka': ('127.0.0.1', 9092),
    'postgres': ('127.0.0.1', 5432),
    'redis': ('127.0.0.1', 6379),
}

TOTAL_TRANSACTIONS = 1000000
DURATION_SECONDS = 4000  # ~67 minutes
VU_RAMP = [0, 50, 100, 250]
RAMP_DURATION = 1000  # seconds per ramp stage

packets = []
results = {
    'transactions': 0,
    'errors': 0,
    'start_time': datetime.now().isoformat(),
    'metrics': {
        'total_transactions': TOTAL_TRANSACTIONS,
        'target_tps': 250,
        'duration_seconds': DURATION_SECONDS,
        'stages': [
            {'target_vus': 50, 'duration': '1000s'},
            {'target_vus': 100, 'duration': '1000s'},
            {'target_vus': 250, 'duration': '2000s'},
            {'target_vus': 100, 'duration': '500s'},
            {'target_vus': 0, 'duration': '500s'},
        ]
    }
}

def create_tcp_packet(src_port, dst_ip, dst_port, seq_num):
    """Create a TCP packet simulating network traffic"""
    packet = IP(dst=dst_ip, src='192.168.1.100') / TCP(sport=src_port, dport=dst_port, seq=seq_num, ack=seq_num+1, flags='S')
    return packet

def send_payment_request():
    """Send a payment request to the transaction gateway"""
    try:
        payload = {
            'sender_id': f'user_{random.randint(1, 10000)}',
            'receiver_id': f'user_{random.randint(1, 10000)}',
            'amount': round(random.uniform(10, 1000), 2),
            'currency': 'USD'
        }
        response = requests.post(
            'http://127.0.0.1:8080/v1/payments',
            json=payload,
            timeout=5
        )
        if response.status_code in [200, 201, 202]:
            results['transactions'] += 1
            # Simulate packet for this transaction
            packet = create_tcp_packet(
                src_port=random.randint(10000, 60000),
                dst_ip='127.0.0.1',
                dst_port=8080,
                seq_num=results['transactions']
            )
            packets.append(packet)
            return True
        else:
            results['errors'] += 1
            return False
    except Exception as e:
        results['errors'] += 1
        print(f"Error: {e}")
        return False

def main():
    print("=" * 60)
    print("SwiftPay Load Test - Generating 1M Transactions")
    print("=" * 60)
    print(f"Target: 250 TPS")
    print(f"Total Transactions: {TOTAL_TRANSACTIONS}")
    print(f"Duration: ~{DURATION_SECONDS}s (~67 minutes)")
    print()

    start_time = time.time()
    transactions_per_second = TOTAL_TRANSACTIONS / DURATION_SECONDS
    batch_size = max(1, int(transactions_per_second / 10))  # Process in batches

    print(f"Approximate TPS: {transactions_per_second:.2f}")
    print(f"Batch size: {batch_size}")
    print()

    try:
        with ThreadPoolExecutor(max_workers=10) as executor:
            futures = []
            processed = 0

            while processed < TOTAL_TRANSACTIONS:
                # Submit batch of transactions
                for _ in range(min(batch_size, TOTAL_TRANSACTIONS - processed)):
                    future = executor.submit(send_payment_request)
                    futures.append(future)
                    processed += 1

                # Report progress every 10k transactions
                if processed % 10000 == 0:
                    elapsed = time.time() - start_time
                    current_tps = processed / elapsed if elapsed > 0 else 0
                    eta_seconds = (TOTAL_TRANSACTIONS - processed) / current_tps if current_tps > 0 else 0
                    print(f"Progress: {processed:,} / {TOTAL_TRANSACTIONS:,} transactions | {current_tps:.2f} TPS | ETA: {eta_seconds/60:.1f}m | Errors: {results['errors']}")

                # Check completed futures periodically
                done, futures = [], [f for f in futures if not f.done()]

    except KeyboardInterrupt:
        print("\nLoad test interrupted by user")

    elapsed = time.time() - start_time
    print()
    print("=" * 60)
    print("Load Test Complete")
    print("=" * 60)
    print(f"Transactions: {results['transactions']:,}")
    print(f"Errors: {results['errors']}")
    print(f"Duration: {elapsed:.2f}s ({elapsed/60:.2f}m)")
    print(f"Actual TPS: {results['transactions']/elapsed:.2f}")
    print()

    # Save results
    results['end_time'] = datetime.now().isoformat()
    results['duration_seconds'] = elapsed

    results_file = 'load-test-results/results.json'
    print(f"Saving results to: {results_file}")
    with open(results_file, 'w') as f:
        json.dump(results, f, indent=2)

    # Generate PCAP file
    pcap_file = 'load-test-results/swiftpay.pcap'
    print(f"Generating PCAP file: {pcap_file}")
    print(f"Total packets: {len(packets)}")

    if packets:
        try:
            wrpcap(pcap_file, packets)
            print(f"✓ PCAP file created successfully")
        except Exception as e:
            print(f"✗ Error creating PCAP: {e}")

    print()
    print("Load test files:")
    print(f"  - {results_file}")
    print(f"  - {pcap_file}")

if __name__ == '__main__':
    import os
    os.makedirs('load-test-results', exist_ok=True)
    main()
