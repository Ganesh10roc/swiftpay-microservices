package com.swiftpay.gateway.service;

import com.swiftpay.common.domain.TransactionStatus;
import com.swiftpay.common.dto.PaymentRequestDto;
import com.swiftpay.common.events.PaymentInitiatedEvent;
import com.swiftpay.common.exception.DuplicateTransactionException;
import com.swiftpay.gateway.entity.Transaction;
import com.swiftpay.gateway.kafka.PaymentEventProducer;
import com.swiftpay.gateway.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Payment Service Tests")
class PaymentServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PaymentEventProducer paymentEventProducer;

    @Mock
    private IdempotencyService idempotencyService;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(transactionRepository, paymentEventProducer, idempotencyService);
    }

    @Test
    @DisplayName("Should successfully initiate payment")
    void testInitiatePaymentSuccess() {
        // Arrange
        PaymentRequestDto request = PaymentRequestDto.builder()
                .transactionId("txn-001")
                .senderId("user001")
                .receiverId("user002")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .build();

        Transaction savedTransaction = Transaction.builder()
                .id(1L)
                .transactionId("txn-001")
                .senderId("user001")
                .receiverId("user002")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .status(TransactionStatus.PENDING)
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        // Act
        Transaction result = paymentService.initiatePayment(request);

        // Assert
        assertNotNull(result);
        assertEquals("txn-001", result.getTransactionId());
        assertEquals(TransactionStatus.PENDING, result.getStatus());
        assertEquals(new BigDecimal("100.00"), result.getAmount());

        verify(idempotencyService, times(1)).checkAndMarkIdempotent(anyString());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(paymentEventProducer, times(1)).publishPaymentInitiated(any(PaymentInitiatedEvent.class));
    }

    @Test
    @DisplayName("Should throw DuplicateTransactionException for duplicate transaction")
    void testInitiatePaymentDuplicate() {
        // Arrange
        PaymentRequestDto request = PaymentRequestDto.builder()
                .transactionId("txn-001")
                .senderId("user001")
                .receiverId("user002")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .build();

        doThrow(new DuplicateTransactionException("txn-001"))
                .when(idempotencyService).checkAndMarkIdempotent(anyString());

        // Act & Assert
        assertThrows(DuplicateTransactionException.class, () -> {
            paymentService.initiatePayment(request);
        });

        verify(transactionRepository, never()).save(any());
        verify(paymentEventProducer, never()).publishPaymentInitiated(any());
    }

    @Test
    @DisplayName("Should generate transaction ID if not provided")
    void testInitiatePaymentWithoutTransactionId() {
        // Arrange
        PaymentRequestDto request = PaymentRequestDto.builder()
                .transactionId(null)
                .senderId("user001")
                .receiverId("user002")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .build();

        Transaction savedTransaction = Transaction.builder()
                .id(1L)
                .transactionId(UUID.randomUUID().toString())
                .senderId("user001")
                .receiverId("user002")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .status(TransactionStatus.PENDING)
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        // Act
        Transaction result = paymentService.initiatePayment(request);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getTransactionId());
        assertTrue(result.getTransactionId().length() > 0);
    }

    @Test
    @DisplayName("Should retrieve transaction by ID")
    void testGetTransaction() {
        // Arrange
        Transaction transaction = Transaction.builder()
                .id(1L)
                .transactionId("txn-001")
                .senderId("user001")
                .receiverId("user002")
                .amount(new BigDecimal("100.00"))
                .status(TransactionStatus.COMPLETED)
                .build();

        when(transactionRepository.findByTransactionId("txn-001"))
                .thenReturn(Optional.of(transaction));

        // Act
        Transaction result = paymentService.getTransaction("txn-001");

        // Assert
        assertNotNull(result);
        assertEquals("txn-001", result.getTransactionId());
        assertEquals(TransactionStatus.COMPLETED, result.getStatus());
    }

    @Test
    @DisplayName("Should throw exception when transaction not found")
    void testGetTransactionNotFound() {
        // Arrange
        when(transactionRepository.findByTransactionId("invalid-id"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            paymentService.getTransaction("invalid-id");
        });
    }

    @Test
    @DisplayName("Should update transaction status")
    void testUpdateTransactionStatus() {
        // Arrange
        Transaction transaction = Transaction.builder()
                .id(1L)
                .transactionId("txn-001")
                .status(TransactionStatus.PENDING)
                .build();

        when(transactionRepository.findByTransactionId("txn-001"))
                .thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(transaction);

        // Act
        paymentService.updateTransactionStatus("txn-001", TransactionStatus.COMPLETED, null);

        // Assert
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        assertEquals(TransactionStatus.COMPLETED, transaction.getStatus());
    }

    @Test
    @DisplayName("Should update transaction status with error reason")
    void testUpdateTransactionStatusWithError() {
        // Arrange
        Transaction transaction = Transaction.builder()
                .id(1L)
                .transactionId("txn-001")
                .status(TransactionStatus.PENDING)
                .build();

        when(transactionRepository.findByTransactionId("txn-001"))
                .thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(transaction);

        // Act
        paymentService.updateTransactionStatus("txn-001", TransactionStatus.FAILED, "Insufficient funds");

        // Assert
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());

        Transaction saved = captor.getValue();
        assertEquals(TransactionStatus.FAILED, saved.getStatus());
        assertEquals("Insufficient funds", saved.getErrorReason());
    }

    @Test
    @DisplayName("Should handle large payment amounts")
    void testInitiatePaymentLargeAmount() {
        // Arrange
        PaymentRequestDto request = PaymentRequestDto.builder()
                .transactionId("txn-large")
                .senderId("user001")
                .receiverId("user002")
                .amount(new BigDecimal("999999.99"))
                .currency("USD")
                .build();

        Transaction savedTransaction = Transaction.builder()
                .id(1L)
                .transactionId("txn-large")
                .amount(new BigDecimal("999999.99"))
                .status(TransactionStatus.PENDING)
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        // Act
        Transaction result = paymentService.initiatePayment(request);

        // Assert
        assertEquals(new BigDecimal("999999.99"), result.getAmount());
    }

    @Test
    @DisplayName("Should handle small payment amounts")
    void testInitiatePaymentSmallAmount() {
        // Arrange
        PaymentRequestDto request = PaymentRequestDto.builder()
                .transactionId("txn-small")
                .senderId("user001")
                .receiverId("user002")
                .amount(new BigDecimal("0.01"))
                .currency("USD")
                .build();

        Transaction savedTransaction = Transaction.builder()
                .id(1L)
                .transactionId("txn-small")
                .amount(new BigDecimal("0.01"))
                .status(TransactionStatus.PENDING)
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        // Act
        Transaction result = paymentService.initiatePayment(request);

        // Assert
        assertEquals(new BigDecimal("0.01"), result.getAmount());
    }
}
