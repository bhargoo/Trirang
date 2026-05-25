package com.trirang.repository;

import com.trirang.model.entity.ArtisanRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ArtisanRequirementRepository extends JpaRepository<ArtisanRequirement, UUID> {
    List<ArtisanRequirement> findByArtisanId(UUID artisanId);
}
