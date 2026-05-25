package com.trirang.service;

import com.trirang.model.dto.MatchResponse;
import com.trirang.model.entity.ArtisanRequirement;
import com.trirang.model.entity.Donation;
import com.trirang.model.entity.Match;
import com.trirang.model.entity.User;
import com.trirang.model.enums.DonationStatus;
import com.trirang.model.enums.MatchStatus;
import com.trirang.model.enums.Role;
import com.trirang.model.mapper.MatchMapper;
import com.trirang.repository.ArtisanRequirementRepository;
import com.trirang.repository.DonationRepository;
import com.trirang.repository.MatchRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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

    public void runMatchmakingForDonation(Donation donation) {
        log.info("Running matchmaking engine for donation ID: {}", donation.getId());
        if (donation.getStatus() != DonationStatus.AVAILABLE) {
            log.info("Donation is not AVAILABLE. Skipping matchmaking.");
            return;
        }

        List<ArtisanRequirement> openRequirements = requirementRepository.findAll().stream()
                .filter(req -> "OPEN".equalsIgnoreCase(req.getStatus()))
                .collect(Collectors.toList());

        for (ArtisanRequirement req : openRequirements) {
            if (matchRepository.existsByDonationIdAndRequirementId(donation.getId(), req.getId())) {
                continue;
            }

            double score = calculateMatchScore(donation, req);
            if (score >= 40.0) { // Threshold for proposing a match
                Match match = Match.builder()
                        .donation(donation)
                        .requirement(req)
                        .matchScore(BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP))
                        .status(MatchStatus.PENDING)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();
                matchRepository.save(match);
                log.info("Persisted match between donation ID: {} and requirement ID: {} with score: {}", 
                        donation.getId(), req.getId(), score);
            }
        }
    }

    public void runMatchmakingForRequirement(ArtisanRequirement req) {
        log.info("Running matchmaking engine for artisan requirement ID: {}", req.getId());
        if (!"OPEN".equalsIgnoreCase(req.getStatus())) {
            log.info("Requirement is not OPEN. Skipping matchmaking.");
            return;
        }

        List<Donation> availableDonations = donationRepository.findAll().stream()
                .filter(donation -> donation.getStatus() == DonationStatus.AVAILABLE)
                .collect(Collectors.toList());

        for (Donation donation : availableDonations) {
            if (matchRepository.existsByDonationIdAndRequirementId(donation.getId(), req.getId())) {
                continue;
            }

            double score = calculateMatchScore(donation, req);
            if (score >= 40.0) {
                Match match = Match.builder()
                        .donation(donation)
                        .requirement(req)
                        .matchScore(BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP))
                        .status(MatchStatus.PENDING)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();
                matchRepository.save(match);
                log.info("Persisted match between donation ID: {} and requirement ID: {} with score: {}", 
                        donation.getId(), req.getId(), score);
            }
        }
    }

    public synchronized MatchResponse acceptMatch(UUID matchId, User caller) {
        log.info("User ID: {} is attempting to accept Match ID: {}", caller.getId(), matchId);

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found with ID: " + matchId));

        if (match.getStatus() != MatchStatus.PENDING) {
            throw new IllegalStateException("Only PENDING matches can be accepted. Current status: " + match.getStatus());
        }

        // Authorization check: Caller must be either the donor or the artisan in this match
        boolean isDonor = match.getDonation().getDonor().getId().equals(caller.getId());
        boolean isArtisan = match.getRequirement().getArtisan().getId().equals(caller.getId());
        if (!isDonor && !isArtisan) {
            throw new IllegalStateException("You are not authorized to accept this match");
        }

        UUID donationId = match.getDonation().getId();
        UUID reqId = match.getRequirement().getId();

        // Enforce race condition & double acceptance prevention
        if (matchRepository.existsByDonationIdAndStatus(donationId, MatchStatus.ACCEPTED)) {
            throw new IllegalStateException("This donation is already matched in an accepted claim");
        }
        if (matchRepository.existsByRequirementIdAndStatus(reqId, MatchStatus.ACCEPTED)) {
            throw new IllegalStateException("This artisan requirement is already matched in an accepted claim");
        }

        // Update match status to ACCEPTED
        match.setStatus(MatchStatus.ACCEPTED);
        matchRepository.save(match);

        // Update donation status to MATCHED
        Donation donation = match.getDonation();
        donation.setStatus(DonationStatus.MATCHED);
        donationRepository.save(donation);

        // Update requirement status to FILLED/CLOSED
        ArtisanRequirement req = match.getRequirement();
        req.setStatus("CLOSED");
        requirementRepository.save(req);

        // Auto-reject all other pending matches involving this donation or requirement
        List<Match> otherMatches = new ArrayList<>();
        otherMatches.addAll(matchRepository.findByDonationId(donationId));
        otherMatches.addAll(matchRepository.findByRequirementId(reqId));

        for (Match other : otherMatches) {
            if (!other.getId().equals(matchId) && other.getStatus() == MatchStatus.PENDING) {
                other.setStatus(MatchStatus.REJECTED);
                matchRepository.save(other);
            }
        }

        log.info("Match ID: {} successfully accepted. Other conflicting pending matches auto-rejected.", matchId);
        return matchMapper.toResponse(match);
    }

    public MatchResponse rejectMatch(UUID matchId, User caller) {
        log.info("User ID: {} is rejecting Match ID: {}", caller.getId(), matchId);

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found with ID: " + matchId));

        if (match.getStatus() != MatchStatus.PENDING) {
            throw new IllegalStateException("Only PENDING matches can be rejected");
        }

        boolean isDonor = match.getDonation().getDonor().getId().equals(caller.getId());
        boolean isArtisan = match.getRequirement().getArtisan().getId().equals(caller.getId());
        if (!isDonor && !isArtisan) {
            throw new IllegalStateException("You are not authorized to reject this match");
        }

        match.setStatus(MatchStatus.REJECTED);
        Match saved = matchRepository.save(match);
        return matchMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> getMatchesForUser(User user) {
        List<Match> matches;
        if (Role.ARTISAN.name().equals(user.getRole())) {
            matches = matchRepository.findByRequirementArtisanId(user.getId());
        } else {
            matches = matchRepository.findByDonationDonorId(user.getId());
        }
        return matches.stream()
                .map(matchMapper::toResponse)
                .collect(Collectors.toList());
    }

    private double calculateMatchScore(Donation donation, ArtisanRequirement req) {
        double score = 0.0;

        // 1. Category Matching (up to 40 points)
        if (donation.getCategory() != null && donation.getCategory() == req.getCategory()) {
            score += 40.0;
        }

        // 2. Fabric Type Matching (up to 30 points)
        if (donation.getFabricType() != null && donation.getFabricType() == req.getFabricType()) {
            score += 30.0;
        }

        // 3. Location / Distance Matching (up to 30 points)
        if (donation.getLatitude() != null && donation.getLongitude() != null &&
                req.getLatitude() != null && req.getLongitude() != null) {

            double distance = calculateHaversineDistance(
                    donation.getLatitude().doubleValue(), donation.getLongitude().doubleValue(),
                    req.getLatitude().doubleValue(), req.getLongitude().doubleValue()
            );

            // If coordinates fall within travel radius, add distance score proportion
            if (distance <= req.getRadiusKm()) {
                double distanceRatio = (req.getRadiusKm() - distance) / req.getRadiusKm();
                score += (30.0 * distanceRatio);
            }
        }

        return score;
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371.0 * c;
    }
}
