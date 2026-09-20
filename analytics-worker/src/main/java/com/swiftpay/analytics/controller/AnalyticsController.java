package com.swiftpay.analytics.controller;

import com.swiftpay.analytics.dto.AnalyticsMetrics;
import com.swiftpay.analytics.service.AnalyticsService;
import com.swiftpay.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {
    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);

    private final AnalyticsService analyticsService;
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_DATE_TIME;

    @GetMapping("/metrics/hour")
    @Operation(summary = "Get metrics for last hour", description = "Retrieve transaction metrics for the last hour")
    public ResponseEntity<ApiResponse<AnalyticsMetrics>> getLastHourMetrics() {
        try {
            AnalyticsMetrics metrics = analyticsService.getMetricsForLastHour();
            return ResponseEntity.ok(ApiResponse.success(metrics, "Metrics retrieved successfully"));
        } catch (Exception e) {
            log.error("Failed to retrieve hour metrics", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to retrieve metrics", "METRICS_RETRIEVAL_FAILED"));
        }
    }

    @GetMapping("/metrics/day")
    @Operation(summary = "Get metrics for last day", description = "Retrieve transaction metrics for the last day")
    public ResponseEntity<ApiResponse<AnalyticsMetrics>> getLastDayMetrics() {
        try {
            AnalyticsMetrics metrics = analyticsService.getMetricsForLastDay();
            return ResponseEntity.ok(ApiResponse.success(metrics, "Metrics retrieved successfully"));
        } catch (Exception e) {
            log.error("Failed to retrieve day metrics", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to retrieve metrics", "METRICS_RETRIEVAL_FAILED"));
        }
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get metrics for time period", description = "Retrieve metrics for a custom time period")
    public ResponseEntity<ApiResponse<AnalyticsMetrics>> getMetricsForPeriod(
            @RequestParam String startTime,
            @RequestParam String endTime) {
        try {
            LocalDateTime start = LocalDateTime.parse(startTime, dateFormatter);
            LocalDateTime end = LocalDateTime.parse(endTime, dateFormatter);

            AnalyticsMetrics metrics = analyticsService.getMetricsForTimePeriod(start, end);
            return ResponseEntity.ok(ApiResponse.success(metrics, "Metrics retrieved successfully"));
        } catch (Exception e) {
            log.error("Failed to retrieve period metrics", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to retrieve metrics", "METRICS_RETRIEVAL_FAILED"));
        }
    }
}
