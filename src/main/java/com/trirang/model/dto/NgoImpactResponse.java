package com.trirang.model.dto;

import java.util.List;
import java.util.UUID;

public record NgoImpactResponse(
    UUID ngoId,
    String ngoName,
    long totalClaimsCount,
    List<RecyclerClaimResponse> claims
) {}
