package com.trirang.model.dto;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponse(
    UUID id,
    UUID matchId,
    UUID senderId,
    String senderName,
    String message,
    Instant createdAt
) {}
