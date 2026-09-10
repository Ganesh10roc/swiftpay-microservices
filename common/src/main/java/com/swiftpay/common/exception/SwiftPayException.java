package com.swiftpay.common.exception;

public class SwiftPayException extends RuntimeException {
    private final String errorCode;

    public SwiftPayException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public SwiftPayException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
