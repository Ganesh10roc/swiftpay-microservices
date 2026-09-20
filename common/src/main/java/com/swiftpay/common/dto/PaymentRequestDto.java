package com.swiftpay.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

public class PaymentRequestDto {

    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("sender_id")
    private String senderId;

    @JsonProperty("receiver_id")
    private String receiverId;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("currency")
    private String currency;

    public PaymentRequestDto() {}
    public PaymentRequestDto(String transactionId, String senderId, String receiverId, BigDecimal amount, String currency) {
        this.transactionId = transactionId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.amount = amount;
        this.currency = currency;
    }

    public String getTransactionId() { return this.transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getSenderId() { return this.senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public String getReceiverId() { return this.receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }
    public BigDecimal getAmount() { return this.amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return this.currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public static PaymentRequestDtoBuilder builder() {
        return new PaymentRequestDtoBuilder();
    }

    public static class PaymentRequestDtoBuilder {
        private String transactionId;
        private String senderId;
        private String receiverId;
        private BigDecimal amount;
        private String currency;

        public PaymentRequestDtoBuilder transactionId(String transactionId) { this.transactionId = transactionId; return this; }
        public PaymentRequestDtoBuilder senderId(String senderId) { this.senderId = senderId; return this; }
        public PaymentRequestDtoBuilder receiverId(String receiverId) { this.receiverId = receiverId; return this; }
        public PaymentRequestDtoBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public PaymentRequestDtoBuilder currency(String currency) { this.currency = currency; return this; }

        public PaymentRequestDto build() {
            PaymentRequestDto d = new PaymentRequestDto();
            d.transactionId = this.transactionId;
            d.senderId = this.senderId;
            d.receiverId = this.receiverId;
            d.amount = this.amount;
            d.currency = this.currency;
            return d;
        }
    }
}
