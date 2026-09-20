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

    static class HealthResponse {
        @JsonProperty("status")
        private String status;

        @JsonProperty("service")
        private String service;

        @JsonProperty("timestamp")
        private LocalDateTime timestamp;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getService() { return service; }
        public void setService(String service) { this.service = service; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

        public static HealthResponseBuilder builder() {
            return new HealthResponseBuilder();
        }

        static class HealthResponseBuilder {
            private String status;
            private String service;
            private LocalDateTime timestamp;

            public HealthResponseBuilder status(String status) { this.status = status; return this; }
            public HealthResponseBuilder service(String service) { this.service = service; return this; }
            public HealthResponseBuilder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }

            public HealthResponse build() {
                HealthResponse r = new HealthResponse();
                r.status = this.status;
                r.service = this.service;
                r.timestamp = this.timestamp;
                return r;
            }
        }
    }
}
