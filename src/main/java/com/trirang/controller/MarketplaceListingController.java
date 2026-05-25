package com.trirang.controller;

import com.trirang.model.dto.*;
import com.trirang.model.entity.User;
import com.trirang.model.enums.ListingType;
import com.trirang.model.enums.shared.ItemCategory;
import com.trirang.repository.UserRepository;
import com.trirang.service.ListingService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
@RequestMapping("/api/marketplace/listings")
public class MarketplaceListingController {

    private final ListingService listingService;
    private final UserRepository userRepository;

    public MarketplaceListingController(ListingService listingService, UserRepository userRepository) {
        this.listingService = listingService;
        this.userRepository = userRepository;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ListingResponse> createListing(
            @RequestParam("type") ListingType type,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("price") BigDecimal price,
            @RequestParam("category") ItemCategory category,
            @RequestParam("latitude") BigDecimal latitude,
            @RequestParam("longitude") BigDecimal longitude,
            @RequestParam(value = "originalDonationId", required = false) UUID originalDonationId,
            @RequestParam("images") List<MultipartFile> images) {

        log.info("Received request to create marketplace listing: {}", title);

        User currentUser = getCurrentUser();

        CreateListingRequest request = new CreateListingRequest(
                type, title, description, price, category, latitude, longitude, originalDonationId
        );

        ListingResponse response = listingService.createListing(currentUser, request, images);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<ListingSummaryResponse>> browseListings(
            @RequestParam(value = "lat", required = false) Double lat,
            @RequestParam(value = "lng", required = false) Double lng,
            @RequestParam(value = "radiusKm", required = false) Double radiusKm,
            @RequestParam(value = "category", required = false) ItemCategory category,
            @RequestParam(value = "type", required = false) ListingType type,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        log.info("Browsing listings request received");
        Pageable pageable = PageRequest.of(page, size);
        Page<ListingSummaryResponse> responses = listingService.browseListings(
                lat, lng, radiusKm, category, type, pageable
        );
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ListingResponse> getListing(@PathVariable("id") UUID id) {
        log.info("Fetching listing ID: {}", id);
        ListingResponse response = listingService.getListing(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ListingResponse> updateListing(
            @PathVariable("id") UUID id,
            @RequestParam(value = "version", required = false) Long version,
            @Valid @RequestBody UpdateListingRequest request) {

        log.info("Updating listing ID: {} with version: {}", id, version);
        User currentUser = getCurrentUser();
        ListingResponse response = listingService.updateListing(id, currentUser, request, version);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteListing(@PathVariable("id") UUID id) {
        log.info("Soft-deleting listing ID: {}", id);
        User currentUser = getCurrentUser();
        listingService.softDeleteListing(id, currentUser);
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
