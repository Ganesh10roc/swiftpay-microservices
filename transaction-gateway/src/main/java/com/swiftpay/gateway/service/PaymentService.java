package com.swiftpay.gateway.service;

import com.swiftpay.common.domain.TransactionStatus;
import com.swiftpay.common.dto.PaymentRequestDto;
import com.swiftpay.common.events.PaymentInitiatedEvent;
import com.swiftpay.gateway.entity.Transaction;
import com.swiftpay.gateway.kafka.PaymentEventProducer;
import com.swiftpay.gateway.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final TransactionRepository transactionRepository;
    private final PaymentEventProducer paymentEventProducer;
    private final IdempotencyService idempotencyService;

    @Transactional
    public Transaction initiatePayment(PaymentRequestDto request) {
        String transactionId = request.getTransactionId() != null ?
                request.getTransactionId() : UUID.randomUUID().toString();

        log.info("Initiating payment: transactionId={}, senderId={}, receiverId={}, amount={}",
                transactionId, request.getSenderId(), request.getReceiverId(), request.getAmount());

        idempotencyService.checkAndMarkIdempotent(transactionId);

        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .senderId(request.getSenderId())
                .receiverId(request.getReceiverId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status(TransactionStatus.PENDING)
                .idempotencyKey(request.getTransactionId())
                .createdAt(LocalDateTime.now())
                .build();

        transaction = transactionRepository.save(transaction);
        log.info("Transaction saved: transactionId={}, id={}", transactionId, transaction.getId());

        PaymentInitiatedEvent event = PaymentInitiatedEvent.builder()
                .transactionId(transactionId)
                .senderId(request.getSenderId())
                .receiverId(request.getReceiverId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .timestamp(LocalDateTime.now())
                .idempotencyKey(transactionId)
                .build();

        paymentEventProducer.publishPaymentInitiated(event);

        return transaction;
    }

    @Transactional(readOnly = true)
    public Transaction getTransaction(String transactionId) {
        return transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));
    }

    @Transactional(readOnly = true)
    public Page<Transaction> getTransactionHistory(String userId, Pageable pageable) {
        return transactionRepository.findByUserId(userId, pageable);
    }

    @Transactional
    public void updateTransactionStatus(String transactionId, TransactionStatus status, String errorReason) {
        Transaction transaction = getTransaction(transactionId);
        transaction.setStatus(status);
        if (errorReason != null) {
            transaction.setErrorReason(errorReason);
        }
        transactionRepository.save(transaction);
        log.info("Transaction status updated: transactionId={}, status={}", transactionId, status);
    }
}
