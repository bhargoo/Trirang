package com.trirang.service;

import com.trirang.model.dto.DonationRequest;
import com.trirang.model.dto.DonationResponse;
import com.trirang.model.entity.Donation;
import com.trirang.model.entity.User;
import com.trirang.model.enums.DonationStatus;
import com.trirang.model.mapper.DonationMapper;
import com.trirang.repository.DonationRepository;
import com.trirang.storage.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class DonationService {

    private final DonationRepository donationRepository;
    private final FileStorageService fileStorageService;
    private final DonationMapper donationMapper;

    public DonationService(
            DonationRepository donationRepository,
            FileStorageService fileStorageService,
            DonationMapper donationMapper) {
        this.donationRepository = donationRepository;
        this.fileStorageService = fileStorageService;
        this.donationMapper = donationMapper;
    }

    public DonationResponse createDonation(User donor, DonationRequest request, MultipartFile imageFile) {
        log.info("Creating donation for donor ID: {}", donor.getId());

        // Validate image file
        validateImageFile(imageFile);

        // Store image
        String imagePath = fileStorageService.store(imageFile);

        Donation donation = Donation.builder()
                .donor(donor)
                .title(request.title())
                .description(request.description())
                .condition(request.condition())
                .category(request.category())
                .fabricType(request.fabricType())
                .classification(request.classification())
                .status(DonationStatus.PENDING_AI)
                .imagePath(imagePath)
                .latitude(request.latitude())
                .longitude(request.longitude())
                .aiAnalysisJson(new HashMap<>()) // Default empty JSONB map
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Donation saved = donationRepository.save(donation);
        log.info("Donation created successfully with ID: {} and status: {}", saved.getId(), saved.getStatus());

        return donationMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public DonationResponse getDonation(UUID id) {
        Donation donation = donationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Donation not found with ID: " + id));
        return donationMapper.toResponse(donation);
    }

    @Transactional(readOnly = true)
    public List<DonationResponse> getMyDonations(User donor) {
        return donationRepository.findByDonorId(donor.getId()).stream()
                .map(donationMapper::toResponse)
                .collect(Collectors.toList());
    }

    public void deleteDonation(UUID id, User currentUser) {
        Donation donation = donationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Donation not found with ID: " + id));

        // Ownership check
        if (!donation.getDonor().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("You are not authorized to delete this donation");
        }

        // Delete image file first
        if (donation.getImagePath() != null) {
            try {
                fileStorageService.delete(donation.getImagePath());
            } catch (Exception e) {
                log.error("Failed to delete stored file: {}", donation.getImagePath(), e);
            }
        }

        // Delete database record
        donationRepository.delete(donation);
        log.info("Donation ID: {} deleted by owner user ID: {}", id, currentUser.getId());
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Donation image is required");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Uploaded file must be a valid image");
        }

        if (file.getSize() > 10 * 1024 * 1024) { // 10MB limit
            throw new IllegalArgumentException("Image size exceeds the maximum limit of 10MB");
        }
    }
}
