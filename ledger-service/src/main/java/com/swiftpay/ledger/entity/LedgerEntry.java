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

    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }
    public String getTransactionId() { return this.transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getUserId() { return this.userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Long getAccountId() { return this.accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public BigDecimal getDebit() { return this.debit; }
    public void setDebit(BigDecimal debit) { this.debit = debit; }
    public BigDecimal getCredit() { return this.credit; }
    public void setCredit(BigDecimal credit) { this.credit = credit; }
    public BigDecimal getBalanceAfter() { return this.balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }
    public TransactionStatus getStatus() { return this.status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    public String getDescription() { return this.description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return this.createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static LedgerEntryBuilder builder() {
        return new LedgerEntryBuilder();
    }

    public static class LedgerEntryBuilder {
        private Long id;
        private String transactionId;
        private String userId;
        private Long accountId;
        private BigDecimal debit;
        private BigDecimal credit;
        private BigDecimal balanceAfter;
        private TransactionStatus status;
        private String description;
        private LocalDateTime createdAt;

        public LedgerEntryBuilder id(Long id) { this.id = id; return this; }
        public LedgerEntryBuilder transactionId(String transactionId) { this.transactionId = transactionId; return this; }
        public LedgerEntryBuilder userId(String userId) { this.userId = userId; return this; }
        public LedgerEntryBuilder accountId(Long accountId) { this.accountId = accountId; return this; }
        public LedgerEntryBuilder debit(BigDecimal debit) { this.debit = debit; return this; }
        public LedgerEntryBuilder credit(BigDecimal credit) { this.credit = credit; return this; }
        public LedgerEntryBuilder balanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; return this; }
        public LedgerEntryBuilder status(TransactionStatus status) { this.status = status; return this; }
        public LedgerEntryBuilder description(String description) { this.description = description; return this; }
        public LedgerEntryBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public LedgerEntry build() {
            LedgerEntry entry = new LedgerEntry();
            entry.id = this.id;
            entry.transactionId = this.transactionId;
            entry.userId = this.userId;
            entry.accountId = this.accountId;
            entry.debit = this.debit;
            entry.credit = this.credit;
            entry.balanceAfter = this.balanceAfter;
            entry.status = this.status;
            entry.description = this.description;
            entry.createdAt = this.createdAt;
            return entry;
        }
    }
}
