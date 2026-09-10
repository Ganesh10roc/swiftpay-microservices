package com.swiftpay.ledger.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@Tag(name = "Health", description = "Health check endpoints")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Verify service is running")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(HealthResponse.builder()
                .status("UP")
                .service("ledger-service")
                .timestamp(LocalDateTime.now())
                .build());
    }

    @Data
    @Builder
    static class HealthResponse {
        @JsonProperty("status")
        private String status;

        @JsonProperty("service")
        private String service;

        @JsonProperty("timestamp")
        private LocalDateTime timestamp;
    }
}
