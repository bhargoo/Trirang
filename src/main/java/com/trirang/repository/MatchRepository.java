package com.trirang.repository;

import com.trirang.model.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MatchRepository extends JpaRepository<Match, UUID> {
    List<Match> findByDonationId(UUID donationId);
    List<Match> findByRequirementId(UUID requirementId);
}
