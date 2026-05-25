package com.trirang.model.dto;

import com.trirang.model.enums.MatchStatus;

import java.time.Instant;
import java.util.UUID;

public record MatchResponse(
    UUID id,
    UUID donationId,
    String donationTitle,
    UUID artisanRequirementId,
    String artisanRequirementMaterial,
    UUID donorId,
    String donorName,
    UUID artisanId,
    String artisanName,
    MatchStatus status,
    Double matchScore,
    Long version,
    Instant createdAt,
    Instant updatedAt
) {}
