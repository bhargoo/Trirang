package com.trirang.model.dto;

import com.trirang.model.enums.VerificationStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ArtisanVerificationResponse(
    UUID id,
    UUID artisanId,
    String governmentIdImageUrl,
    String selfieImageUrl,
    List<String> workspaceImageUrls,
    VerificationStatus status,
    String rejectionReason,
    Instant submittedAt,
    Instant reviewedAt
) {}
