package com.trirang.controller;

import com.trirang.model.dto.request.UserBanRequest;
import com.trirang.model.dto.response.AdminAnalyticsResponse;
import com.trirang.model.entity.Listing;
import com.trirang.model.entity.User;
import com.trirang.model.entity.Verification;
import com.trirang.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<User>> getAllUsers(Pageable pageable) {
        return ResponseEntity.ok(adminService.getAllUsers(pageable));
    }

    @PutMapping("/users/{id}/ban")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> banUser(
            @PathVariable UUID id,
            @Valid @RequestBody UserBanRequest request) {
        adminService.banUser(id, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/verifications")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Verification>> getPendingVerifications(Pageable pageable) {
        return ResponseEntity.ok(adminService.getPendingVerifications(pageable));
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminAnalyticsResponse> getAnalytics() {
        return ResponseEntity.ok(adminService.getAnalytics());
    }

    @GetMapping("/listings/pending-review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Listing>> getPendingListings(Pageable pageable) {
        return ResponseEntity.ok(adminService.getPendingListings(pageable));
    }
}
