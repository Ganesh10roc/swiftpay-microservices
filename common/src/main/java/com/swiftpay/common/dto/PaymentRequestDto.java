package com.swiftpay.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
}
