package com.trirang.model.dto;

import java.util.UUID;

public record WalletBalanceResponse(
    UUID userId,
    String userName,
    Integer currentBalance
) {}
