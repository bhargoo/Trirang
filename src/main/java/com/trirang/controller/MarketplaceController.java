package com.trirang.controller;

import com.trirang.model.dto.MatchResponse;
import com.trirang.model.entity.User;
import com.trirang.repository.UserRepository;
import com.trirang.service.MatchmakingEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/marketplace/matches")
public class MarketplaceController {

    private final MatchmakingEngine matchmakingEngine;
    private final UserRepository userRepository;

    public MarketplaceController(MatchmakingEngine matchmakingEngine, UserRepository userRepository) {
        this.matchmakingEngine = matchmakingEngine;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<Page<MatchResponse>> getMyMatches(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "direction", defaultValue = "DESC") String direction) {

        log.info("Received request to fetch matches page: {}, size: {}, sort: {}, direction: {}", page, size, sortBy, direction);
        
        User currentUser = getCurrentUser();
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<MatchResponse> responses = matchmakingEngine.getMyMatches(currentUser, pageable);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<MatchResponse> acceptMatch(
            @PathVariable("id") UUID id,
            @RequestParam(value = "version", required = false) Long version) {

        log.info("Received request to accept match ID: {} with version: {}", id, version);
        User currentUser = getCurrentUser();
        MatchResponse response = matchmakingEngine.acceptMatch(id, currentUser, version);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<MatchResponse> rejectMatch(@PathVariable("id") UUID id) {
        log.info("Received request to reject match ID: {}", id);
        User currentUser = getCurrentUser();
        MatchResponse response = matchmakingEngine.rejectMatch(id, currentUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<MatchResponse> completeMatch(@PathVariable("id") UUID id) {
        log.info("Received request to complete match ID: {}", id);
        User currentUser = getCurrentUser();
        MatchResponse response = matchmakingEngine.completeMatch(id, currentUser);
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
