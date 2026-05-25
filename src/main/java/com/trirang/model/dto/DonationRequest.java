package com.trirang.model.dto;

import com.trirang.model.enums.shared.Classification;
import com.trirang.model.enums.shared.FabricType;
import com.trirang.model.enums.shared.ItemCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record DonationRequest(
    @NotBlank(message = "Title is required")
    String title,

    String description,

    @NotBlank(message = "Condition is required")
    String condition,

    @NotNull(message = "Category is required")
    ItemCategory category,

    @NotNull(message = "Fabric type is required")
    FabricType fabricType,

    @NotNull(message = "Classification is required")
    Classification classification,

    @NotNull(message = "Latitude is required")
    BigDecimal latitude,

    @NotNull(message = "Longitude is required")
    BigDecimal longitude
) {}
