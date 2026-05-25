package com.trirang.model.dto;

import com.trirang.model.enums.TriCoinTransactionType;
import java.time.Instant;
import java.util.UUID;

public record LedgerTransactionResponse(
    UUID id,
    UUID userId,
    Integer amount,
    Integer balanceAfterTransaction,
    TriCoinTransactionType transactionType,
    String reason,
    UUID referenceId,
    Instant createdAt
) {}
