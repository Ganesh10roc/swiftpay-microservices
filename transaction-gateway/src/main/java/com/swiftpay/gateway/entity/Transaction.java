package com.swiftpay.gateway.entity;

import com.swiftpay.common.domain.TransactionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transaction_id", columnList = "transaction_id", unique = true),
        @Index(name = "idx_sender_id", columnList = "sender_id"),
        @Index(name = "idx_receiver_id", columnList = "receiver_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", unique = true, nullable = false, length = 36)
    private String transactionId;

    @Column(name = "sender_id", nullable = false, length = 100)
    private String senderId;

    @Column(name = "receiver_id", nullable = false, length = 100)
    private String receiverId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "error_reason")
    private String errorReason;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = TransactionStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }
    public String getTransactionId() { return this.transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getSenderId() { return this.senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public String getReceiverId() { return this.receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }
    public BigDecimal getAmount() { return this.amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return this.currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public TransactionStatus getStatus() { return this.status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    public String getIdempotencyKey() { return this.idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public LocalDateTime getCreatedAt() { return this.createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return this.updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getErrorReason() { return this.errorReason; }
    public void setErrorReason(String errorReason) { this.errorReason = errorReason; }

    public static TransactionBuilder builder() {
        return new TransactionBuilder();
    }

    public static class TransactionBuilder {
        private Long id;
        private String transactionId;
        private String senderId;
        private String receiverId;
        private BigDecimal amount;
        private String currency;
        private TransactionStatus status;
        private String idempotencyKey;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String errorReason;

        public TransactionBuilder id(Long id) { this.id = id; return this; }
        public TransactionBuilder transactionId(String transactionId) { this.transactionId = transactionId; return this; }
        public TransactionBuilder senderId(String senderId) { this.senderId = senderId; return this; }
        public TransactionBuilder receiverId(String receiverId) { this.receiverId = receiverId; return this; }
        public TransactionBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public TransactionBuilder currency(String currency) { this.currency = currency; return this; }
        public TransactionBuilder status(TransactionStatus status) { this.status = status; return this; }
        public TransactionBuilder idempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; return this; }
        public TransactionBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public TransactionBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public TransactionBuilder errorReason(String errorReason) { this.errorReason = errorReason; return this; }

        public Transaction build() {
            Transaction t = new Transaction();
            t.id = this.id;
            t.transactionId = this.transactionId;
            t.senderId = this.senderId;
            t.receiverId = this.receiverId;
            t.amount = this.amount;
            t.currency = this.currency;
            t.status = this.status;
            t.idempotencyKey = this.idempotencyKey;
            t.createdAt = this.createdAt;
            t.updatedAt = this.updatedAt;
            t.errorReason = this.errorReason;
            return t;
        }
    }
}
