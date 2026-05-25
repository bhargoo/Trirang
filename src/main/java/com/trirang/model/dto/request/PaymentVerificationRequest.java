package com.trirang.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record PaymentVerificationRequest(
        @NotBlank(message = "Razorpay Order ID cannot be blank")
        String razorpayOrderId,

        @NotBlank(message = "Razorpay Payment ID cannot be blank")
        String razorpayPaymentId,

        @NotBlank(message = "Razorpay Signature cannot be blank")
        String razorpaySignature
) {
}
