package com.trirang.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ArtisanRequirementRequest(
    @NotBlank(message = "Material is required")
    String material,

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    Integer quantity,

    @NotBlank(message = "Purpose is required")
    String purpose,

    @NotNull(message = "Urgency is required")
    Integer urgency,

    @NotNull(message = "Radius is required")
    Double radiusKm,

    @NotNull(message = "Latitude is required")
    BigDecimal latitude,

    @NotNull(message = "Longitude is required")
    BigDecimal longitude
) {}
