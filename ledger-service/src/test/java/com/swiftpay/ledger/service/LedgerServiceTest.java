package com.swiftpay.ledger.service;

import com.swiftpay.common.domain.TransactionStatus;
import com.swiftpay.common.events.PaymentInitiatedEvent;
import com.swiftpay.ledger.entity.Account;
import com.swiftpay.ledger.kafka.PaymentEventPublisher;
import com.swiftpay.ledger.repository.AccountRepository;
import com.swiftpay.ledger.repository.LedgerEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Ledger Service Tests")
class LedgerServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private PaymentEventPublisher paymentEventPublisher;

    private LedgerService ledgerService;

    @BeforeEach
    void setUp() {
        ledgerService = new LedgerService(accountRepository, ledgerEntryRepository, paymentEventPublisher);
    }

    @Test
    @DisplayName("Should process payment successfully when balance is sufficient")
    void testProcessPaymentSuccessful() {
        // Arrange
        Account senderAccount = Account.builder()
                .id(1L)
                .userId("user001")
                .balance(new BigDecimal("1000.00"))
                .currency("USD")
                .build();

        Account receiverAccount = Account.builder()
                .id(2L)
                .userId("user002")
                .balance(new BigDecimal("500.00"))
                .currency("USD")
                .build();

        PaymentInitiatedEvent event = PaymentInitiatedEvent.builder()
                .transactionId("txn-001")
                .senderId("user001")
                .receiverId("user002")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .timestamp(LocalDateTime.now())
                .build();

        when(accountRepository.findByUserId("user001"))
                .thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByUserId("user002"))
                .thenReturn(Optional.of(receiverAccount));
        when(accountRepository.save(any(Account.class)))
                .thenReturn(senderAccount);

        // Act
        ledgerService.processPayment(event);

        // Assert
        verify(accountRepository, atLeast(2)).save(any(Account.class));
        verify(ledgerEntryRepository, times(2)).save(any());
        verify(paymentEventPublisher, times(1)).publishPaymentCompleted(any());
        verify(paymentEventPublisher, never()).publishPaymentFailed(any());
    }

    @Test
    @DisplayName("Should fail payment when insufficient funds")
    void testProcessPaymentInsufficientFunds() {
        // Arrange
        Account senderAccount = Account.builder()
                .id(1L)
                .userId("user001")
                .balance(new BigDecimal("50.00"))
                .currency("USD")
                .build();

        Account receiverAccount = Account.builder()
                .id(2L)
                .userId("user002")
                .balance(new BigDecimal("500.00"))
                .currency("USD")
                .build();

        PaymentInitiatedEvent event = PaymentInitiatedEvent.builder()
                .transactionId("txn-002")
                .senderId("user001")
                .receiverId("user002")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .build();

        when(accountRepository.findByUserId("user001"))
                .thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByUserId("user002"))
                .thenReturn(Optional.of(receiverAccount));

        // Act
        ledgerService.processPayment(event);

        // Assert
        verify(accountRepository, never()).save(any());
        verify(ledgerEntryRepository, never()).save(any());
        verify(paymentEventPublisher, times(1)).publishPaymentFailed(any());
        verify(paymentEventPublisher, never()).publishPaymentCompleted(any());
    }

    @Test
    @DisplayName("Should create new account if not exists")
    void testProcessPaymentCreateNewAccount() {
        // Arrange
        Account newAccount = Account.builder()
                .id(3L)
                .userId("user003")
                .balance(new BigDecimal("10000.00"))
                .currency("USD")
                .build();

        Account receiverAccount = Account.builder()
                .id(2L)
                .userId("user002")
                .balance(new BigDecimal("500.00"))
                .currency("USD")
                .build();

        PaymentInitiatedEvent event = PaymentInitiatedEvent.builder()
                .transactionId("txn-003")
                .senderId("user003")
                .receiverId("user002")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .build();

        when(accountRepository.findByUserId("user003"))
                .thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class)))
                .thenReturn(newAccount);
        when(accountRepository.findByUserId("user002"))
                .thenReturn(Optional.of(receiverAccount));

        // Act
        ledgerService.processPayment(event);

        // Assert
        verify(accountRepository, atLeast(3)).save(any(Account.class));
    }

    @Test
    @DisplayName("Should handle exact balance payment")
    void testProcessPaymentExactBalance() {
        // Arrange
        Account senderAccount = Account.builder()
                .id(1L)
                .userId("user001")
                .balance(new BigDecimal("100.00"))
                .currency("USD")
                .build();

        Account receiverAccount = Account.builder()
                .id(2L)
                .userId("user002")
                .balance(new BigDecimal("500.00"))
                .currency("USD")
                .build();

        PaymentInitiatedEvent event = PaymentInitiatedEvent.builder()
                .transactionId("txn-exact")
                .senderId("user001")
                .receiverId("user002")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .build();

        when(accountRepository.findByUserId("user001"))
                .thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByUserId("user002"))
                .thenReturn(Optional.of(receiverAccount));
        when(accountRepository.save(any(Account.class)))
                .thenReturn(senderAccount);

        // Act
        ledgerService.processPayment(event);

        // Assert
        verify(paymentEventPublisher, times(1)).publishPaymentCompleted(any());
    }

    @Test
    @DisplayName("Should retrieve account by user ID")
    void testGetAccount() {
        // Arrange
        Account account = Account.builder()
                .id(1L)
                .userId("user001")
                .balance(new BigDecimal("1000.00"))
                .currency("USD")
                .build();

        when(accountRepository.findByUserId("user001"))
                .thenReturn(Optional.of(account));

        // Act
        Account result = ledgerService.getAccount("user001");

        // Assert
        assertNotNull(result);
        assertEquals("user001", result.getUserId());
        assertEquals(new BigDecimal("1000.00"), result.getBalance());
    }

    @Test
    @DisplayName("Should throw exception when account not found")
    void testGetAccountNotFound() {
        // Arrange
        when(accountRepository.findByUserId("nonexistent"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            ledgerService.getAccount("nonexistent");
        });
    }

    @Test
    @DisplayName("Should handle zero balance")
    void testProcessPaymentZeroBalance() {
        // Arrange
        Account senderAccount = Account.builder()
                .id(1L)
                .userId("user001")
                .balance(BigDecimal.ZERO)
                .currency("USD")
                .build();

        Account receiverAccount = Account.builder()
                .id(2L)
                .userId("user002")
                .balance(new BigDecimal("500.00"))
                .currency("USD")
                .build();

        PaymentInitiatedEvent event = PaymentInitiatedEvent.builder()
                .transactionId("txn-zero")
                .senderId("user001")
                .receiverId("user002")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .build();

        when(accountRepository.findByUserId("user001"))
                .thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByUserId("user002"))
                .thenReturn(Optional.of(receiverAccount));

        // Act
        ledgerService.processPayment(event);

        // Assert
        verify(paymentEventPublisher, times(1)).publishPaymentFailed(any());
    }

    @Test
    @DisplayName("Should handle negative balance comparison edge case")
    void testProcessPaymentNegativeComparison() {
        // Arrange
        Account senderAccount = Account.builder()
                .id(1L)
                .userId("user001")
                .balance(new BigDecimal("99.99"))
                .currency("USD")
                .build();

        Account receiverAccount = Account.builder()
                .id(2L)
                .userId("user002")
                .balance(new BigDecimal("500.00"))
                .currency("USD")
                .build();

        PaymentInitiatedEvent event = PaymentInitiatedEvent.builder()
                .transactionId("txn-edge")
                .senderId("user001")
                .receiverId("user002")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .build();

        when(accountRepository.findByUserId("user001"))
                .thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByUserId("user002"))
                .thenReturn(Optional.of(receiverAccount));

        // Act
        ledgerService.processPayment(event);

        // Assert
        verify(paymentEventPublisher, times(1)).publishPaymentFailed(any());
    }
}
