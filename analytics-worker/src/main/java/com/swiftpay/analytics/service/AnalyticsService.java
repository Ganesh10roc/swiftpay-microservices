package com.swiftpay.analytics.service;

import com.swiftpay.analytics.dto.AnalyticsMetrics;
import com.swiftpay.analytics.repository.PaymentAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final PaymentAnalyticsRepository analyticsRepository;

    @Transactional(readOnly = true)
    public AnalyticsMetrics getMetricsForTimePeriod(LocalDateTime startTime, LocalDateTime endTime) {
        try {
            long transactionCount = analyticsRepository.countTransactionsBetween(startTime, endTime);
            BigDecimal totalAmount = analyticsRepository.sumTransactionAmountBetween(startTime, endTime);
            long uniqueSenders = analyticsRepository.countUniqueSendersBetween(startTime, endTime);

            BigDecimal averageAmount = BigDecimal.ZERO;
            if (transactionCount > 0 && totalAmount != null) {
                averageAmount = totalAmount.divide(BigDecimal.valueOf(transactionCount), 2, java.math.RoundingMode.HALF_UP);
            }

            return AnalyticsMetrics.builder()
                    .startTime(startTime)
                    .endTime(endTime)
                    .transactionCount(transactionCount)
                    .totalAmount(totalAmount != null ? totalAmount : BigDecimal.ZERO)
                    .averageAmount(averageAmount)
                    .uniqueSenderCount(uniqueSenders)
                    .build();

        } catch (Exception e) {
            log.error("Error retrieving analytics metrics", e);
            throw new RuntimeException("Failed to retrieve analytics", e);
        }
    }

    @Transactional(readOnly = true)
    public AnalyticsMetrics getMetricsForLastHour() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);
        return getMetricsForTimePeriod(oneHourAgo, now);
    }

    @Transactional(readOnly = true)
    public AnalyticsMetrics getMetricsForLastDay() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneDayAgo = now.minusDays(1);
        return getMetricsForTimePeriod(oneDayAgo, now);
    }
}
