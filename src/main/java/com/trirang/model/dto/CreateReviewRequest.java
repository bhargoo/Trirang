package com.trirang.model.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record CreateReviewRequest(
    @NotNull(message = "Reviewed User ID is required")
    UUID reviewedUserId,

    @NotNull(message = "Match ID is required")
    UUID relatedMatchId,

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must not exceed 5")
    Integer rating,

    @Size(max = 1000, message = "Comment must not exceed 1000 characters")
    String comment
) {}
