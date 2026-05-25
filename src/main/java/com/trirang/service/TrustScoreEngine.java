package com.trirang.service;

import com.trirang.model.entity.User;
import com.trirang.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class TrustScoreEngine {

    private final UserRepository userRepository;

    public TrustScoreEngine(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void rewardArtisan(UUID artisanId, int points) {
        log.info("Rewarding artisan ID: {} with {} trust score points", artisanId, points);
        User artisan = userRepository.findById(artisanId).orElse(null);
        if (artisan != null && artisan.getTrustScore() != null) {
            artisan.setTrustScore(Math.min(100, artisan.getTrustScore() + points));
            userRepository.save(artisan);
            log.info("Updated trust score for artisan {}: new score = {}", artisan.getFullName(), artisan.getTrustScore());
        }
    }
}
