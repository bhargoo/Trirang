package com.trirang.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ArtisanRequirementResponse(
    UUID id,
    UUID artisanId,
    String artisanName,
    String material,
    Integer quantity,
    String purpose,
    Integer urgency,
    Double radiusKm,
    BigDecimal latitude,
    BigDecimal longitude,
    String status,
    Instant createdAt
) {}
