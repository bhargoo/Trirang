package com.trirang.model.dto.response;

import com.trirang.model.enums.shared.NotificationType;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record NotificationResponse(
        UUID id,
        String title,
        String message,
        NotificationType type,
        Boolean isRead,
        Instant createdAt
) {
}
