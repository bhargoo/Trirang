package com.trirang.controller;

import com.trirang.model.dto.NgoImpactResponse;
import com.trirang.model.entity.User;
import com.trirang.repository.UserRepository;
import com.trirang.service.RecyclerClaimService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/ngos")
public class NGOController {

    private final RecyclerClaimService claimService;
    private final UserRepository userRepository;

    public NGOController(RecyclerClaimService claimService, UserRepository userRepository) {
        this.claimService = claimService;
        this.userRepository = userRepository;
    }

    @GetMapping("/impact")
    public ResponseEntity<NgoImpactResponse> getNgoImpact() {
        log.info("Received request to fetch NGO impact statistics");
        User currentUser = getCurrentUser();
        NgoImpactResponse response = claimService.getNgoImpact(currentUser);
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
