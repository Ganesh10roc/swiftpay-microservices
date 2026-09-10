package com.swiftpay.gateway.repository;

import com.swiftpay.gateway.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByTransactionId(String transactionId);

    @Query("SELECT t FROM Transaction t WHERE t.senderId = :userId OR t.receiverId = :userId ORDER BY t.createdAt DESC")
    Page<Transaction> findByUserId(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.senderId = :userId ORDER BY t.createdAt DESC")
    Page<Transaction> findBySenderId(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.receiverId = :userId ORDER BY t.createdAt DESC")
    Page<Transaction> findByReceiverId(@Param("userId") String userId, Pageable pageable);
}
