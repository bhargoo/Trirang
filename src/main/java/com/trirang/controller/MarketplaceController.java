package com.trirang.controller;

import com.trirang.model.dto.MatchResponse;
import com.trirang.model.entity.ArtisanRequirement;
import com.trirang.model.entity.Donation;
import com.trirang.model.entity.User;
import com.trirang.model.enums.Role;
import com.trirang.repository.ArtisanRequirementRepository;
import com.trirang.repository.DonationRepository;
import com.trirang.repository.UserRepository;
import com.trirang.service.MatchmakingEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/marketplace")
public class MarketplaceController {

    private final MatchmakingEngine matchmakingEngine;
    private final UserRepository userRepository;
    private final DonationRepository donationRepository;
    private final ArtisanRequirementRepository requirementRepository;

    public MarketplaceController(
            MatchmakingEngine matchmakingEngine,
            UserRepository userRepository,
            DonationRepository donationRepository,
            ArtisanRequirementRepository requirementRepository) {
        this.matchmakingEngine = matchmakingEngine;
        this.userRepository = userRepository;
        this.donationRepository = donationRepository;
        this.requirementRepository = requirementRepository;
    }

    @PostMapping("/matches/{id}/accept")
    public ResponseEntity<MatchResponse> acceptMatch(@PathVariable("id") UUID id) {
        log.info("Request to accept match ID: {}", id);
        User currentUser = getCurrentUser();
        MatchResponse response = matchmakingEngine.acceptMatch(id, currentUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/matches/{id}/reject")
    public ResponseEntity<MatchResponse> rejectMatch(@PathVariable("id") UUID id) {
        log.info("Request to reject match ID: {}", id);
        User currentUser = getCurrentUser();
        MatchResponse response = matchmakingEngine.rejectMatch(id, currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/matches")
    public ResponseEntity<List<MatchResponse>> getMatches() {
        log.info("Request to get matches for current user");
        User currentUser = getCurrentUser();
        List<MatchResponse> responses = matchmakingEngine.getMatchesForUser(currentUser);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/matchmaking/run")
    public ResponseEntity<Void> runMatchmaking() {
        log.info("Manual matchmaking run requested");
        User currentUser = getCurrentUser();
        
        if (Role.ARTISAN.name().equals(currentUser.getRole())) {
            List<ArtisanRequirement> requirements = requirementRepository.findByArtisanId(currentUser.getId());
            for (ArtisanRequirement req : requirements) {
                matchmakingEngine.runMatchmakingForRequirement(req);
            }
        } else {
            List<Donation> donations = donationRepository.findByDonorId(currentUser.getId());
            for (Donation don : donations) {
                matchmakingEngine.runMatchmakingForDonation(don);
            }
        }
        
        return ResponseEntity.noContent().build();
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated");
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found in database"));
    }
}
