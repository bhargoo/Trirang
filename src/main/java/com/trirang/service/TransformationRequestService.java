package com.trirang.service;

import com.trirang.model.dto.*;
import com.trirang.model.entity.Donation;
import com.trirang.model.entity.TransformationRequest;
import com.trirang.model.entity.User;
import com.trirang.model.enums.Role;
import com.trirang.model.enums.TransformationProgress;
import com.trirang.model.mapper.TransformationRequestMapper;
import com.trirang.repository.DonationRepository;
import com.trirang.repository.TransformationRequestRepository;
import com.trirang.repository.UserRepository;
import com.trirang.storage.LocalFileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class TransformationRequestService {

    private final TransformationRequestRepository transformationRepository;
    private final UserRepository userRepository;
    private final DonationRepository donationRepository;
    private final LocalFileStorageService fileStorageService;
    private final NotificationService notificationService;
    private final TrustScoreEngine trustScoreEngine;
    private final TransformationRequestMapper transformationMapper;

    public TransformationRequestService(
            TransformationRequestRepository transformationRepository,
            UserRepository userRepository,
            DonationRepository donationRepository,
            LocalFileStorageService fileStorageService,
            NotificationService notificationService,
            TrustScoreEngine trustScoreEngine,
            TransformationRequestMapper transformationMapper) {
        this.transformationRepository = transformationRepository;
        this.userRepository = userRepository;
        this.donationRepository = donationRepository;
        this.fileStorageService = fileStorageService;
        this.notificationService = notificationService;
        this.trustScoreEngine = trustScoreEngine;
        this.transformationMapper = transformationMapper;
    }

    public TransformationResponse createRequest(
            User donor, CreateTransformationRequest request, List<MultipartFile> beforeFiles) {
        log.info("Donor {} creating custom transformation request on donation: {}", donor.getId(), request.donationId());

        // Validate caller role
        if (!Role.DONOR.name().equals(donor.getRole())) {
            throw new IllegalArgumentException("Only users with role DONOR can create transformation requests");
        }

        // Fetch artisan
        User artisan = userRepository.findById(request.artisanId())
                .orElseThrow(() -> new IllegalArgumentException("Artisan not found with ID: " + request.artisanId()));

        if (!Role.ARTISAN.name().equals(artisan.getRole())) {
            throw new IllegalArgumentException("Assigned user must have role ARTISAN");
        }

        // Fetch donation and ownership checks
        Donation donation = donationRepository.findById(request.donationId())
                .orElseThrow(() -> new IllegalArgumentException("Donation not found with ID: " + request.donationId()));

        if (!donation.getDonor().getId().equals(donor.getId())) {
            throw new IllegalStateException("You do not own this donation to request custom transformation");
        }

        // Image validations & uploads
        validateImages(beforeFiles);
        List<String> beforeImages = new ArrayList<>();
        if (beforeFiles != null) {
            for (MultipartFile file : beforeFiles) {
                if (file != null && !file.isEmpty()) {
                    beforeImages.add(fileStorageService.store(file));
                }
            }
        }

        TransformationRequest tr = TransformationRequest.builder()
                .donor(donor)
                .artisan(artisan)
                .donation(donation)
                .customizationRequest(request.customizationRequest())
                .progress(TransformationProgress.REQUESTED)
                .beforeImages(beforeImages)
                .afterImages(new ArrayList<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        TransformationRequest saved = transformationRepository.save(tr);
        notificationService.sendProgressNotification(saved.getId(), "Custom transformation request submitted!");

        return transformationMapper.toResponse(saved);
    }

    public TransformationResponse submitQuote(
            UUID id, User artisan, SubmitQuoteRequest request, Long expectedVersion) {
        log.info("Artisan {} submitting quotedPrice for request ID: {}", artisan.getId(), id);

        TransformationRequest tr = transformationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transformation request not found with ID: " + id));

        // Assigned artisan check
        if (!tr.getArtisan().getId().equals(artisan.getId())) {
            throw new IllegalStateException("Only the assigned artisan can submit a quote for this request");
        }

        if (tr.getProgress() != TransformationProgress.REQUESTED) {
            throw new IllegalStateException("Quotes can only be submitted for PENDING/REQUESTED transformations");
        }

        // Optimistic locking check
        if (expectedVersion != null && !expectedVersion.equals(tr.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(TransformationRequest.class, id);
        }

        tr.setQuotedPrice(request.quotedPrice());
        validateTransition(tr.getProgress(), TransformationProgress.QUOTED);
        tr.setProgress(TransformationProgress.QUOTED);
        tr.setUpdatedAt(Instant.now());

        TransformationRequest saved = transformationRepository.save(tr);
        notificationService.sendProgressNotification(saved.getId(), "Artisan submitted quote: INR " + request.quotedPrice());

        return transformationMapper.toResponse(saved);
    }

    public TransformationResponse approveQuote(UUID id, User donor, Long expectedVersion) {
        log.info("Donor {} approving quote for request ID: {}", donor.getId(), id);

        TransformationRequest tr = transformationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transformation request not found with ID: " + id));

        // Ownership check
        if (!tr.getDonor().getId().equals(donor.getId())) {
            throw new IllegalStateException("Only the requesting donor can approve this quote");
        }

        if (tr.getProgress() != TransformationProgress.QUOTED) {
            throw new IllegalStateException("Only QUOTED transformation requests can be approved");
        }

        // Optimistic locking check
        if (expectedVersion != null && !expectedVersion.equals(tr.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(TransformationRequest.class, id);
        }

        validateTransition(tr.getProgress(), TransformationProgress.APPROVED);
        tr.setProgress(TransformationProgress.APPROVED);
        tr.setUpdatedAt(Instant.now());

        TransformationRequest saved = transformationRepository.save(tr);
        notificationService.sendProgressNotification(saved.getId(), "Quote approved by donor! Production will start soon.");

        return transformationMapper.toResponse(saved);
    }

    public TransformationResponse updateProgress(
            UUID id, User artisan, ProgressUpdateRequest request, List<MultipartFile> afterFiles, Long expectedVersion) {
        log.info("Artisan {} updating progress to {} for request ID: {}", artisan.getId(), request.progress(), id);

        TransformationRequest tr = transformationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transformation request not found with ID: " + id));

        // Ownership check
        if (!tr.getArtisan().getId().equals(artisan.getId())) {
            throw new IllegalStateException("Only the assigned artisan can update progress");
        }

        // Optimistic locking check
        if (expectedVersion != null && !expectedVersion.equals(tr.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(TransformationRequest.class, id);
        }

        // Validate workflow transition
        validateTransition(tr.getProgress(), request.progress());

        // Process after-images if completed or delivered
        if ((request.progress() == TransformationProgress.COMPLETED || request.progress() == TransformationProgress.DELIVERED) 
                && afterFiles != null && !afterFiles.isEmpty()) {
            validateImages(afterFiles);
            List<String> afterImages = new ArrayList<>();
            for (MultipartFile file : afterFiles) {
                if (file != null && !file.isEmpty()) {
                    afterImages.add(fileStorageService.store(file));
                }
            }
            tr.setAfterImages(afterImages);
        }

        // Transition progress
        tr.setProgress(request.progress());
        tr.setUpdatedAt(Instant.now());

        // Reward artisan trust score on DELIVERED
        if (request.progress() == TransformationProgress.DELIVERED) {
            trustScoreEngine.rewardArtisan(artisan.getId(), 10);
        }

        TransformationRequest saved = transformationRepository.save(tr);
        notificationService.sendProgressNotification(saved.getId(), "Progress updated to: " + request.progress());

        return transformationMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TransformationResponse> getMyTransformations(User user) {
        log.info("Fetching all transformation workflows involving user: {}", user.getId());
        return transformationRepository.findByUserId(user.getId()).stream()
                .map(transformationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TransformationResponse getTransformation(UUID id, User user) {
        log.info("Fetching custom transformation details for ID: {}", id);

        TransformationRequest tr = transformationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transformation request not found with ID: " + id));

        if (!tr.getDonor().getId().equals(user.getId()) && !tr.getArtisan().getId().equals(user.getId())) {
            throw new IllegalStateException("You are not authorized to view this transformation request");
        }

        return transformationMapper.toResponse(tr);
    }

    private void validateImages(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return;
        }
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            if (file.getSize() > 10 * 1024 * 1024) {
                throw new IllegalArgumentException("Image size must be less than 10MB");
            }
            String mime = file.getContentType();
            if (mime == null || (!mime.equals("image/jpeg") && !mime.equals("image/png") && !mime.equals("image/webp"))) {
                throw new IllegalArgumentException("Invalid image type: " + mime + ". Only JPEG, PNG, and WEBP are allowed.");
            }
        }
    }

    private void validateTransition(TransformationProgress current, TransformationProgress target) {
        boolean valid = switch (current) {
            case REQUESTED -> target == TransformationProgress.QUOTED || target == TransformationProgress.CANCELLED;
            case QUOTED -> target == TransformationProgress.APPROVED || target == TransformationProgress.CANCELLED;
            case APPROVED -> target == TransformationProgress.RECEIVED || target == TransformationProgress.CANCELLED;
            case RECEIVED -> target == TransformationProgress.CUTTING || target == TransformationProgress.CANCELLED;
            case CUTTING -> target == TransformationProgress.STITCHING || target == TransformationProgress.CANCELLED;
            case STITCHING -> target == TransformationProgress.COMPLETED || target == TransformationProgress.CANCELLED;
            case COMPLETED -> target == TransformationProgress.DELIVERED || target == TransformationProgress.CANCELLED;
            case DELIVERED -> false;
            case CANCELLED -> false;
        };
        if (!valid) {
            throw new IllegalStateException("Invalid custom transformation progress transition from " + current + " to " + target);
        }
    }
}
