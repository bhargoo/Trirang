package com.trirang.repository;

import com.trirang.model.entity.ArtisanVerification;
import com.trirang.model.enums.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArtisanVerificationRepository extends JpaRepository<ArtisanVerification, UUID> {
    Optional<ArtisanVerification> findTopByArtisanIdOrderBySubmittedAtDesc(UUID artisanId);
    List<ArtisanVerification> findByStatus(VerificationStatus status);
}
