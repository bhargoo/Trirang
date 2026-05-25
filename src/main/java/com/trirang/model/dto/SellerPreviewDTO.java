package com.trirang.model.dto;

public record SellerPreviewDTO(
    String name,
    Double trustScore,
    Boolean verificationBadge
) {}
