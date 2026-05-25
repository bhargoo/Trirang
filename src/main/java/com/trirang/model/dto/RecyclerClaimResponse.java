package com.trirang.model.dto;

import java.time.Instant;
import java.util.UUID;

public record RecyclerClaimResponse(
    UUID id,
    UUID recyclerId,
    String recyclerName,
    UUID donationId,
    String donationTitle,
    Instant claimedAt,
    String status
) {}
