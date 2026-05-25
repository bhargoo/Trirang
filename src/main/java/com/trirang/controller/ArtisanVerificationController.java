package com.trirang.controller;

import com.trirang.model.dto.ArtisanVerificationResponse;
import com.trirang.model.entity.User;
import com.trirang.repository.UserRepository;
import com.trirang.service.ArtisanVerificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/artisans/verify")
public class ArtisanVerificationController {

    private final ArtisanVerificationService verificationService;
    private final UserRepository userRepository;

    public ArtisanVerificationController(ArtisanVerificationService verificationService, UserRepository userRepository) {
        this.verificationService = verificationService;
        this.userRepository = userRepository;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ArtisanVerificationResponse> submitVerification(
            @RequestParam("governmentId") MultipartFile governmentId,
            @RequestParam("selfie") MultipartFile selfie,
            @RequestParam("workspaceImages") List<MultipartFile> workspaceImages) {

        log.info("Received artisan verification submission request");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated");
        }

        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found in database"));

        // Basic image file validation
        validateImageFile(governmentId, "Government ID");
        validateImageFile(selfie, "Selfie");
        for (MultipartFile file : workspaceImages) {
            validateImageFile(file, "Workspace image");
        }

        ArtisanVerificationResponse response = verificationService.submitVerification(
                currentUser,
                governmentId,
                selfie,
                workspaceImages
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private void validateImageFile(MultipartFile file, String fieldName) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " file is empty or missing");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException(fieldName + " must be an image file (e.g., JPEG, PNG)");
        }
        if (file.getSize() > 10 * 1024 * 1024) { // 10MB
            throw new IllegalArgumentException(fieldName + " file size exceeds the 10MB limit");
        }
    }
}
