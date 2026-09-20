package com.swiftpay.ledger.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", unique = true, nullable = false, length = 100)
    private String userId;

    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "version")
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (version == null) {
            version = 0L;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public BigDecimal getBalance() {
        return this.balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return this.userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCurrency() {
        return this.currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Long getVersion() {
        return this.version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static AccountBuilder builder() {
        return new AccountBuilder();
    }

    public static class AccountBuilder {
        private Long id;
        private String userId;
        private BigDecimal balance;
        private String currency;
        private Long version;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public AccountBuilder id(Long id) { this.id = id; return this; }
        public AccountBuilder userId(String userId) { this.userId = userId; return this; }
        public AccountBuilder balance(BigDecimal balance) { this.balance = balance; return this; }
        public AccountBuilder currency(String currency) { this.currency = currency; return this; }
        public AccountBuilder version(Long version) { this.version = version; return this; }
        public AccountBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public AccountBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public Account build() {
            Account account = new Account();
            account.id = this.id;
            account.userId = this.userId;
            account.balance = this.balance;
            account.currency = this.currency;
            account.version = this.version;
            account.createdAt = this.createdAt;
            account.updatedAt = this.updatedAt;
            return account;
        }
    }
}
