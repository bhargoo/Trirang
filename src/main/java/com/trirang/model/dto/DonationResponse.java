package com.trirang.model.dto;

import com.trirang.model.enums.DonationStatus;
import com.trirang.model.enums.shared.Classification;
import com.trirang.model.enums.shared.FabricType;
import com.trirang.model.enums.shared.ItemCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record DonationResponse(
    UUID id,
    UUID donorId,
    String donorName,
    String title,
    String description,
    String condition,
    ItemCategory category,
    FabricType fabricType,
    Classification classification,
    String imageUrl,
    String qrCodePath,
    BigDecimal latitude,
    BigDecimal longitude,
    DonationStatus status,
    Map<String, Object> aiAnalysisJson,
    Instant createdAt,
    Instant updatedAt
) {}
