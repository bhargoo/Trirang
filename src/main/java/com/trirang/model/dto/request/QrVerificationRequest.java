package com.trirang.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record QrVerificationRequest(
        @NotBlank(message = "QR Token cannot be blank")
        String token
) {
}
