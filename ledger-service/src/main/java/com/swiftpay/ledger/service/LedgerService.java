package com.swiftpay.ledger.service;

import com.swiftpay.common.domain.TransactionStatus;
import com.swiftpay.common.events.PaymentCompletedEvent;
import com.swiftpay.common.events.PaymentFailedEvent;
import com.swiftpay.common.events.PaymentInitiatedEvent;
import com.swiftpay.common.exception.InsufficientFundsException;
import com.swiftpay.ledger.entity.Account;
import com.swiftpay.ledger.entity.LedgerEntry;
import com.swiftpay.ledger.kafka.PaymentEventPublisher;
import com.swiftpay.ledger.repository.AccountRepository;
import com.swiftpay.ledger.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final PaymentEventPublisher paymentEventPublisher;

    @Transactional
    public void processPayment(PaymentInitiatedEvent event) {
        try {
            log.info("Processing payment: transactionId={}, senderId={}, receiverId={}, amount={}",
                    event.getTransactionId(), event.getSenderId(), event.getReceiverId(), event.getAmount());

            // Get accounts with pessimistic locking to avoid race conditions
            Account senderAccount = getOrCreateAccount(event.getSenderId(), event.getCurrency());
            Account receiverAccount = getOrCreateAccount(event.getReceiverId(), event.getCurrency());

            // Validate sufficient balance
            if (senderAccount.getBalance().compareTo(event.getAmount()) < 0) {
                publishPaymentFailed(event, "Insufficient funds");
                log.warn("Insufficient funds: transactionId={}, senderId={}, balance={}, amount={}",
                        event.getTransactionId(), event.getSenderId(), senderAccount.getBalance(), event.getAmount());
                return;
            }

            // Perform atomic debit/credit operation
            senderAccount.setBalance(senderAccount.getBalance().subtract(event.getAmount()));
            receiverAccount.setBalance(receiverAccount.getBalance().add(event.getAmount()));

            accountRepository.save(senderAccount);
            accountRepository.save(receiverAccount);

            // Record ledger entries
            LedgerEntry debitEntry = LedgerEntry.builder()
                    .transactionId(event.getTransactionId())
                    .userId(event.getSenderId())
                    .accountId(senderAccount.getId())
                    .debit(event.getAmount())
                    .balanceAfter(senderAccount.getBalance())
                    .status(TransactionStatus.COMPLETED)
                    .description("Debit: Payment to " + event.getReceiverId())
                    .build();

            LedgerEntry creditEntry = LedgerEntry.builder()
                    .transactionId(event.getTransactionId() + "-credit")
                    .userId(event.getReceiverId())
                    .accountId(receiverAccount.getId())
                    .credit(event.getAmount())
                    .balanceAfter(receiverAccount.getBalance())
                    .status(TransactionStatus.COMPLETED)
                    .description("Credit: Payment from " + event.getSenderId())
                    .build();

            ledgerEntryRepository.save(debitEntry);
            ledgerEntryRepository.save(creditEntry);

            publishPaymentCompleted(event);
            log.info("Payment processed successfully: transactionId={}", event.getTransactionId());

        } catch (Exception e) {
            log.error("Error processing payment: transactionId={}", event.getTransactionId(), e);
            publishPaymentFailed(event, "Processing error: " + e.getMessage());
        }
    }

    private Account getOrCreateAccount(String userId, String currency) {
        Optional<Account> existing = accountRepository.findByUserId(userId);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Create new account with initial balance
        Account newAccount = Account.builder()
                .userId(userId)
                .balance(new BigDecimal("10000.00"))
                .currency(currency)
                .version(0L)
                .build();

        return accountRepository.save(newAccount);
    }

    private void publishPaymentCompleted(PaymentInitiatedEvent event) {
        PaymentCompletedEvent completedEvent = PaymentCompletedEvent.builder()
                .transactionId(event.getTransactionId())
                .senderId(event.getSenderId())
                .receiverId(event.getReceiverId())
                .amount(event.getAmount())
                .currency(event.getCurrency())
                .completedAt(LocalDateTime.now())
                .build();

        paymentEventPublisher.publishPaymentCompleted(completedEvent);
    }

    private void publishPaymentFailed(PaymentInitiatedEvent event, String reason) {
        PaymentFailedEvent failedEvent = PaymentFailedEvent.builder()
                .transactionId(event.getTransactionId())
                .senderId(event.getSenderId())
                .receiverId(event.getReceiverId())
                .reason(reason)
                .failedAt(LocalDateTime.now())
                .build();

        paymentEventPublisher.publishPaymentFailed(failedEvent);
    }

    @Transactional(readOnly = true)
    public Page<LedgerEntry> getLedgerHistory(String userId, Pageable pageable) {
        return ledgerEntryRepository.findByUserId(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Account getAccount(String userId) {
        return accountRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Account not found: " + userId));
    }
}
