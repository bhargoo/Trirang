package com.trirang.controller;

import com.trirang.model.dto.ArtisanVerificationResponse;
import com.trirang.model.dto.RejectVerificationRequest;
import com.trirang.model.enums.VerificationStatus;
import com.trirang.service.ArtisanVerificationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/admin/verifications")
@PreAuthorize("hasRole('ADMIN')")
public class AdminVerificationController {

    private final ArtisanVerificationService verificationService;

    public AdminVerificationController(ArtisanVerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @GetMapping
    public ResponseEntity<List<ArtisanVerificationResponse>> getAllVerifications(
            @RequestParam(value = "status", required = false) VerificationStatus status) {
        log.info("Admin request to get all artisan verifications with status: {}", status);
        List<ArtisanVerificationResponse> responses = verificationService.getAllVerifications(status);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ArtisanVerificationResponse> approveVerification(@PathVariable("id") UUID id) {
        log.info("Admin request to approve artisan verification ID: {}", id);
        ArtisanVerificationResponse response = verificationService.approveVerification(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ArtisanVerificationResponse> rejectVerification(
            @PathVariable("id") UUID id,
            @Valid @RequestBody RejectVerificationRequest request) {
        log.info("Admin request to reject artisan verification ID: {} with reason: {}", id, request.rejectionReason());
        ArtisanVerificationResponse response = verificationService.rejectVerification(id, request.rejectionReason());
        return ResponseEntity.ok(response);
    }
}
