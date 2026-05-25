package com.trirang.controller;

import com.trirang.model.dto.DonationRequest;
import com.trirang.model.dto.DonationResponse;
import com.trirang.model.entity.User;
import com.trirang.model.enums.shared.Classification;
import com.trirang.model.enums.shared.FabricType;
import com.trirang.model.enums.shared.ItemCategory;
import com.trirang.repository.UserRepository;
import com.trirang.service.DonationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/donations")
public class DonationController {

    private final DonationService donationService;
    private final UserRepository userRepository;

    public DonationController(DonationService donationService, UserRepository userRepository) {
        this.donationService = donationService;
        this.userRepository = userRepository;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DonationResponse> createDonation(
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("condition") String condition,
            @RequestParam("category") ItemCategory category,
            @RequestParam("fabricType") FabricType fabricType,
            @RequestParam("classification") Classification classification,
            @RequestParam("latitude") BigDecimal latitude,
            @RequestParam("longitude") BigDecimal longitude,
            @RequestParam("image") MultipartFile image) {

        log.info("Received donation creation request with title: {}", title);

        User currentUser = getCurrentUser();

        DonationRequest request = new DonationRequest(
                title,
                description,
                condition,
                category,
                fabricType,
                classification,
                latitude,
                longitude
        );

        DonationResponse response = donationService.createDonation(currentUser, request, image);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<DonationResponse>> getMyDonations() {
        log.info("Fetching donations for current user");
        User currentUser = getCurrentUser();
        List<DonationResponse> responses = donationService.getMyDonations(currentUser);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DonationResponse> getDonation(@PathVariable("id") UUID id) {
        log.info("Fetching donation with ID: {}", id);
        DonationResponse response = donationService.getDonation(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDonation(@PathVariable("id") UUID id) {
        log.info("Deleting donation with ID: {}", id);
        User currentUser = getCurrentUser();
        donationService.deleteDonation(id, currentUser);
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
