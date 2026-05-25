package com.trirang.repository;

import com.trirang.model.entity.RecyclerClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RecyclerClaimRepository extends JpaRepository<RecyclerClaim, UUID> {
    boolean existsByDonationId(UUID donationId);
    long countByRecyclerId(UUID recyclerId);
    List<RecyclerClaim> findByRecyclerId(UUID recyclerId);
}
