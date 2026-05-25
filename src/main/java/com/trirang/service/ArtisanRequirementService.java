package com.trirang.service;

import com.trirang.model.dto.ArtisanRequirementRequest;
import com.trirang.model.dto.ArtisanRequirementResponse;
import com.trirang.model.entity.ArtisanRequirement;
import com.trirang.model.entity.User;
import com.trirang.model.enums.Role;
import com.trirang.model.mapper.ArtisanRequirementMapper;
import com.trirang.repository.ArtisanRequirementRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class ArtisanRequirementService {

    private final ArtisanRequirementRepository requirementRepository;
    private final ArtisanRequirementMapper requirementMapper;

    public ArtisanRequirementService(
            ArtisanRequirementRepository requirementRepository,
            ArtisanRequirementMapper requirementMapper) {
        this.requirementRepository = requirementRepository;
        this.requirementMapper = requirementMapper;
    }

    public ArtisanRequirementResponse createRequirement(User artisan, ArtisanRequirementRequest request) {
        log.info("Creating artisan requirement for artisan ID: {}", artisan.getId());

        // Validate that user role is ARTISAN
        if (!Role.ARTISAN.name().equals(artisan.getRole())) {
            throw new IllegalArgumentException("Only users with ARTISAN role can submit requirements");
        }

        ArtisanRequirement requirement = ArtisanRequirement.builder()
                .artisan(artisan)
                .material(request.material())
                .quantity(request.quantity())
                .purpose(request.purpose())
                .urgency(request.urgency())
                .radiusKm(request.radiusKm())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .status("OPEN")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        ArtisanRequirement saved = requirementRepository.save(requirement);
        log.info("Artisan requirement created with ID: {}", saved.getId());

        return requirementMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ArtisanRequirementResponse> getMyRequirements(User artisan) {
        return requirementRepository.findByArtisanId(artisan.getId()).stream()
                .map(requirementMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ArtisanRequirementResponse> getNearbyRequirements(
            double searchLat, double searchLng, Double searchRadius) {
        log.info("Searching for nearby requirements around ({}, {}) with search radius: {}", searchLat, searchLng, searchRadius);

        List<ArtisanRequirement> allRequirements = requirementRepository.findAll();

        return allRequirements.stream()
                .filter(req -> "OPEN".equalsIgnoreCase(req.getStatus()))
                .filter(req -> {
                    double reqLat = req.getLatitude().doubleValue();
                    double reqLng = req.getLongitude().doubleValue();
                    double distance = calculateHaversineDistance(searchLat, searchLng, reqLat, reqLng);

                    // If searchRadius is specified, check against it.
                    // Otherwise, check if search point falls within the artisan's active radius.
                    if (searchRadius != null) {
                        return distance <= searchRadius;
                    } else {
                        return distance <= req.getRadiusKm();
                    }
                })
                .sorted((r1, r2) -> {
                    double dist1 = calculateHaversineDistance(searchLat, searchLng, r1.getLatitude().doubleValue(), r1.getLongitude().doubleValue());
                    double dist2 = calculateHaversineDistance(searchLat, searchLng, r2.getLatitude().doubleValue(), r2.getLongitude().doubleValue());
                    return Double.compare(dist1, dist2);
                })
                .map(requirementMapper::toResponse)
                .collect(Collectors.toList());
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
