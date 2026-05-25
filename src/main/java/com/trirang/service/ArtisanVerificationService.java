package com.trirang.service;

import com.trirang.model.dto.ArtisanVerificationResponse;
import com.trirang.model.entity.ArtisanVerification;
import com.trirang.model.entity.User;
import com.trirang.model.enums.Role;
import com.trirang.model.enums.VerificationBadge;
import com.trirang.model.enums.VerificationStatus;
import com.trirang.repository.ArtisanVerificationRepository;
import com.trirang.repository.UserRepository;
import com.trirang.storage.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class ArtisanVerificationService {

    private final ArtisanVerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public ArtisanVerificationService(
            ArtisanVerificationRepository verificationRepository,
            UserRepository userRepository,
            FileStorageService fileStorageService) {
        this.verificationRepository = verificationRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    public ArtisanVerificationResponse submitVerification(
            User artisan,
            MultipartFile governmentIdFile,
            MultipartFile selfieFile,
            List<MultipartFile> workspaceImageFiles) {

        // Validate role is ARTISAN
        if (!Role.ARTISAN.name().equals(artisan.getRole())) {
            throw new IllegalArgumentException("Only users with ARTISAN role can submit verification");
        }

        // Store files locally
        String governmentIdUrl = fileStorageService.store(governmentIdFile);
        String selfieUrl = fileStorageService.store(selfieFile);
        List<String> workspaceImageUrls = workspaceImageFiles.stream()
                .filter(file -> file != null && !file.isEmpty())
                .map(fileStorageService::store)
                .collect(Collectors.toList());

        ArtisanVerification verification = ArtisanVerification.builder()
                .artisan(artisan)
                .governmentIdImageUrl(governmentIdUrl)
                .selfieImageUrl(selfieUrl)
                .workspaceImageUrls(workspaceImageUrls)
                .status(VerificationStatus.PENDING)
                .submittedAt(Instant.now())
                .build();

        ArtisanVerification saved = verificationRepository.save(verification);
        log.info("Artisan verification submitted successfully for artisan ID: {}", artisan.getId());

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ArtisanVerificationResponse> getAllVerifications(VerificationStatus status) {
        List<ArtisanVerification> verifications;
        if (status != null) {
            verifications = verificationRepository.findByStatus(status);
        } else {
            verifications = verificationRepository.findAll();
        }
        return verifications.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ArtisanVerificationResponse approveVerification(UUID verificationId) {
        ArtisanVerification verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new IllegalArgumentException("Verification request not found with ID: " + verificationId));

        if (verification.getStatus() != VerificationStatus.PENDING) {
            throw new IllegalStateException("Verification request is not in PENDING status");
        }

        verification.setStatus(VerificationStatus.APPROVED);
        verification.setReviewedAt(Instant.now());
        
        // Update user's verification badge to VERIFIED
        User artisan = verification.getArtisan();
        artisan.setVerificationBadge(VerificationBadge.VERIFIED);
        userRepository.save(artisan);

        ArtisanVerification saved = verificationRepository.save(verification);
        log.info("Artisan verification ID: {} approved. User ID: {} badge updated to VERIFIED", verificationId, artisan.getId());

        return mapToResponse(saved);
    }

    public ArtisanVerificationResponse rejectVerification(UUID verificationId, String rejectionReason) {
        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason cannot be blank");
        }

        ArtisanVerification verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new IllegalArgumentException("Verification request not found with ID: " + verificationId));

        if (verification.getStatus() != VerificationStatus.PENDING) {
            throw new IllegalStateException("Verification request is not in PENDING status");
        }

        verification.setStatus(VerificationStatus.REJECTED);
        verification.setRejectionReason(rejectionReason);
        verification.setReviewedAt(Instant.now());

        ArtisanVerification saved = verificationRepository.save(verification);
        log.info("Artisan verification ID: {} rejected. Reason: {}", verificationId, rejectionReason);

        return mapToResponse(saved);
    }

    private ArtisanVerificationResponse mapToResponse(ArtisanVerification verification) {
        return new ArtisanVerificationResponse(
                verification.getId(),
                verification.getArtisan().getId(),
                verification.getGovernmentIdImageUrl(),
                verification.getSelfieImageUrl(),
                verification.getWorkspaceImageUrls(),
                verification.getStatus(),
                verification.getRejectionReason(),
                verification.getSubmittedAt(),
                verification.getReviewedAt()
        );
    }
}
