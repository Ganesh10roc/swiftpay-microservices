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
}
