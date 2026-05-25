package com.trirang.model.mapper;

import com.trirang.model.dto.LedgerTransactionResponse;
import com.trirang.model.entity.TriCoinLedger;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WalletMapper {

    @Mapping(target = "userId", source = "user.id")
    LedgerTransactionResponse toResponse(TriCoinLedger ledger);
}
