package com.swiftpay.gateway.controller;

import com.swiftpay.common.dto.ApiResponse;
import com.swiftpay.common.dto.PaymentRequestDto;
import com.swiftpay.common.exception.DuplicateTransactionException;
import com.swiftpay.common.exception.SwiftPayException;
import com.swiftpay.gateway.entity.Transaction;
import com.swiftpay.gateway.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "Payment transaction endpoints")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Initiate a new payment transaction", description = "Creates a new payment request and publishes it to Kafka")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "Payment initiated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Duplicate transaction")
    })
    public ResponseEntity<ApiResponse<Transaction>> initiatePayment(@RequestBody PaymentRequestDto request) {
        try {
            log.info("Payment request received: transactionId={}, amount={}", request.getTransactionId(), request.getAmount());
            Transaction transaction = paymentService.initiatePayment(request);
            return ResponseEntity.accepted()
                    .body(ApiResponse.success(transaction, "Payment initiated successfully"));
        } catch (DuplicateTransactionException e) {
            log.warn("Duplicate transaction: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(e.getMessage(), e.getErrorCode()));
        } catch (SwiftPayException e) {
            log.error("Payment initiation failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), e.getErrorCode()));
        }
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "Get transaction details", description = "Retrieve details of a specific transaction")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transaction found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    public ResponseEntity<ApiResponse<Transaction>> getTransaction(@PathVariable String transactionId) {
        try {
            Transaction transaction = paymentService.getTransaction(transactionId);
            return ResponseEntity.ok(ApiResponse.success(transaction, "Transaction retrieved successfully"));
        } catch (Exception e) {
            log.error("Transaction retrieval failed: transactionId={}", transactionId, e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/history/{userId}")
    @Operation(summary = "Get transaction history", description = "Retrieve transaction history for a user")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "History retrieved successfully")
    })
    public ResponseEntity<ApiResponse<Page<Transaction>>> getTransactionHistory(
            @PathVariable String userId,
            Pageable pageable) {
        try {
            Page<Transaction> history = paymentService.getTransactionHistory(userId, pageable);
            return ResponseEntity.ok(ApiResponse.success(history, "Transaction history retrieved successfully"));
        } catch (Exception e) {
            log.error("Transaction history retrieval failed: userId={}", userId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to retrieve history", "HISTORY_RETRIEVAL_FAILED"));
        }
    }
}
