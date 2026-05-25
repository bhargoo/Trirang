package com.trirang.repository;

import com.trirang.model.entity.Listing;
import com.trirang.model.enums.shared.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ListingRepository extends JpaRepository<Listing, UUID> {
    Page<Listing> findByStatus(ReviewStatus status, Pageable pageable);
}
