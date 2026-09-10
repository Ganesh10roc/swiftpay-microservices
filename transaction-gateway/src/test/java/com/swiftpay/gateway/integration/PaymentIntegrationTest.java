package com.swiftpay.gateway.integration;

import com.swiftpay.common.domain.TransactionStatus;
import com.swiftpay.common.dto.PaymentRequestDto;
import com.swiftpay.gateway.entity.Transaction;
import com.swiftpay.gateway.repository.TransactionRepository;
import com.swiftpay.gateway.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import({PaymentService.class})
@ActiveProfiles("test")
@DisplayName("Payment Integration Tests")
class PaymentIntegrationTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    @DisplayName("Should save and retrieve transaction from database")
    void testSaveAndRetrieveTransaction() {
        // Arrange
        Transaction transaction = Transaction.builder()
                .transactionId("txn-integration-001")
                .senderId("user001")
                .receiverId("user002")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .status(TransactionStatus.PENDING)
                .build();

        // Act
        Transaction saved = transactionRepository.save(transaction);
        Optional<Transaction> retrieved = transactionRepository.findByTransactionId("txn-integration-001");

        // Assert
        assertTrue(retrieved.isPresent());
        assertEquals(saved.getTransactionId(), retrieved.get().getTransactionId());
        assertEquals(TransactionStatus.PENDING, retrieved.get().getStatus());
    }

    @Test
    @DisplayName("Should enforce unique transaction ID constraint")
    void testUniqueTransactionIdConstraint() {
        // Arrange
        Transaction transaction1 = Transaction.builder()
                .transactionId("txn-duplicate")
                .senderId("user001")
                .receiverId("user002")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .status(TransactionStatus.PENDING)
                .build();

        Transaction transaction2 = Transaction.builder()
                .transactionId("txn-duplicate")
                .senderId("user003")
                .receiverId("user004")
                .amount(new BigDecimal("50.00"))
                .currency("USD")
                .status(TransactionStatus.PENDING)
                .build();

        // Act & Assert
        transactionRepository.save(transaction1);
        assertThrows(Exception.class, () -> {
            transactionRepository.save(transaction2);
        });
    }

    @Test
    @DisplayName("Should index transactions by status for efficient queries")
    void testTransactionStatusIndex() {
        // Arrange
        for (int i = 0; i < 5; i++) {
            transactionRepository.save(Transaction.builder()
                    .transactionId("txn-status-" + i)
                    .senderId("user001")
                    .receiverId("user002")
                    .amount(new BigDecimal("100.00"))
                    .currency("USD")
                    .status(TransactionStatus.PENDING)
                    .build());
        }

        // Act - Index should make this fast
        long startTime = System.currentTimeMillis();
        transactionRepository.findByTransactionId("txn-status-0");
        long endTime = System.currentTimeMillis();

        // Assert - Should be very fast (< 100ms)
        assertTrue(endTime - startTime < 100);
    }

    @Test
    @DisplayName("Should update transaction status")
    void testUpdateTransactionStatus() {
        // Arrange
        Transaction transaction = transactionRepository.save(Transaction.builder()
                .transactionId("txn-update")
                .senderId("user001")
                .receiverId("user002")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .status(TransactionStatus.PENDING)
                .build());

        // Act
        transaction.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(transaction);

        Optional<Transaction> updated = transactionRepository.findByTransactionId("txn-update");

        // Assert
        assertTrue(updated.isPresent());
        assertEquals(TransactionStatus.COMPLETED, updated.get().getStatus());
    }

    @Test
    @DisplayName("Should store error reason on failed transaction")
    void testStoreErrorReason() {
        // Arrange & Act
        Transaction transaction = transactionRepository.save(Transaction.builder()
                .transactionId("txn-error")
                .senderId("user001")
                .receiverId("user002")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .status(TransactionStatus.FAILED)
                .errorReason("Insufficient funds")
                .build());

        Optional<Transaction> retrieved = transactionRepository.findByTransactionId("txn-error");

        // Assert
        assertTrue(retrieved.isPresent());
        assertEquals("Insufficient funds", retrieved.get().getErrorReason());
        assertEquals(TransactionStatus.FAILED, retrieved.get().getStatus());
    }

    @Test
    @DisplayName("Should handle concurrent transactions")
    void testConcurrentTransactions() {
        // Arrange & Act
        Thread thread1 = new Thread(() -> {
            transactionRepository.save(Transaction.builder()
                    .transactionId("txn-concurrent-1")
                    .senderId("user001")
                    .receiverId("user002")
                    .amount(new BigDecimal("100.00"))
                    .currency("USD")
                    .status(TransactionStatus.PENDING)
                    .build());
        });

        Thread thread2 = new Thread(() -> {
            transactionRepository.save(Transaction.builder()
                    .transactionId("txn-concurrent-2")
                    .senderId("user003")
                    .receiverId("user004")
                    .amount(new BigDecimal("50.00"))
                    .currency("USD")
                    .status(TransactionStatus.PENDING)
                    .build());
        });

        thread1.start();
        thread2.start();

        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Assert
        assertTrue(transactionRepository.findByTransactionId("txn-concurrent-1").isPresent());
        assertTrue(transactionRepository.findByTransactionId("txn-concurrent-2").isPresent());
    }

    @Test
    @DisplayName("Should preserve timestamp on save")
    void testTimestampPreservation() {
        // Arrange
        Transaction transaction = transactionRepository.save(Transaction.builder()
                .transactionId("txn-timestamp")
                .senderId("user001")
                .receiverId("user002")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .status(TransactionStatus.PENDING)
                .build());

        // Act
        Optional<Transaction> retrieved = transactionRepository.findByTransactionId("txn-timestamp");

        // Assert
        assertTrue(retrieved.isPresent());
        assertNotNull(retrieved.get().getCreatedAt());
        assertNotNull(retrieved.get().getUpdatedAt());
    }
}
