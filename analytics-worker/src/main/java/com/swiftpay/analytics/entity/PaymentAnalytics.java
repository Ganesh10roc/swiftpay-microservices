package com.swiftpay.analytics.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_analytics", indexes = {
        @Index(name = "idx_transaction_id", columnList = "transaction_id", unique = true),
        @Index(name = "idx_completed_at", columnList = "completed_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentAnalytics {

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

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    @Column(name = "ingested_at", nullable = false, updatable = false)
    private LocalDateTime ingestedAt;

    @PrePersist
    protected void onCreate() {
        ingestedAt = LocalDateTime.now();
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
    public LocalDateTime getCompletedAt() { return this.completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getIngestedAt() { return this.ingestedAt; }
    public void setIngestedAt(LocalDateTime ingestedAt) { this.ingestedAt = ingestedAt; }

    public static PaymentAnalyticsBuilder builder() {
        return new PaymentAnalyticsBuilder();
    }

    public static class PaymentAnalyticsBuilder {
        private Long id;
        private String transactionId;
        private String senderId;
        private String receiverId;
        private BigDecimal amount;
        private String currency;
        private LocalDateTime completedAt;
        private LocalDateTime ingestedAt;

        public PaymentAnalyticsBuilder id(Long id) { this.id = id; return this; }
        public PaymentAnalyticsBuilder transactionId(String transactionId) { this.transactionId = transactionId; return this; }
        public PaymentAnalyticsBuilder senderId(String senderId) { this.senderId = senderId; return this; }
        public PaymentAnalyticsBuilder receiverId(String receiverId) { this.receiverId = receiverId; return this; }
        public PaymentAnalyticsBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public PaymentAnalyticsBuilder currency(String currency) { this.currency = currency; return this; }
        public PaymentAnalyticsBuilder completedAt(LocalDateTime completedAt) { this.completedAt = completedAt; return this; }
        public PaymentAnalyticsBuilder ingestedAt(LocalDateTime ingestedAt) { this.ingestedAt = ingestedAt; return this; }

        public PaymentAnalytics build() {
            PaymentAnalytics p = new PaymentAnalytics();
            p.id = this.id;
            p.transactionId = this.transactionId;
            p.senderId = this.senderId;
            p.receiverId = this.receiverId;
            p.amount = this.amount;
            p.currency = this.currency;
            p.completedAt = this.completedAt;
            p.ingestedAt = this.ingestedAt;
            return p;
        }
    }
}
