# SwiftPay API Documentation

## Base URLs
- Transaction Gateway: `http://localhost:8080`
- Ledger Service: `http://localhost:8081`
- Analytics Worker: `http://localhost:8082`

---

## Transaction Gateway API (Port 8080)

### POST /v1/payments
Initiate a payment between two users.

**Request:**
```json
{
  "sender_id": "user123",
  "receiver_id": "user456",
  "amount": 100.50,
  "currency": "USD"
}
```

**Response (201):**
```json
{
  "transaction_id": "uuid-1234",
  "status": "INITIATED",
  "sender_id": "user123",
  "receiver_id": "user456",
  "amount": 100.50,
  "currency": "USD",
  "timestamp": "2026-09-24T10:30:00Z"
}
```

**Error (400):**
```json
{
  "status": 400,
  "error": "Invalid request",
  "message": "Amount must be positive"
}
```

### GET /v1/transactions/{id}
Get transaction status.

**Response (200):**
```json
{
  "transaction_id": "uuid-1234",
  "status": "COMPLETED",
  "sender_id": "user123",
  "receiver_id": "user456",
  "amount": 100.50,
  "created_at": "2026-09-24T10:30:00Z"
}
```

### GET /health
Health check endpoint.

**Response (200):**
```json
{
  "status": "UP",
  "service": "transaction-gateway",
  "timestamp": "2026-09-24T10:30:00Z"
}
```

---

## Ledger Service API (Port 8081)

### GET /v1/accounts/{userId}
Get user account and balance.

**Response (200):**
```json
{
  "user_id": "user123",
  "balance": 9900.50,
  "currency": "USD",
  "account_created": "2026-09-01T00:00:00Z"
}
```

### GET /v1/transactions
Get transaction history.

**Query Parameters:**
- `user_id` (required): User ID to query
- `limit` (optional): Number of records (default: 100)
- `offset` (optional): Pagination offset (default: 0)

**Response (200):**
```json
{
  "user_id": "user123",
  "transactions": [
    {
      "transaction_id": "uuid-1234",
      "type": "DEBIT",
      "amount": 100.50,
      "counterparty": "user456",
      "timestamp": "2026-09-24T10:30:00Z"
    }
  ],
  "total_count": 50
}
```

### GET /health
Service health check.

---

## Analytics Worker API (Port 8082)

### GET /v1/analytics/summary
Get payment analytics summary.

**Response (200):**
```json
{
  "total_transactions": 1000000,
  "successful_transactions": 985234,
  "failed_transactions": 14766,
  "success_rate": 98.52,
  "avg_transaction_amount": 500.00,
  "total_volume": 497,
  "transactions_per_second": 247.33
}
```

### GET /health
Service health check.

---

## Error Responses

All services return standardized error responses:

```json
{
  "timestamp": "2026-09-24T10:30:00Z",
  "status": 500,
  "error": "Internal Server Error",
  "path": "/v1/payments"
}
```

**Common Status Codes:**
- 200: Success
- 201: Created
- 400: Bad Request
- 404: Not Found
- 500: Internal Server Error

---

## Authentication
Currently no authentication required (for hackathon demo).
Production deployments should add OAuth 2.0 or API keys.

## Rate Limiting
None configured (for benchmarking purposes).
Production should implement rate limiting.

## Idempotency
Transaction Gateway supports idempotent requests via `idempotency_key` header.
Same key within 24 hours returns cached result.
