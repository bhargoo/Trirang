package com.trirang.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record SubmitQuoteRequest(
    @NotNull(message = "Quoted price is required")
    @DecimalMin(value = "0.01", message = "Quoted price must be greater than 0")
    BigDecimal quotedPrice
) {}
