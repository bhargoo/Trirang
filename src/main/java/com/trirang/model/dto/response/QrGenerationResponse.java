package com.trirang.model.dto.response;

import lombok.Builder;

@Builder
public record QrGenerationResponse(
        String qrImageBase64,
        String token
) {
}
