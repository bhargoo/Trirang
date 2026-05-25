package com.trirang.model.dto;

import com.trirang.model.enums.TransformationProgress;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TransformationResponse(
    UUID id,
    UUID donorId,
    String donorName,
    UUID artisanId,
    String artisanName,
    UUID donationId,
    String donationTitle,
    String customizationRequest,
    BigDecimal quotedPrice,
    TransformationProgress progress,
    List<String> beforeImages,
    List<String> afterImages,
    Long version,
    Instant createdAt,
    Instant updatedAt
) {}
