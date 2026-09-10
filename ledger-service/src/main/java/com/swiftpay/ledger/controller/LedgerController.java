package com.swiftpay.ledger.controller;

import com.swiftpay.common.dto.ApiResponse;
import com.swiftpay.ledger.entity.Account;
import com.swiftpay.ledger.entity.LedgerEntry;
import com.swiftpay.ledger.service.LedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/ledger")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Ledger", description = "Ledger and account endpoints")
public class LedgerController {

    private final LedgerService ledgerService;

    @GetMapping("/history/{userId}")
    @Operation(summary = "Get ledger history", description = "Retrieve ledger entries for a user")
    public ResponseEntity<ApiResponse<Page<LedgerEntry>>> getLedgerHistory(
            @PathVariable String userId,
            Pageable pageable) {
        try {
            Page<LedgerEntry> history = ledgerService.getLedgerHistory(userId, pageable);
            return ResponseEntity.ok(ApiResponse.success(history, "Ledger history retrieved successfully"));
        } catch (Exception e) {
            log.error("Failed to retrieve ledger history: userId={}", userId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to retrieve history", "HISTORY_RETRIEVAL_FAILED"));
        }
    }

    @GetMapping("/account/{userId}")
    @Operation(summary = "Get account details", description = "Retrieve account balance and details")
    public ResponseEntity<ApiResponse<Account>> getAccount(@PathVariable String userId) {
        try {
            Account account = ledgerService.getAccount(userId);
            return ResponseEntity.ok(ApiResponse.success(account, "Account retrieved successfully"));
        } catch (Exception e) {
            log.error("Failed to retrieve account: userId={}", userId, e);
            return ResponseEntity.notFound().build();
        }
    }
}
