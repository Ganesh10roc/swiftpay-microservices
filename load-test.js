import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const duration = new Trend('request_duration');
const successfulPayments = new Counter('successful_payments');
const failedPayments = new Counter('failed_payments');
const duplicatePayments = new Counter('duplicate_payments');

export const options = {
  // Performance test configuration
  // 250 TPS (virtual users) for 1 million transactions (~4166 seconds or ~70 minutes)
  scenarios: {
    ramping: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 50 },    // Ramp up to 50 VUs
        { duration: '1m', target: 100 },    // Ramp up to 100 VUs
        { duration: '2m', target: 250 },    // Ramp up to 250 VUs (target load)
        { duration: '60m', target: 250 },   // Hold at 250 VUs for main test
        { duration: '2m', target: 100 },    // Ramp down to 100 VUs
        { duration: '1m', target: 0 },      // Ramp down to 0 VUs
      ],
    },
  },

  // Thresholds for pass/fail
  thresholds: {
    'http_req_duration': ['p(50)<200', 'p(90)<500', 'p(95)<800', 'p(99)<1500'],
    'http_req_failed': ['rate<0.05'],
    'errors': ['rate<0.1'],
  },

  ext: {
    loadimpact: {
      projectID: 3515305,
      name: 'SwiftPay Load Test'
    }
  },
};

const BASE_URL = 'http://localhost:8080';
const LEDGER_URL = 'http://localhost:8081';
const ANALYTICS_URL = 'http://localhost:8082';

// User pool (to simulate real users)
const USERS = [
  'user001', 'user002', 'user003', 'user004', 'user005',
  'user006', 'user007', 'user008', 'user009', 'user010',
];

function getRandomUser(exclude = null) {
  let user;
  do {
    user = USERS[Math.floor(Math.random() * USERS.length)];
  } while (user === exclude);
  return user;
}

export default function () {
  group('Payment Processing Flow', function () {
    // 1. Initiate Payment
    const sender = getRandomUser();
    const receiver = getRandomUser(sender);
    const amount = Math.floor(Math.random() * 1000) + 1;
    const txnId = `txn-${Date.now()}-${Math.random().toString(36).substring(7)}`;

    const paymentPayload = JSON.stringify({
      transaction_id: txnId,
      sender_id: sender,
      receiver_id: receiver,
      amount: amount,
      currency: 'USD',
    });

    const paymentParams = {
      headers: {
        'Content-Type': 'application/json',
      },
      timeout: '30s',
    };

    const startTime = Date.now();
    const paymentRes = http.post(
      `${BASE_URL}/v1/payments`,
      paymentPayload,
      paymentParams
    );
    const endTime = Date.now();
    const responseDuration = endTime - startTime;

    duration.add(responseDuration);

    const paymentSuccess = check(paymentRes, {
      'Payment initiation status is 202 or 409': (r) => r.status === 202 || r.status === 409,
      'Payment response time < 800ms': (r) => r.timings.duration < 800,
      'Payment response is JSON': (r) => r.headers['Content-Type'].includes('application/json'),
    });

    if (paymentRes.status === 202) {
      successfulPayments.add(1);
    } else if (paymentRes.status === 409) {
      duplicatePayments.add(1);
    } else {
      failedPayments.add(1);
      errorRate.add(1);
    }

    if (!paymentSuccess) {
      errorRate.add(1);
    }

    // 2. Brief wait for Kafka processing
    sleep(0.1);

    // 3. Check transaction status (occasional check)
    if (Math.random() < 0.1) {
      group('Transaction Status Check', function () {
        const statusRes = http.get(
          `${BASE_URL}/v1/payments/${txnId}`,
          { timeout: '30s' }
        );

        check(statusRes, {
          'Status check successful': (r) => r.status === 200,
          'Status check < 300ms': (r) => r.timings.duration < 300,
        });
      });
    }

    // 4. Check account balance (occasional check)
    if (Math.random() < 0.05) {
      group('Account Balance Check', function () {
        const balanceRes = http.get(
          `${LEDGER_URL}/v1/ledger/account/${sender}`,
          { timeout: '30s' }
        );

        check(balanceRes, {
          'Balance check successful': (r) => r.status === 200,
          'Balance check < 300ms': (r) => r.timings.duration < 300,
        });
      });
    }

    // 5. Check analytics (very occasional)
    if (Math.random() < 0.01) {
      group('Analytics Query', function () {
        const analyticsRes = http.get(
          `${ANALYTICS_URL}/v1/analytics/metrics/hour`,
          { timeout: '30s' }
        );

        check(analyticsRes, {
          'Analytics query successful': (r) => r.status === 200,
          'Analytics query < 500ms': (r) => r.timings.duration < 500,
        });
      });
    }
  });

  // Small random sleep to vary load pattern
  sleep(Math.random() * 0.5);
}

export function handleSummary(data) {
  return {
    'stdout': textSummary(data, { indent: ' ', enableColors: true }),
    'results.json': JSON.stringify(data),
  };
}

function textSummary(data, options) {
  const indent = options.indent || '  ';
  let summary = '\n';

  summary += '='.repeat(60) + '\n';
  summary += 'SwiftPay Load Test Results\n';
  summary += '='.repeat(60) + '\n\n';

  // Metrics
  if (data.metrics) {
    const metrics = data.metrics;

    summary += 'HTTP Performance Metrics:\n';
    summary += indent + '✓ Total Requests: ' + (metrics.http_reqs?.value || 0) + '\n';
    summary += indent + '✓ Failed Requests: ' + (metrics.http_req_failed?.value || 0) + '\n';

    if (metrics.http_req_duration) {
      const dur = metrics.http_req_duration.values;
      summary += indent + '✓ Request Duration:\n';
      summary += indent + indent + '  avg: ' + formatDuration(dur.avg) + '\n';
      summary += indent + indent + '  p90: ' + formatDuration(dur.p90) + '\n';
      summary += indent + indent + '  p95: ' + formatDuration(dur.p95) + '\n';
      summary += indent + indent + '  p99: ' + formatDuration(dur.p99) + '\n';
      summary += indent + indent + '  max: ' + formatDuration(dur.max) + '\n';
    }

    summary += '\nCustom Metrics:\n';
    summary += indent + '✓ Error Rate: ' + ((metrics.errors?.value || 0) * 100).toFixed(2) + '%\n';
    summary += indent + '✓ Successful Payments: ' + (metrics.successful_payments?.value || 0) + '\n';
    summary += indent + '✓ Failed Payments: ' + (metrics.failed_payments?.value || 0) + '\n';
    summary += indent + '✓ Duplicate Payments: ' + (metrics.duplicate_payments?.value || 0) + '\n';
  }

  // Overall result
  summary += '\n' + '='.repeat(60) + '\n';
  if (data.state === 'passed') {
    summary += '✅ Load Test PASSED\n';
  } else {
    summary += '❌ Load Test FAILED\n';
  }
  summary += '='.repeat(60) + '\n';

  return summary;
}

function formatDuration(ms) {
  if (!ms) return '0ms';
  return ms.toFixed(2) + 'ms';
}
