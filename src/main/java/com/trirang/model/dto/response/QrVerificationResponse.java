package com.trirang.model.dto.response;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record QrVerificationResponse(
        boolean isValid,
        UUID matchId,
        String message,
        Instant verifiedAt
) {
}
