package com.trirang.controller;

import com.trirang.model.dto.ArtisanRequirementRequest;
import com.trirang.model.dto.ArtisanRequirementResponse;
import com.trirang.model.entity.User;
import com.trirang.repository.UserRepository;
import com.trirang.service.ArtisanRequirementService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/artisan/requirements")
public class ArtisanRequirementController {

    private final ArtisanRequirementService requirementService;
    private final UserRepository userRepository;

    public ArtisanRequirementController(
            ArtisanRequirementService requirementService,
            UserRepository userRepository) {
        this.requirementService = requirementService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<ArtisanRequirementResponse> createRequirement(
            @Valid @RequestBody ArtisanRequirementRequest request) {
        log.info("Received request to create artisan requirement");
        User currentUser = getCurrentUser();
        ArtisanRequirementResponse response = requirementService.createRequirement(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<ArtisanRequirementResponse>> getMyRequirements() {
        log.info("Received request to get current artisan's requirements");
        User currentUser = getCurrentUser();
        List<ArtisanRequirementResponse> responses = requirementService.getMyRequirements(currentUser);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<ArtisanRequirementResponse>> getNearbyRequirements(
            @RequestParam("latitude") BigDecimal latitude,
            @RequestParam("longitude") BigDecimal longitude,
            @RequestParam(value = "radius", required = false) Double radius) {
        log.info("Received request to search nearby requirements at ({}, {}) with radius: {}", latitude, longitude, radius);
        List<ArtisanRequirementResponse> responses = requirementService.getNearbyRequirements(
                latitude.doubleValue(),
                longitude.doubleValue(),
                radius
        );
        return ResponseEntity.ok(responses);
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
