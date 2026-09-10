package com.swiftpay.ledger.repository;

import com.swiftpay.ledger.entity.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    Optional<LedgerEntry> findByTransactionId(String transactionId);

    @Query("SELECT l FROM LedgerEntry l WHERE l.userId = :userId ORDER BY l.createdAt DESC")
    Page<LedgerEntry> findByUserId(@Param("userId") String userId, Pageable pageable);
}
