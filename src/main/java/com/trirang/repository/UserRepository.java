package com.trirang.repository;

import com.trirang.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    
    // Example PostGIS Spatial Query: Finds users within a certain radius (in meters)
    @Query(value = "SELECT * FROM users u WHERE ST_DWithin(u.location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326), :radiusInMeters)", nativeQuery = true)
    List<User> findUsersWithinRadius(@Param("lat") double lat, @Param("lng") double lng, @Param("radiusInMeters") double radiusInMeters);
}
