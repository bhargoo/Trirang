package com.trirang.model.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectVerificationRequest(
    @NotBlank(message = "Rejection reason is required")
    String rejectionReason
) {}
