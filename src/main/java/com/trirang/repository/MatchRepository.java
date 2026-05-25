package com.trirang.repository;

import com.trirang.model.entity.Match;
import com.trirang.model.enums.MatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface MatchRepository extends JpaRepository<Match, UUID> {

    boolean existsByDonationIdAndArtisanRequirementIdAndStatusIn(
            UUID donationId, UUID artisanRequirementId, Collection<MatchStatus> statuses);

    @Query("SELECT m FROM Match m WHERE m.donor.id = :userId OR m.artisan.id = :userId")
    Page<Match> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    List<Match> findByDonationId(UUID donationId);
}
