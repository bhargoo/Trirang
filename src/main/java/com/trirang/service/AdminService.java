package com.trirang.service;

import com.trirang.model.dto.request.UserBanRequest;
import com.trirang.model.dto.response.AdminAnalyticsResponse;
import com.trirang.model.entity.Listing;
import com.trirang.model.entity.User;
import com.trirang.model.entity.Verification;
import com.trirang.model.enums.shared.ReviewStatus;
import com.trirang.repository.ListingRepository;
import com.trirang.repository.PaymentTransactionRepository;
import com.trirang.repository.UserRepository;
import com.trirang.repository.VerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final VerificationRepository verificationRepository;
    private final ListingRepository listingRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;

    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional
    public void banUser(UUID userId, UserBanRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setIsBanned(true);
        userRepository.save(user);
        log.info("Admin banned user {} with reason: {}", userId, request.reason());
    }

    @Transactional(readOnly = true)
    public Page<Verification> getPendingVerifications(Pageable pageable) {
        return verificationRepository.findByStatus(ReviewStatus.PENDING, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Listing> getPendingListings(Pageable pageable) {
        return listingRepository.findByStatus(com.trirang.model.enums.ListingStatus.ACTIVE, pageable);
    }

    @Transactional(readOnly = true)
    public AdminAnalyticsResponse getAnalytics() {
        return AdminAnalyticsResponse.builder()
                .totalUsers(userRepository.count())
                .pendingVerifications(verificationRepository.count())
                .pendingListings(listingRepository.count())
                .totalPayments(paymentTransactionRepository.count())
                .build();
    }
}
