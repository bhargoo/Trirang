package com.trirang.repository;

import com.trirang.model.entity.TriCoinLedger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<TriCoinLedger, UUID> {
    Page<TriCoinLedger> findByUserId(UUID userId, Pageable pageable);

    @Query("SELECT t FROM TriCoinLedger t WHERE t.user.id = :userId ORDER BY t.createdAt DESC, t.id DESC LIMIT 1")
    Optional<TriCoinLedger> findLatestTransactionByUserId(@Param("userId") UUID userId);
}
