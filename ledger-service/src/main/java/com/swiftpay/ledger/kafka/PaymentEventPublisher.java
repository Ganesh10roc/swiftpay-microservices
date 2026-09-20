package com.swiftpay.ledger.kafka;

import com.swiftpay.common.events.PaymentCompletedEvent;
import com.swiftpay.common.events.PaymentFailedEvent;
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
public class PaymentEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.payment-completed:payment-completed}")
    private String paymentCompletedTopic;

    @Value("${kafka.topics.payment-failed:payment-failed}")
    private String paymentFailedTopic;

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        try {
            kafkaTemplate.send(paymentCompletedTopic, event.getTransactionId(), event);
            log.info("PaymentCompletedEvent published: transactionId={}", event.getTransactionId());
        } catch (Exception e) {
            log.error("Failed to publish PaymentCompletedEvent: transactionId={}", event.getTransactionId(), e);
        }
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        try {
            kafkaTemplate.send(paymentFailedTopic, event.getTransactionId(), event);
            log.info("PaymentFailedEvent published: transactionId={}", event.getTransactionId());
        } catch (Exception e) {
            log.error("Failed to publish PaymentFailedEvent: transactionId={}", event.getTransactionId(), e);
        }
    }
}
