package com.swiftpay.ledger.entity;

import com.swiftpay.common.domain.TransactionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ledger_entries", indexes = {
        @Index(name = "idx_transaction_id", columnList = "transaction_id", unique = true),
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", unique = true, nullable = false, length = 36)
    private String transactionId;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "debit", precision = 19, scale = 2)
    private BigDecimal debit;

    @Column(name = "credit", precision = 19, scale = 2)
    private BigDecimal credit;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
