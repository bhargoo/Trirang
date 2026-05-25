package com.trirang.model.dto;

import com.trirang.model.enums.ListingStatus;
import com.trirang.model.enums.ListingType;
import com.trirang.model.enums.shared.ItemCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ListingResponse(
    UUID id,
    UUID sellerId,
    SellerPreviewDTO seller,
    ListingType type,
    String title,
    String description,
    BigDecimal price,
    ItemCategory category,
    List<String> imageUrls,
    BigDecimal latitude,
    BigDecimal longitude,
    ListingStatus status,
    UUID originalDonationId,
    Long version,
    Instant createdAt,
    Instant updatedAt
) {}
