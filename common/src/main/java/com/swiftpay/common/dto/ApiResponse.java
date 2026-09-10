package com.swiftpay.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    @JsonProperty("status")
    private String status;

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    private T data;

    @JsonProperty("error")
    private ErrorDetails error;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .status("SUCCESS")
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return ApiResponse.<T>builder()
                .status("ERROR")
                .message(message)
                .error(ErrorDetails.builder()
                        .code(errorCode)
                        .message(message)
                        .timestamp(LocalDateTime.now())
                        .build())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ErrorDetails {
        @JsonProperty("code")
        private String code;

        @JsonProperty("message")
        private String message;

        @JsonProperty("timestamp")
        private LocalDateTime timestamp;
    }
}
