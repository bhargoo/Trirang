package com.trirang.repository;

import com.trirang.model.entity.TransformationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransformationRequestRepository extends JpaRepository<TransformationRequest, UUID> {
    List<TransformationRequest> findByDonorId(UUID donorId);
    List<TransformationRequest> findByArtisanId(UUID artisanId);

    @Query("SELECT t FROM TransformationRequest t WHERE t.donor.id = :userId OR t.artisan.id = :userId")
    List<TransformationRequest> findByUserId(@Param("userId") UUID userId);
}
