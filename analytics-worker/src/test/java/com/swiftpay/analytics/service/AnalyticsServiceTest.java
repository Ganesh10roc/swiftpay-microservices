package com.swiftpay.analytics.service;

import com.swiftpay.analytics.dto.AnalyticsMetrics;
import com.swiftpay.analytics.repository.PaymentAnalyticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Analytics Service Tests")
class AnalyticsServiceTest {

    @Mock
    private PaymentAnalyticsRepository analyticsRepository;

    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(analyticsRepository);
    }

    @Test
    @DisplayName("Should calculate metrics for time period")
    void testGetMetricsForTimePeriod() {
        // Arrange
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now();

        when(analyticsRepository.countTransactionsBetween(startTime, endTime))
                .thenReturn(150L);
        when(analyticsRepository.sumTransactionAmountBetween(startTime, endTime))
                .thenReturn(new BigDecimal("15000.00"));
        when(analyticsRepository.countUniqueSendersBetween(startTime, endTime))
                .thenReturn(45L);

        // Act
        AnalyticsMetrics metrics = analyticsService.getMetricsForTimePeriod(startTime, endTime);

        // Assert
        assertNotNull(metrics);
        assertEquals(150L, metrics.getTransactionCount());
        assertEquals(new BigDecimal("15000.00"), metrics.getTotalAmount());
        assertEquals(45L, metrics.getUniqueSenderCount());
        assertEquals(new BigDecimal("100.00"), metrics.getAverageAmount());
    }

    @Test
    @DisplayName("Should calculate average amount correctly")
    void testAverageAmountCalculation() {
        // Arrange
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now();

        when(analyticsRepository.countTransactionsBetween(startTime, endTime))
                .thenReturn(10L);
        when(analyticsRepository.sumTransactionAmountBetween(startTime, endTime))
                .thenReturn(new BigDecimal("1000.00"));
        when(analyticsRepository.countUniqueSendersBetween(startTime, endTime))
                .thenReturn(5L);

        // Act
        AnalyticsMetrics metrics = analyticsService.getMetricsForTimePeriod(startTime, endTime);

        // Assert
        assertEquals(new BigDecimal("100.00"), metrics.getAverageAmount());
    }

    @Test
    @DisplayName("Should handle zero transactions")
    void testMetricsWithZeroTransactions() {
        // Arrange
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now();

        when(analyticsRepository.countTransactionsBetween(startTime, endTime))
                .thenReturn(0L);
        when(analyticsRepository.sumTransactionAmountBetween(startTime, endTime))
                .thenReturn(null);
        when(analyticsRepository.countUniqueSendersBetween(startTime, endTime))
                .thenReturn(0L);

        // Act
        AnalyticsMetrics metrics = analyticsService.getMetricsForTimePeriod(startTime, endTime);

        // Assert
        assertEquals(0L, metrics.getTransactionCount());
        assertEquals(BigDecimal.ZERO, metrics.getTotalAmount());
        assertEquals(BigDecimal.ZERO, metrics.getAverageAmount());
        assertEquals(0L, metrics.getUniqueSenderCount());
    }

    @Test
    @DisplayName("Should get metrics for last hour")
    void testGetMetricsForLastHour() {
        // Arrange
        when(analyticsRepository.countTransactionsBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(100L);
        when(analyticsRepository.sumTransactionAmountBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("10000.00"));
        when(analyticsRepository.countUniqueSendersBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(30L);

        // Act
        AnalyticsMetrics metrics = analyticsService.getMetricsForLastHour();

        // Assert
        assertNotNull(metrics);
        assertEquals(100L, metrics.getTransactionCount());
        assertNotNull(metrics.getStartTime());
        assertNotNull(metrics.getEndTime());
    }

    @Test
    @DisplayName("Should get metrics for last day")
    void testGetMetricsForLastDay() {
        // Arrange
        when(analyticsRepository.countTransactionsBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(5000L);
        when(analyticsRepository.sumTransactionAmountBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("500000.00"));
        when(analyticsRepository.countUniqueSendersBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(1000L);

        // Act
        AnalyticsMetrics metrics = analyticsService.getMetricsForLastDay();

        // Assert
        assertNotNull(metrics);
        assertEquals(5000L, metrics.getTransactionCount());
    }

    @Test
    @DisplayName("Should handle large transaction counts")
    void testMetricsWithLargeTransactionCount() {
        // Arrange
        LocalDateTime startTime = LocalDateTime.now().minusDays(1);
        LocalDateTime endTime = LocalDateTime.now();

        when(analyticsRepository.countTransactionsBetween(startTime, endTime))
                .thenReturn(1000000L);
        when(analyticsRepository.sumTransactionAmountBetween(startTime, endTime))
                .thenReturn(new BigDecimal("50000000.00"));
        when(analyticsRepository.countUniqueSendersBetween(startTime, endTime))
                .thenReturn(100000L);

        // Act
        AnalyticsMetrics metrics = analyticsService.getMetricsForTimePeriod(startTime, endTime);

        // Assert
        assertEquals(1000000L, metrics.getTransactionCount());
        assertEquals(new BigDecimal("50000000.00"), metrics.getTotalAmount());
        assertEquals(new BigDecimal("50.00"), metrics.getAverageAmount());
    }

    @Test
    @DisplayName("Should handle fractional average amounts")
    void testFractionalAverageAmount() {
        // Arrange
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now();

        when(analyticsRepository.countTransactionsBetween(startTime, endTime))
                .thenReturn(3L);
        when(analyticsRepository.sumTransactionAmountBetween(startTime, endTime))
                .thenReturn(new BigDecimal("100.00"));
        when(analyticsRepository.countUniqueSendersBetween(startTime, endTime))
                .thenReturn(2L);

        // Act
        AnalyticsMetrics metrics = analyticsService.getMetricsForTimePeriod(startTime, endTime);

        // Assert
        assertEquals(new BigDecimal("33.33"), metrics.getAverageAmount());
    }

    @Test
    @DisplayName("Should handle single transaction")
    void testMetricsWithSingleTransaction() {
        // Arrange
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now();

        when(analyticsRepository.countTransactionsBetween(startTime, endTime))
                .thenReturn(1L);
        when(analyticsRepository.sumTransactionAmountBetween(startTime, endTime))
                .thenReturn(new BigDecimal("500.00"));
        when(analyticsRepository.countUniqueSendersBetween(startTime, endTime))
                .thenReturn(1L);

        // Act
        AnalyticsMetrics metrics = analyticsService.getMetricsForTimePeriod(startTime, endTime);

        // Assert
        assertEquals(1L, metrics.getTransactionCount());
        assertEquals(new BigDecimal("500.00"), metrics.getTotalAmount());
        assertEquals(new BigDecimal("500.00"), metrics.getAverageAmount());
        assertEquals(1L, metrics.getUniqueSenderCount());
    }

    @Test
    @DisplayName("Should handle high precision amounts")
    void testHighPrecisionAmounts() {
        // Arrange
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now();

        when(analyticsRepository.countTransactionsBetween(startTime, endTime))
                .thenReturn(2L);
        when(analyticsRepository.sumTransactionAmountBetween(startTime, endTime))
                .thenReturn(new BigDecimal("1000.99"));
        when(analyticsRepository.countUniqueSendersBetween(startTime, endTime))
                .thenReturn(1L);

        // Act
        AnalyticsMetrics metrics = analyticsService.getMetricsForTimePeriod(startTime, endTime);

        // Assert
        assertEquals(new BigDecimal("500.50"), metrics.getAverageAmount());
    }
}
