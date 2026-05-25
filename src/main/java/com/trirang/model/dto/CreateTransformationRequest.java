package com.trirang.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateTransformationRequest(
    @NotNull(message = "Artisan ID is required")
    UUID artisanId,

    @NotNull(message = "Donation ID is required")
    UUID donationId,

    @NotBlank(message = "Customization request details are required")
    String customizationRequest
) {}
