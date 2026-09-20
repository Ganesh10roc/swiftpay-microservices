package com.swiftpay.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    public ApiResponse() {}
    public ApiResponse(String status, String message, T data, ErrorDetails error, LocalDateTime timestamp) {
        this.status = status;
        this.message = message;
        this.data = data;
        this.error = error;
        this.timestamp = timestamp;
    }

    public String getStatus() { return this.status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return this.message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return this.data; }
    public void setData(T data) { this.data = data; }
    public ErrorDetails getError() { return this.error; }
    public void setError(ErrorDetails error) { this.error = error; }
    public LocalDateTime getTimestamp() { return this.timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public static <T> ApiResponse<T> builder() {
        return new ApiResponseBuilder<>();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.status = "SUCCESS";
        response.message = message;
        response.data = data;
        response.timestamp = LocalDateTime.now();
        return response;
    }

    public static <T> ApiResponse<T> error(String message, String errorCode) {
        ApiResponse<T> response = new ApiResponse<>();
        response.status = "ERROR";
        response.message = message;
        response.error = new ErrorDetails(errorCode, message, LocalDateTime.now());
        response.timestamp = LocalDateTime.now();
        return response;
    }

    public static class ApiResponseBuilder<T> {
        private String status;
        private String message;
        private T data;
        private ErrorDetails error;
        private LocalDateTime timestamp;

        public ApiResponseBuilder<T> status(String status) { this.status = status; return this; }
        public ApiResponseBuilder<T> message(String message) { this.message = message; return this; }
        public ApiResponseBuilder<T> data(T data) { this.data = data; return this; }
        public ApiResponseBuilder<T> error(ErrorDetails error) { this.error = error; return this; }
        public ApiResponseBuilder<T> timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }

        public ApiResponse<T> build() {
            ApiResponse<T> r = new ApiResponse<>();
            r.status = this.status;
            r.message = this.message;
            r.data = this.data;
            r.error = this.error;
            r.timestamp = this.timestamp;
            return r;
        }
    }

    public static class ErrorDetails {
        @JsonProperty("code")
        private String code;

        @JsonProperty("message")
        private String message;

        @JsonProperty("timestamp")
        private LocalDateTime timestamp;

        public ErrorDetails() {}
        public ErrorDetails(String code, String message, LocalDateTime timestamp) {
            this.code = code;
            this.message = message;
            this.timestamp = timestamp;
        }

        public String getCode() { return this.code; }
        public void setCode(String code) { this.code = code; }
        public String getMessage() { return this.message; }
        public void setMessage(String message) { this.message = message; }
        public LocalDateTime getTimestamp() { return this.timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

        public static ErrorDetailsBuilder builder() {
            return new ErrorDetailsBuilder();
        }

        public static class ErrorDetailsBuilder {
            private String code;
            private String message;
            private LocalDateTime timestamp;

            public ErrorDetailsBuilder code(String code) { this.code = code; return this; }
            public ErrorDetailsBuilder message(String message) { this.message = message; return this; }
            public ErrorDetailsBuilder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }

            public ErrorDetails build() {
                ErrorDetails e = new ErrorDetails();
                e.code = this.code;
                e.message = this.message;
                e.timestamp = this.timestamp;
                return e;
            }
        }
    }
}
