package com.trirang.service;

import com.trirang.model.dto.*;
import com.trirang.model.entity.Match;
import com.trirang.model.entity.Review;
import com.trirang.model.entity.User;
import com.trirang.model.enums.MatchStatus;
import com.trirang.model.mapper.ReviewMapper;
import com.trirang.repository.MatchRepository;
import com.trirang.repository.ReviewRepository;
import com.trirang.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final TrustScoreEngine trustScoreEngine;
    private final ReviewMapper reviewMapper;

    public ReviewService(
            ReviewRepository reviewRepository,
            MatchRepository matchRepository,
            UserRepository userRepository,
            TrustScoreEngine trustScoreEngine,
            ReviewMapper reviewMapper) {
        this.reviewRepository = reviewRepository;
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
        this.trustScoreEngine = trustScoreEngine;
        this.reviewMapper = reviewMapper;
    }

    public ReviewResponse createReview(User reviewer, CreateReviewRequest request) {
        log.info("User {} submitting review for user {}", reviewer.getId(), request.reviewedUserId());

        // Prevent self reviews
        if (reviewer.getId().equals(request.reviewedUserId())) {
            throw new IllegalArgumentException("You cannot review yourself");
        }

        // Fetch reviewed user
        User reviewedUser = userRepository.findById(request.reviewedUserId())
                .orElseThrow(() -> new IllegalArgumentException("Reviewed user not found with ID: " + request.reviewedUserId()));

        // Fetch match
        Match match = matchRepository.findById(request.relatedMatchId())
                .orElseThrow(() -> new IllegalArgumentException("Match not found with ID: " + request.relatedMatchId()));

        // Prevent reviews before completion
        if (match.getStatus() != MatchStatus.COMPLETED) {
            throw new IllegalStateException("You can only review users after the match is marked as COMPLETED");
        }

        // Only users involved in completed matches may review each other
        boolean isDonorReviewingArtisan = match.getDonor().getId().equals(reviewer.getId()) && match.getArtisan().getId().equals(reviewedUser.getId());
        boolean isArtisanReviewingDonor = match.getArtisan().getId().equals(reviewer.getId()) && match.getDonor().getId().equals(reviewedUser.getId());

        if (!isDonorReviewingArtisan && !isArtisanReviewingDonor) {
            throw new IllegalArgumentException("You can only review users with whom you have a completed match");
        }

        // Prevent duplicate reviews for same match
        boolean duplicateExists = reviewRepository.existsByReviewerIdAndReviewedUserIdAndRelatedMatchId(
                reviewer.getId(), reviewedUser.getId(), match.getId()
        );
        if (duplicateExists) {
            throw new IllegalStateException("You have already reviewed this user for this match");
        }

        // Sanitize comment
        String sanitizedComment = request.comment() != null ? HtmlUtils.htmlEscape(request.comment().trim()) : null;
        if (sanitizedComment != null && sanitizedComment.length() > 1000) {
            throw new IllegalArgumentException("Review comment must not exceed 1000 characters");
        }

        Review review = Review.builder()
                .reviewer(reviewer)
                .reviewedUser(reviewedUser)
                .relatedMatch(match)
                .rating(request.rating())
                .comment(sanitizedComment)
                .createdAt(Instant.now())
                .build();

        Review saved = reviewRepository.save(review);
        log.info("Successfully created review: {}", saved.getId());

        // Process trust score updates atomically
        trustScoreEngine.processReviewRating(reviewedUser, request.rating());

        return reviewMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public TrustScoreResponse getUserTrustScoreDetails(UUID userId) {
        log.info("Fetching trust score details for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        List<Review> userReviews = reviewRepository.findByReviewedUserId(userId);

        double avgRating = 0.0;
        if (!userReviews.isEmpty()) {
            avgRating = userReviews.stream()
                    .mapToInt(Review::getRating)
                    .average()
                    .orElse(0.0);
        }

        int score = user.getTrustScore() != null ? user.getTrustScore() : 50;

        return new TrustScoreResponse(
                user.getId(),
                user.getFullName(),
                score,
                avgRating,
                userReviews.size()
        );
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getUserReviews(UUID userId, Pageable pageable) {
        log.info("Fetching reviews list for user ID: {}", userId);
        
        // Ensure user exists
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }

        Page<Review> reviews = reviewRepository.findByReviewedUserId(userId, pageable);
        return reviews.map(reviewMapper::toResponse);
    }
}
