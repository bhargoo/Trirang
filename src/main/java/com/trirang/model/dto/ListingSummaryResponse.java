package com.trirang.model.dto;

import com.trirang.model.enums.ListingStatus;
import com.trirang.model.enums.ListingType;
import com.trirang.model.enums.shared.ItemCategory;

import java.math.BigDecimal;
import java.util.UUID;

public record ListingSummaryResponse(
    UUID id,
    String title,
    BigDecimal price,
    ListingType type,
    ItemCategory category,
    String primaryImageUrl,
    BigDecimal latitude,
    BigDecimal longitude,
    ListingStatus status,
    Double distanceKm
) {}
