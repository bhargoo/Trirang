package com.trirang.repository;

import com.trirang.model.entity.Listing;
import com.trirang.model.enums.ListingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ListingRepository extends JpaRepository<Listing, UUID> {
    List<Listing> findBySellerId(UUID sellerId);
    Page<Listing> findByStatus(ListingStatus status, Pageable pageable);
}
