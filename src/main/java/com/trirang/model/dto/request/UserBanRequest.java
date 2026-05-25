package com.trirang.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record UserBanRequest(
        @NotBlank(message = "Reason for ban is required")
        String reason
) {
}
