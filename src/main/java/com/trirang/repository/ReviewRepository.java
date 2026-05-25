package com.trirang.repository;

import com.trirang.model.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
    Page<Review> findByReviewedUserId(UUID reviewedUserId, Pageable pageable);
    List<Review> findByReviewedUserId(UUID reviewedUserId);
    boolean existsByReviewerIdAndReviewedUserIdAndRelatedMatchId(UUID reviewerId, UUID reviewedUserId, UUID relatedMatchId);
}
