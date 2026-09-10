package com.swiftpay.gateway.service;

import com.swiftpay.common.exception.DuplicateTransactionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${idempotency.ttl-hours:24}")
    private long idempotencyTtlHours;

    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:";

    public void checkAndMarkIdempotent(String transactionId) {
        String key = IDEMPOTENCY_KEY_PREFIX + transactionId;

        Boolean exists = redisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(exists)) {
            log.warn("Duplicate transaction attempt: transactionId={}", transactionId);
            throw new DuplicateTransactionException(transactionId);
        }

        redisTemplate.opsForValue().set(key, transactionId, idempotencyTtlHours, TimeUnit.HOURS);
        log.debug("Idempotency key marked: transactionId={}", transactionId);
    }

    public void removeIdempotencyKey(String transactionId) {
        String key = IDEMPOTENCY_KEY_PREFIX + transactionId;
        redisTemplate.delete(key);
        log.debug("Idempotency key removed: transactionId={}", transactionId);
    }
}
