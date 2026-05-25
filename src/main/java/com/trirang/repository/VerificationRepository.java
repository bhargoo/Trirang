package com.trirang.repository;

import com.trirang.model.entity.Verification;
import com.trirang.model.enums.shared.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VerificationRepository extends JpaRepository<Verification, UUID> {
    Page<Verification> findByStatus(ReviewStatus status, Pageable pageable);
}
