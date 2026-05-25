package com.trirang.service;

import com.trirang.model.dto.MatchResponse;
import com.trirang.model.entity.ArtisanRequirement;
import com.trirang.model.entity.Donation;
import com.trirang.model.entity.Match;
import com.trirang.model.entity.User;
import com.trirang.model.enums.DonationStatus;
import com.trirang.model.enums.MatchStatus;
import com.trirang.model.mapper.MatchMapper;
import com.trirang.repository.ArtisanRequirementRepository;
import com.trirang.repository.DonationRepository;
import com.trirang.repository.MatchRepository;
import com.trirang.util.DistanceCalculationUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class MatchmakingEngine {

    private final MatchRepository matchRepository;
    private final DonationRepository donationRepository;
    private final ArtisanRequirementRepository requirementRepository;
    private final MatchMapper matchMapper;

    public MatchmakingEngine(
            MatchRepository matchRepository,
            DonationRepository donationRepository,
            ArtisanRequirementRepository requirementRepository,
            MatchMapper matchMapper) {
        this.matchRepository = matchRepository;
        this.donationRepository = donationRepository;
        this.requirementRepository = requirementRepository;
        this.matchMapper = matchMapper;
    }

    /**
     * Executes the matchmaking logic globally or for a specific donation.
     */
    public List<MatchResponse> runMatchmaking(Donation donation) {
        log.info("Running matchmaking engine for donation ID: {}", donation.getId());

        if (donation.getStatus() != DonationStatus.AVAILABLE) {
            log.warn("Donation {} is not AVAILABLE for matchmaking", donation.getId());
            return new ArrayList<>();
        }

        List<ArtisanRequirement> openRequirements = requirementRepository.findAll().stream()
                .filter(req -> "OPEN".equalsIgnoreCase(req.getStatus()))
                .collect(Collectors.toList());

        List<Match> newMatches = new ArrayList<>();

        for (ArtisanRequirement req : openRequirements) {
            // 1. Compatibility Matching (fabricType or category)
            boolean compatible = false;
            if (req.getCategory() != null && req.getCategory() == donation.getCategory()) {
                compatible = true;
            }
            if (req.getFabricType() != null && req.getFabricType() == donation.getFabricType()) {
                compatible = true;
            }

            if (!compatible) {
                continue;
            }

            // 2. Distance check
            if (donation.getLatitude() == null || donation.getLongitude() == null ||
                    req.getLatitude() == null || req.getLongitude() == null) {
                continue;
            }

            double distance = DistanceCalculationUtil.calculateDistance(
                    donation.getLatitude().doubleValue(),
                    donation.getLongitude().doubleValue(),
                    req.getLatitude().doubleValue(),
                    req.getLongitude().doubleValue()
            );

            // Radius filtering
            if (distance > req.getRadiusKm()) {
                continue;
            }

            // 3. Prevent duplicate active matches
            boolean matchExists = matchRepository.existsByDonationIdAndArtisanRequirementIdAndStatusIn(
                    donation.getId(),
                    req.getId(),
                    Set.of(MatchStatus.PENDING, MatchStatus.ACCEPTED, MatchStatus.COMPLETED)
            );

            if (matchExists) {
                continue;
            }

            // 4. Calculate Score
            double urgencyScore = req.getUrgency() != null ? req.getUrgency() * 10.0 : 30.0;
            double distanceScore = (1.0 - (distance / req.getRadiusKm())) * 40.0;
            double categoryMatchBonus = (req.getCategory() == donation.getCategory()) ? 30.0 : 0.0;
            double score = Math.min(100.0, urgencyScore + distanceScore + categoryMatchBonus);

            Match match = Match.builder()
                    .donation(donation)
                    .artisanRequirement(req)
                    .donor(donation.getDonor())
                    .artisan(req.getArtisan())
                    .status(MatchStatus.PENDING)
                    .matchScore(score)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            newMatches.add(matchRepository.save(match));
        }

        log.info("Matchmaking complete. Created {} new matches.", newMatches.size());
        return newMatches.stream().map(matchMapper::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<MatchResponse> getMyMatches(User user, Pageable pageable) {
        log.info("Fetching matches page for user ID: {}", user.getId());
        return matchRepository.findByUserId(user.getId(), pageable)
                .map(matchMapper::toResponse);
    }

    public MatchResponse acceptMatch(UUID matchId, User user, Long expectedVersion) {
        log.info("User {} accepting match ID: {}", user.getId(), matchId);

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found with ID: " + matchId));

        // Ownership validation
        if (!match.getDonor().getId().equals(user.getId()) && !match.getArtisan().getId().equals(user.getId())) {
            throw new IllegalStateException("You are not authorized to accept this match");
        }

        // Double acceptance prevention
        if (match.getStatus() != MatchStatus.PENDING) {
            throw new IllegalStateException("Only PENDING matches can be accepted. Current status: " + match.getStatus());
        }

        // Optimistic locking version validation
        if (expectedVersion != null && !expectedVersion.equals(match.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(Match.class, matchId);
        }

        // Check if the donation is still AVAILABLE
        Donation donation = match.getDonation();
        if (donation.getStatus() != DonationStatus.AVAILABLE) {
            throw new IllegalStateException("The donation is no longer AVAILABLE for matching");
        }

        // Update match status
        match.setStatus(MatchStatus.ACCEPTED);
        match.setUpdatedAt(Instant.now());
        Match saved = matchRepository.save(match);

        // Update donation status
        donation.setStatus(DonationStatus.MATCHED);
        donationRepository.save(donation);

        // Automatically reject/cancel other PENDING matches for this donation to prevent race conditions & duplicate accepted matches
        List<Match> otherMatches = matchRepository.findByDonationId(donation.getId());
        for (Match other : otherMatches) {
            if (!other.getId().equals(match.getId()) && other.getStatus() == MatchStatus.PENDING) {
                other.setStatus(MatchStatus.CANCELLED);
                other.setUpdatedAt(Instant.now());
                matchRepository.save(other);
            }
        }

        log.info("Match ID: {} accepted successfully.", matchId);
        return matchMapper.toResponse(saved);
    }

    public MatchResponse rejectMatch(UUID matchId, User user) {
        log.info("User {} rejecting match ID: {}", user.getId(), matchId);

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found with ID: " + matchId));

        // Ownership validation
        if (!match.getDonor().getId().equals(user.getId()) && !match.getArtisan().getId().equals(user.getId())) {
            throw new IllegalStateException("You are not authorized to reject this match");
        }

        if (match.getStatus() != MatchStatus.PENDING) {
            throw new IllegalStateException("Only PENDING matches can be rejected");
        }

        match.setStatus(MatchStatus.REJECTED);
        match.setUpdatedAt(Instant.now());
        Match saved = matchRepository.save(match);

        log.info("Match ID: {} rejected successfully.", matchId);
        return matchMapper.toResponse(saved);
    }

    public MatchResponse completeMatch(UUID matchId, User user) {
        log.info("User {} completing match ID: {}", user.getId(), matchId);

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found with ID: " + matchId));

        // Ownership validation
        if (!match.getDonor().getId().equals(user.getId()) && !match.getArtisan().getId().equals(user.getId())) {
            throw new IllegalStateException("You are not authorized to complete this match");
        }

        if (match.getStatus() != MatchStatus.ACCEPTED) {
            throw new IllegalStateException("Only ACCEPTED matches can be marked as COMPLETED");
        }

        match.setStatus(MatchStatus.COMPLETED);
        match.setUpdatedAt(Instant.now());
        Match saved = matchRepository.save(match);

        // Update donation status to COMPLETED
        Donation donation = match.getDonation();
        donation.setStatus(DonationStatus.COMPLETED);
        donationRepository.save(donation);

        log.info("Match ID: {} completed successfully.", matchId);
        return matchMapper.toResponse(saved);
    }
}
