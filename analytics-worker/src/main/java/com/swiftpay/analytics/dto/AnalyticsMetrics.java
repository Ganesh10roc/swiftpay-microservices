package com.swiftpay.analytics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsMetrics {

    @JsonProperty("start_time")
    private LocalDateTime startTime;

    @JsonProperty("end_time")
    private LocalDateTime endTime;

    @JsonProperty("transaction_count")
    private long transactionCount;

    @JsonProperty("total_amount")
    private BigDecimal totalAmount;

    @JsonProperty("average_amount")
    private BigDecimal averageAmount;

    @JsonProperty("unique_sender_count")
    private long uniqueSenderCount;
}
