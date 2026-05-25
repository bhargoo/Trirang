package com.trirang.service;

import com.trirang.model.entity.User;
import com.trirang.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
public class TrustScoreEngine {

    private static final int BASE_TRUST_SCORE = 50;
    private static final int MIN_SCORE = 0;
    private static final int MAX_SCORE = 100;

    private final UserRepository userRepository;

    public TrustScoreEngine(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void rewardArtisan(UUID artisanId, int points) {
        log.info("Rewarding artisan ID: {} with {} trust score points", artisanId, points);
        User artisan = userRepository.findById(artisanId).orElse(null);
        if (artisan != null) {
            int currentScore = artisan.getTrustScore() != null ? artisan.getTrustScore() : BASE_TRUST_SCORE;
            int newScore = Math.max(MIN_SCORE, Math.min(MAX_SCORE, currentScore + points));
            artisan.setTrustScore(newScore);
            userRepository.save(artisan);
            log.info("Updated trust score for artisan {}: new score = {}", artisan.getFullName(), newScore);
        }
    }

    /**
     * Atomically processes a review rating and updates user's trust score.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void processReviewRating(User user, int rating) {
        log.info("Processing review rating {} for user ID: {}", rating, user.getId());
        
        int currentScore = user.getTrustScore() != null ? user.getTrustScore() : BASE_TRUST_SCORE;
        int delta;

        if (rating >= 4) {
            delta = 5; // Positive rating increases trust
        } else if (rating == 3) {
            delta = 1; // Neutral rating slight increase
        } else {
            delta = -10; // Low ratings (1 or 2 stars) severely impact/decrease trust
        }

        int newScore = Math.max(MIN_SCORE, Math.min(MAX_SCORE, currentScore + delta));
        user.setTrustScore(newScore);
        userRepository.save(user);

        log.info("Trust score for user {} updated from {} to {}", user.getFullName(), currentScore, newScore);
    }
}
