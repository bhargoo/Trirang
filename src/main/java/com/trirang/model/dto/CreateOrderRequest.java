package com.trirang.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateOrderRequest(
    @NotNull(message = "Listing ID is required")
    UUID listingId,

    @NotBlank(message = "Delivery address is required")
    String deliveryAddress
) {}
