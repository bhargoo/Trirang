package com.trirang.repository;

import com.trirang.model.entity.Match;
import com.trirang.model.enums.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MatchRepository extends JpaRepository<Match, UUID> {
    List<Match> findByDonationId(UUID donationId);
    List<Match> findByRequirementId(UUID requirementId);
    
    boolean existsByDonationIdAndRequirementId(UUID donationId, UUID requirementId);
    boolean existsByDonationIdAndStatus(UUID donationId, MatchStatus status);
    boolean existsByRequirementIdAndStatus(UUID requirementId, MatchStatus status);
    
    List<Match> findByRequirementArtisanId(UUID artisanId);
    List<Match> findByDonationDonorId(UUID donorId);
}

