package com.swiftpay.gateway.kafka;

import com.swiftpay.common.events.PaymentInitiatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.payment-initiated}")
    private String paymentInitiatedTopic;

    public void publishPaymentInitiated(PaymentInitiatedEvent event) {
        try {
            kafkaTemplate.send(paymentInitiatedTopic, event.getTransactionId(), event);
            log.info("PaymentInitiatedEvent published: transactionId={}", event.getTransactionId());
        } catch (Exception e) {
            log.error("Failed to publish PaymentInitiatedEvent: transactionId={}", event.getTransactionId(), e);
            throw new RuntimeException("Failed to publish payment event", e);
        }
    }
}
