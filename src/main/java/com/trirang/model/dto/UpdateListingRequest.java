package com.trirang.model.dto;

import com.trirang.model.enums.ListingStatus;
import com.trirang.model.enums.shared.ItemCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdateListingRequest(
    @NotBlank(message = "Title is required")
    String title,

    String description,

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    BigDecimal price,

    @NotNull(message = "Category is required")
    ItemCategory category,

    @NotNull(message = "Status is required")
    ListingStatus status,

    @NotNull(message = "Latitude is required")
    BigDecimal latitude,

    @NotNull(message = "Longitude is required")
    BigDecimal longitude
) {}
