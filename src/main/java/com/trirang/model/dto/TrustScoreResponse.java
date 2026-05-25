package com.trirang.model.dto;

import java.util.UUID;

public record TrustScoreResponse(
    UUID userId,
    String userName,
    Integer trustScore,
    Double averageRating,
    Integer totalReviewsCount
) {}
