package com.trirang.model.dto.response;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record OrderResponse(
        String razorpayOrderId,
        BigDecimal amount,
        String currency
) {
}
