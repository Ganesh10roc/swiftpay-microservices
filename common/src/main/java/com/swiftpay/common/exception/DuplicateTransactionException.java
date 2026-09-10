package com.swiftpay.common.exception;

public class DuplicateTransactionException extends SwiftPayException {
    public DuplicateTransactionException(String transactionId) {
        super("Transaction already exists: " + transactionId, "DUPLICATE_TRANSACTION");
    }
}
