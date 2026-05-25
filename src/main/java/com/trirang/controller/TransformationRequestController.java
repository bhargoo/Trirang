package com.trirang.controller;

import com.trirang.model.dto.*;
import com.trirang.model.entity.User;
import com.trirang.model.enums.TransformationProgress;
import com.trirang.repository.UserRepository;
import com.trirang.service.TransformationRequestService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/transformations")
public class TransformationRequestController {

    private final TransformationRequestService transformationService;
    private final UserRepository userRepository;

    public TransformationRequestController(
            TransformationRequestService transformationService, UserRepository userRepository) {
        this.transformationService = transformationService;
        this.userRepository = userRepository;
    }

    @PostMapping(value = "/request", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TransformationResponse> createRequest(
            @RequestParam("artisanId") UUID artisanId,
            @RequestParam("donationId") UUID donationId,
            @RequestParam("customizationRequest") String customizationRequest,
            @RequestParam(value = "beforeFiles", required = false) List<MultipartFile> beforeFiles) {

        log.info("Received request to create transformation request");
        User currentUser = getCurrentUser();

        CreateTransformationRequest request = new CreateTransformationRequest(
                artisanId, donationId, customizationRequest
        );

        TransformationResponse response = transformationService.createRequest(currentUser, request, beforeFiles);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/quote")
    public ResponseEntity<TransformationResponse> submitQuote(
            @PathVariable("id") UUID id,
            @RequestParam(value = "version", required = false) Long version,
            @Valid @RequestBody SubmitQuoteRequest request) {

        log.info("Received request to submit quote for transformation request ID: {}", id);
        User currentUser = getCurrentUser();
        TransformationResponse response = transformationService.submitQuote(id, currentUser, request, version);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<TransformationResponse> approveQuote(
            @PathVariable("id") UUID id,
            @RequestParam(value = "version", required = false) Long version) {

        log.info("Received request to approve quote for transformation request ID: {}", id);
        User currentUser = getCurrentUser();
        TransformationResponse response = transformationService.approveQuote(id, currentUser, version);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/{id}/progress", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TransformationResponse> updateProgress(
            @PathVariable("id") UUID id,
            @RequestParam("progress") TransformationProgress progress,
            @RequestParam(value = "afterFiles", required = false) List<MultipartFile> afterFiles,
            @RequestParam(value = "version", required = false) Long version) {

        log.info("Received request to update progress to {} for transformation ID: {}", progress, id);
        User currentUser = getCurrentUser();
        ProgressUpdateRequest request = new ProgressUpdateRequest(progress);
        TransformationResponse response = transformationService.updateProgress(id, currentUser, request, afterFiles, version);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<TransformationResponse>> getMyTransformations() {
        log.info("Received request to fetch current user's transformations");
        User currentUser = getCurrentUser();
        List<TransformationResponse> responses = transformationService.getMyTransformations(currentUser);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransformationResponse> getTransformation(@PathVariable("id") UUID id) {
        log.info("Received request to fetch transformation ID: {}", id);
        User currentUser = getCurrentUser();
        TransformationResponse response = transformationService.getTransformation(id, currentUser);
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
