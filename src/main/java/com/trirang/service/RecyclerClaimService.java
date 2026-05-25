package com.trirang.service;

import com.trirang.model.dto.DonationResponse;
import com.trirang.model.dto.NgoImpactResponse;
import com.trirang.model.dto.RecyclerClaimResponse;
import com.trirang.model.entity.Donation;
import com.trirang.model.entity.RecyclerClaim;
import com.trirang.model.entity.User;
import com.trirang.model.enums.DonationStatus;
import com.trirang.model.enums.Role;
import com.trirang.model.mapper.DonationMapper;
import com.trirang.model.mapper.RecyclerClaimMapper;
import com.trirang.repository.DonationRepository;
import com.trirang.repository.RecyclerClaimRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class RecyclerClaimService {

    private final RecyclerClaimRepository claimRepository;
    private final DonationRepository donationRepository;
    private final RecyclerClaimMapper claimMapper;
    private final DonationMapper donationMapper;
    private final WalletService walletService;

    public RecyclerClaimService(
            RecyclerClaimRepository claimRepository,
            DonationRepository donationRepository,
            RecyclerClaimMapper claimMapper,
            DonationMapper donationMapper,
            WalletService walletService) {
        this.claimRepository = claimRepository;
        this.donationRepository = donationRepository;
        this.claimMapper = claimMapper;
        this.donationMapper = donationMapper;
        this.walletService = walletService;
    }

    @Transactional(readOnly = true)
    public List<DonationResponse> getRecyclableDonations(
            double searchLat, double searchLng, Double searchRadius) {
        log.info("Searching for recyclable donations around ({}, {}) with radius: {}", searchLat, searchLng, searchRadius);

        List<Donation> allDonations = donationRepository.findAll();

        return allDonations.stream()
                .filter(donation -> DonationStatus.AVAILABLE == donation.getStatus())
                .filter(donation -> donation.getClassification() != null && 
                        "RECYCLE".equalsIgnoreCase(donation.getClassification().name()))
                .filter(donation -> {
                    if (searchRadius == null) {
                        return true;
                    }
                    if (donation.getLatitude() == null || donation.getLongitude() == null) {
                        return false;
                    }
                    double dist = calculateHaversineDistance(
                            searchLat, searchLng,
                            donation.getLatitude().doubleValue(),
                            donation.getLongitude().doubleValue()
                    );
                    return dist <= searchRadius;
                })
                .sorted((d1, d2) -> {
                    if (d1.getLatitude() == null || d2.getLatitude() == null) {
                        return 0;
                    }
                    double dist1 = calculateHaversineDistance(searchLat, searchLng, d1.getLatitude().doubleValue(), d1.getLongitude().doubleValue());
                    double dist2 = calculateHaversineDistance(searchLat, searchLng, d2.getLatitude().doubleValue(), d2.getLongitude().doubleValue());
                    return Double.compare(dist1, dist2);
                })
                .map(donationMapper::toResponse)
                .collect(Collectors.toList());
    }

    public RecyclerClaimResponse claimDonation(User user, UUID donationId) {
        log.info("User {} claiming donation ID: {}", user.getId(), donationId);

        // Role verification
        String userRole = user.getRole();
        if (!Role.RECYCLER.name().equals(userRole) && !Role.NGO.name().equals(userRole)) {
            throw new IllegalArgumentException("Only users with RECYCLER or NGO role can claim recyclable donations");
        }

        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new IllegalArgumentException("Donation not found with ID: " + donationId));

        // Enforce classification constraint
        if (donation.getClassification() == null || !"RECYCLE".equalsIgnoreCase(donation.getClassification().name())) {
            throw new IllegalArgumentException("Only donations classified under 'RECYCLE' can be claimed by a recycler or NGO");
        }

        // Enforce status constraint
        if (DonationStatus.AVAILABLE != donation.getStatus()) {
            throw new IllegalStateException("This donation is not currently AVAILABLE to be claimed (current status: " + donation.getStatus() + ")");
        }

        // Prevent duplicate claims
        if (claimRepository.existsByDonationId(donationId)) {
            throw new IllegalStateException("This donation has already been claimed");
        }

        // Create the claim
        RecyclerClaim claim = RecyclerClaim.builder()
                .recycler(user)
                .donation(donation)
                .claimedAt(Instant.now())
                .status("CLAIMED")
                .build();

        RecyclerClaim savedClaim = claimRepository.save(claim);

        // Update the donation status to RECYCLED
        donation.setStatus(DonationStatus.RECYCLED);
        donationRepository.save(donation);

        // Award TriCoins to donor for PICKUP_REWARD
        try {
            walletService.creditCoins(
                    donation.getDonor(),
                    30,
                    com.trirang.model.enums.TriCoinTransactionType.PICKUP_REWARD,
                    "Donation picked up and recycled",
                    savedClaim.getId()
            );
        } catch (Exception e) {
            log.error("Failed to credit TriCoins for pickup reward", e);
        }

        log.info("Donation ID: {} claimed successfully by user ID: {}. Stored claim ID: {}", donationId, user.getId(), savedClaim.getId());

        return claimMapper.toResponse(savedClaim);
    }

    @Transactional(readOnly = true)
    public NgoImpactResponse getNgoImpact(User ngoUser) {
        log.info("Fetching NGO impact summary for user ID: {}", ngoUser.getId());

        // Validate role is NGO
        if (!Role.NGO.name().equals(ngoUser.getRole())) {
            throw new IllegalArgumentException("Only users with NGO role can retrieve NGO impact reports");
        }

        List<RecyclerClaim> claims = claimRepository.findByRecyclerId(ngoUser.getId());
        List<RecyclerClaimResponse> responses = claims.stream()
                .map(claimMapper::toResponse)
                .collect(Collectors.toList());

        return new NgoImpactResponse(
                ngoUser.getId(),
                ngoUser.getFullName(),
                responses.size(),
                responses
        );
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371.0 * c; // Earth radius in km
    }
}
