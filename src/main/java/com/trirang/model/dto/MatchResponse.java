package com.trirang.model.dto;

import com.trirang.model.enums.MatchStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MatchResponse(
    UUID id,
    UUID donationId,
    String donationTitle,
    String donorName,
    UUID requirementId,
    String requirementMaterial,
    String artisanName,
    BigDecimal matchScore,
    MatchStatus status,
    Instant createdAt,
    Instant updatedAt
) {}
