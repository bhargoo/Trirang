package com.trirang.model.dto;

import com.trirang.model.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
    UUID id,
    UUID buyerId,
    String buyerName,
    UUID listingId,
    String listingTitle,
    BigDecimal amount,
    OrderStatus status,
    String razorpayOrderId,
    String deliveryAddress,
    Long version,
    Instant createdAt,
    Instant updatedAt
) {}
