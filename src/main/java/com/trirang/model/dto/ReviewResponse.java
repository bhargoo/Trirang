package com.trirang.model.dto;

import java.time.Instant;
import java.util.UUID;

public record ReviewResponse(
    UUID id,
    UUID reviewerId,
    String reviewerName,
    UUID reviewedUserId,
    String reviewedUserName,
    UUID relatedMatchId,
    Integer rating,
    String comment,
    Instant createdAt
) {}
