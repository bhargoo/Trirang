package com.trirang.model.dto.response;

import lombok.Builder;

@Builder
public record AdminAnalyticsResponse(
        long totalUsers,
        long pendingVerifications,
        long pendingListings,
        long totalPayments
) {
}
