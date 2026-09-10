package com.swiftpay.ledger.repository;

import com.swiftpay.ledger.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    @Query("SELECT a FROM Account a WHERE a.userId = :userId")
    Optional<Account> findByUserId(@Param("userId") String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.userId = :userId")
    Optional<Account> findByUserIdForUpdate(@Param("userId") String userId);
}
