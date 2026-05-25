package com.trirang.controller;

import com.trirang.model.dto.DonationResponse;
import com.trirang.model.dto.RecyclerClaimResponse;
import com.trirang.model.entity.User;
import com.trirang.repository.UserRepository;
import com.trirang.service.RecyclerClaimService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/donations")
public class RecyclerController {

    private final RecyclerClaimService claimService;
    private final UserRepository userRepository;

    public RecyclerController(RecyclerClaimService claimService, UserRepository userRepository) {
        this.claimService = claimService;
        this.userRepository = userRepository;
    }

    @GetMapping("/recyclables")
    public ResponseEntity<List<DonationResponse>> getRecyclables(
            @RequestParam(value = "latitude", required = false) BigDecimal latitude,
            @RequestParam(value = "longitude", required = false) BigDecimal longitude,
            @RequestParam(value = "radius", required = false) Double radius) {

        log.info("Received request to fetch recyclable donations around ({}, {}) with radius: {}", latitude, longitude, radius);
        
        // If coordinate parameters are not passed, default to the current user's location
        double searchLat = 0.0;
        double searchLng = 0.0;
        if (latitude != null && longitude != null) {
            searchLat = latitude.doubleValue();
            searchLng = longitude.doubleValue();
        } else {
            User currentUser = getCurrentUser();
            if (currentUser.getLatitude() != null && currentUser.getLongitude() != null) {
                searchLat = currentUser.getLatitude().doubleValue();
                searchLng = currentUser.getLongitude().doubleValue();
            }
        }

        List<DonationResponse> responses = claimService.getRecyclableDonations(searchLat, searchLng, radius);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{id}/claim")
    public ResponseEntity<RecyclerClaimResponse> claimDonation(@PathVariable("id") UUID id) {
        log.info("Received request to claim donation ID: {}", id);
        User currentUser = getCurrentUser();
        RecyclerClaimResponse response = claimService.claimDonation(currentUser, id);
        return ResponseEntity.ok(response);
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
