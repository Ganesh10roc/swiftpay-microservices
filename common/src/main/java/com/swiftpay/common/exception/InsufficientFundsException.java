package com.swiftpay.common.exception;

public class InsufficientFundsException extends SwiftPayException {
    public InsufficientFundsException(String senderId) {
        super("Insufficient funds for sender: " + senderId, "INSUFFICIENT_FUNDS");
    }
}
