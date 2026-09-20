package com.swiftpay.ledger.kafka;

import com.swiftpay.common.events.PaymentInitiatedEvent;
import com.swiftpay.ledger.service.LedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentEventListener {
    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    private final LedgerService ledgerService;

    @RetryableTopic(
            attempts = 4,
            backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 10000),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE
    )
    @KafkaListener(topics = "${kafka.topics.payment-initiated}", groupId = "${spring.kafka.consumer.group-id}")
    public void onPaymentInitiated(PaymentInitiatedEvent event) {
        try {
            log.info("PaymentInitiatedEvent received: transactionId={}", event.getTransactionId());
            ledgerService.processPayment(event);
        } catch (Exception e) {
            log.error("Error processing payment event: transactionId={}", event.getTransactionId(), e);
            throw new RuntimeException("Failed to process payment", e);
        }
    }
}
