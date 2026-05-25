package com.trirang.model.dto;

import com.trirang.model.enums.TransformationProgress;
import jakarta.validation.constraints.NotNull;

public record ProgressUpdateRequest(
    @NotNull(message = "Target progress status is required")
    TransformationProgress progress
) {}
