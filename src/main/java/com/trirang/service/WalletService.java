package com.trirang.service;

import com.trirang.model.dto.*;
import com.trirang.model.entity.TriCoinLedger;
import com.trirang.model.entity.User;
import com.trirang.model.enums.TriCoinTransactionType;
import com.trirang.model.mapper.WalletMapper;
import com.trirang.repository.WalletRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletMapper walletMapper;

    public WalletService(WalletRepository walletRepository, WalletMapper walletMapper) {
        this.walletRepository = walletRepository;
        this.walletMapper = walletMapper;
    }

    @Transactional(readOnly = true)
    public WalletBalanceResponse getBalance(User user) {
        log.info("Fetching TriCoins balance for user ID: {}", user.getId());
        
        int balance = walletRepository.findLatestTransactionByUserId(user.getId())
                .map(TriCoinLedger::getBalanceAfterTransaction)
                .orElse(0);

        return new WalletBalanceResponse(user.getId(), user.getFullName(), balance);
    }

    @Transactional(readOnly = true)
    public Page<LedgerTransactionResponse> getTransactions(User user, Pageable pageable) {
        log.info("Fetching paged transaction ledger history for user ID: {}", user.getId());
        Page<TriCoinLedger> transactions = walletRepository.findByUserId(user.getId(), pageable);
        return transactions.map(walletMapper::toResponse);
    }

    /**
     * Atomically credits or debits coins from a user's wallet.
     */
    public int creditCoins(User user, Integer amount, TriCoinTransactionType type, String reason, UUID referenceId) {
        log.info("Adjusting TriCoins wallet for user ID: {} by amount: {} (Type: {})", user.getId(), amount, type);

        // Fetch current balance from the latest transaction
        int currentBalance = walletRepository.findLatestTransactionByUserId(user.getId())
                .map(TriCoinLedger::getBalanceAfterTransaction)
                .orElse(0);

        int balanceAfter = currentBalance + amount;

        // Never allow negative final balance
        if (balanceAfter < 0) {
            throw new IllegalArgumentException("Insufficient TriCoins balance. Current balance is: " + currentBalance);
        }

        TriCoinLedger entry = TriCoinLedger.builder()
                .user(user)
                .amount(amount)
                .balanceAfterTransaction(balanceAfter)
                .transactionType(type)
                .reason(reason)
                .referenceId(referenceId)
                .createdAt(Instant.now())
                .build();

        walletRepository.save(entry);
        log.info("Successfully persisted tricoin ledger entry. New balance: {}", balanceAfter);

        return balanceAfter;
    }
}
