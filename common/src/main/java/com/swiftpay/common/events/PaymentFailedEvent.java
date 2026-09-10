package com.swiftpay.common.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentFailedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("sender_id")
    private String senderId;

    @JsonProperty("receiver_id")
    private String receiverId;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("failed_at")
    private LocalDateTime failedAt;
}
