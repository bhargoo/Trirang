package com.trirang.model.dto;

import com.trirang.model.enums.shared.Classification;
import com.trirang.model.enums.shared.FabricType;
import com.trirang.model.enums.shared.ItemCategory;
import java.math.BigDecimal;

public record AiClassificationResponse(
    ItemCategory category,
    FabricType fabricType,
    Classification classification,
    String condition,
    BigDecimal confidence,
    String description,
    String suggestedAction
) {}
