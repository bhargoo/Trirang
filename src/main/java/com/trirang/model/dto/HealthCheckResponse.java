package com.trirang.model.dto;

import java.time.Instant;

public record HealthCheckResponse(
    String status,
    String service,
    Instant timestamp
) {}
