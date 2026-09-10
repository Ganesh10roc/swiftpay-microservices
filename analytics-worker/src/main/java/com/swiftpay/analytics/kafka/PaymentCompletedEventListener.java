package com.swiftpay.analytics.kafka;

import com.swiftpay.analytics.entity.PaymentAnalytics;
import com.swiftpay.analytics.repository.PaymentAnalyticsRepository;
import com.swiftpay.common.events.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCompletedEventListener {

    private final PaymentAnalyticsRepository analyticsRepository;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 10000),
            autoCreateTopic = "true",
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE
    )
    @KafkaListener(topics = "${kafka.topics.payment-completed}", groupId = "${spring.kafka.consumer.group-id}")
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        try {
            log.info("PaymentCompletedEvent received for analytics: transactionId={}, amount={}",
                    event.getTransactionId(), event.getAmount());

            PaymentAnalytics analytics = PaymentAnalytics.builder()
                    .transactionId(event.getTransactionId())
                    .senderId(event.getSenderId())
                    .receiverId(event.getReceiverId())
                    .amount(event.getAmount())
                    .currency(event.getCurrency())
                    .completedAt(event.getCompletedAt())
                    .build();

            analyticsRepository.save(analytics);
            log.info("Analytics record saved: transactionId={}", event.getTransactionId());

        } catch (Exception e) {
            log.error("Error processing payment completed event: transactionId={}", event.getTransactionId(), e);
            throw new RuntimeException("Failed to process analytics event", e);
        }
    }
}
