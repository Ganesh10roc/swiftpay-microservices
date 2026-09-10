package com.swiftpay.analytics.repository;

import com.swiftpay.analytics.entity.PaymentAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public interface PaymentAnalyticsRepository extends JpaRepository<PaymentAnalytics, Long> {

    @Query("SELECT COUNT(p) FROM PaymentAnalytics p WHERE p.completedAt >= :startTime AND p.completedAt < :endTime")
    long countTransactionsBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT SUM(p.amount) FROM PaymentAnalytics p WHERE p.completedAt >= :startTime AND p.completedAt < :endTime")
    BigDecimal sumTransactionAmountBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT COUNT(DISTINCT p.senderId) FROM PaymentAnalytics p WHERE p.completedAt >= :startTime AND p.completedAt < :endTime")
    long countUniqueSendersBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
