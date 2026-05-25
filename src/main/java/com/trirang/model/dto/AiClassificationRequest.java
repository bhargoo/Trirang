package com.trirang.model.dto;

import jakarta.validation.constraints.NotBlank;

public record AiClassificationRequest(
    @NotBlank(message = "Base64 image data is required")
    String base64Image
) {}
