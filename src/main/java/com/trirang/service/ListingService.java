package com.trirang.service;

import com.trirang.model.dto.*;
import com.trirang.model.entity.Listing;
import com.trirang.model.entity.User;
import com.trirang.model.enums.ListingStatus;
import com.trirang.model.enums.ListingType;
import com.trirang.model.enums.Role;
import com.trirang.model.enums.shared.ItemCategory;
import com.trirang.model.mapper.ListingMapper;
import com.trirang.repository.ListingRepository;
import com.trirang.storage.MarketplaceImageStorageService;
import com.trirang.util.DistanceCalculationUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class ListingService {

    private final ListingRepository listingRepository;
    private final ListingMapper listingMapper;
    private final MarketplaceImageStorageService imageStorageService;

    public ListingService(
            ListingRepository listingRepository,
            ListingMapper listingMapper,
            MarketplaceImageStorageService imageStorageService) {
        this.listingRepository = listingRepository;
        this.listingMapper = listingMapper;
        this.imageStorageService = imageStorageService;
    }

    public ListingResponse createListing(User seller, CreateListingRequest request, List<MultipartFile> files) {
        log.info("Creating marketplace listing for seller: {}", seller.getId());

        // Validate seller status
        validateSeller(seller, request.type());

        // Price validation
        if (request.price() == null || request.price().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Listing price must be positive and greater than 0");
        }

        // Store images
        List<String> imageUrls = imageStorageService.storeImages(files);

        Listing listing = Listing.builder()
                .seller(seller)
                .type(request.type())
                .title(request.title())
                .description(request.description())
                .price(request.price())
                .category(request.category())
                .imageUrls(imageUrls)
                .latitude(request.latitude())
                .longitude(request.longitude())
                .status(ListingStatus.ACTIVE)
                .originalDonationId(request.originalDonationId())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Listing saved = listingRepository.save(listing);
        log.info("Marketplace listing created successfully with ID: {}", saved.getId());

        return listingMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ListingResponse getListing(UUID id) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found with ID: " + id));

        if (listing.getStatus() == ListingStatus.REMOVED) {
            throw new IllegalArgumentException("This listing is no longer available");
        }

        return listingMapper.toResponse(listing);
    }

    public ListingResponse updateListing(UUID id, User currentUser, UpdateListingRequest request, Long expectedVersion) {
        log.info("Updating marketplace listing ID: {} by user: {}", id, currentUser.getId());

        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found with ID: " + id));

        // Security check: Only seller or ADMIN may edit
        validateOwnershipOrAdmin(listing, currentUser);

        // Optimistic locking check
        if (expectedVersion != null && !expectedVersion.equals(listing.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(Listing.class, id);
        }

        // Validate price
        if (request.price() == null || request.price().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Listing price must be positive and greater than 0");
        }

        listing.setTitle(request.title());
        listing.setDescription(request.description());
        listing.setPrice(request.price());
        listing.setCategory(request.category());
        listing.setStatus(request.status());
        listing.setLatitude(request.latitude());
        listing.setLongitude(request.longitude());
        listing.setUpdatedAt(Instant.now());

        Listing saved = listingRepository.save(listing);
        return listingMapper.toResponse(saved);
    }

    public void softDeleteListing(UUID id, User currentUser) {
        log.info("Soft-deleting listing ID: {} by user: {}", id, currentUser.getId());

        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found with ID: " + id));

        // Security check
        validateOwnershipOrAdmin(listing, currentUser);

        listing.setStatus(ListingStatus.REMOVED);
        listing.setUpdatedAt(Instant.now());
        listingRepository.save(listing);

        log.info("Listing ID: {} soft-deleted successfully", id);
    }

    @Transactional(readOnly = true)
    public Page<ListingSummaryResponse> browseListings(
            Double searchLat, Double searchLng, Double radiusKm,
            ItemCategory category, ListingType type, Pageable pageable) {

        log.info("Browsing listings: lat={}, lng={}, radius={}, category={}, type={}", searchLat, searchLng, radiusKm, category, type);

        List<Listing> allListings = listingRepository.findAll();

        List<ListingSummaryResponse> filtered = allListings.stream()
                .filter(l -> l.getStatus() == ListingStatus.ACTIVE)
                .filter(l -> category == null || l.getCategory() == category)
                .filter(l -> type == null || l.getType() == type)
                .map(l -> {
                    Double distance = null;
                    if (searchLat != null && searchLng != null && l.getLatitude() != null && l.getLongitude() != null) {
                        distance = DistanceCalculationUtil.calculateDistance(
                                searchLat, searchLng,
                                l.getLatitude().doubleValue(),
                                l.getLongitude().doubleValue()
                        );
                    }

                    String primaryImage = (l.getImageUrls() != null && !l.getImageUrls().isEmpty()) 
                            ? l.getImageUrls().get(0) 
                            : null;

                    return new ListingSummaryResponse(
                            l.getId(),
                            l.getTitle(),
                            l.getPrice(),
                            l.getType(),
                            l.getCategory(),
                            primaryImage,
                            l.getLatitude(),
                            l.getLongitude(),
                            l.getStatus(),
                            distance
                    );
                })
                .filter(dto -> {
                    if (radiusKm == null || searchLat == null || searchLng == null) {
                        return true;
                    }
                    return dto.distanceKm() != null && dto.distanceKm() <= radiusKm;
                })
                .sorted((d1, d2) -> {
                    if (d1.distanceKm() == null && d2.distanceKm() == null) return 0;
                    if (d1.distanceKm() == null) return 1;
                    if (d2.distanceKm() == null) return -1;
                    return Double.compare(d1.distanceKm(), d2.distanceKm());
                })
                .collect(Collectors.toList());

        // Perform manual pagination on sorted lists
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());
        
        List<ListingSummaryResponse> pagedList = new ArrayList<>();
        if (start < filtered.size()) {
            pagedList = filtered.subList(start, end);
        }

        return new PageImpl<>(pagedList, pageable, filtered.size());
    }

    private void validateSeller(User seller, ListingType type) {
        if (seller.getTrustScore() == null || seller.getTrustScore() <= 0) {
            throw new IllegalArgumentException("User trust score must be positive to post listings");
        }

        if (seller.getTrustScore() <= 10) {
            throw new IllegalArgumentException("Your trust score is too low to post to the marketplace");
        }

        String roleStr = seller.getRole();

        if (type == ListingType.THRIFT) {
            if (!Role.DONOR.name().equals(roleStr) && !Role.THRIFT_USER.name().equals(roleStr)) {
                throw new IllegalArgumentException("Only users with role DONOR or THRIFT_USER can post THRIFT listings");
            }
        } else if (type == ListingType.ARTISAN_PRODUCT) {
            if (!Role.ARTISAN.name().equals(roleStr)) {
                throw new IllegalArgumentException("Only users with role ARTISAN can post ARTISAN_PRODUCT listings");
            }
        }
    }

    private void validateOwnershipOrAdmin(Listing listing, User currentUser) {
        if (Role.ADMIN.name().equals(currentUser.getRole())) {
            return;
        }
        if (!listing.getSeller().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("You are not authorized to modify this listing");
        }
    }
}
